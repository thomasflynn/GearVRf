/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.gearvrf.GVRScript.SplashMode;
import org.gearvrf.animation.GVRAnimation;
import org.gearvrf.animation.GVROnFinish;
import org.gearvrf.animation.GVROpacityAnimation;
import org.gearvrf.asynchronous.GVRAsynchronousResourceLoader;
import org.gearvrf.utility.Log;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.KeyEvent;

/*
 * This is the most important part of gvrf.
 * Initialization can be told as 2 parts. A General part and the GL part.
 * The general part needs nothing special but the GL part needs a GL context.
 * Since something being done while the GL context creates a surface is time-efficient,
 * the general initialization is done in the constructor and the GL initialization is
 * done in onSurfaceCreated().
 * 
 * After the initialization, gvrf works with 2 types of threads.
 * Input threads, and a GL thread.
 * Input threads are about the sensor, joysticks, and keyboards. They send data to gvrf.
 * gvrf handles those data as a message. It saves the data, doesn't do something
 * immediately. That's because gvrf is built to do everything about the scene in the GL thread.
 * There might be some pros by doing some rendering related stuffs outside the GL thread,
 * but since I thought simplicity of the structure results in efficiency, I didn't do that.
 * 
 * Now it's about the GL thread. It lets the user handle the scene by calling the users GVRScript.onStep().
 * There are also GVRFrameListeners, GVRAnimationEngine, and Runnables but they aren't that special.
 */

/**
 * This is the core internal class.
 * 
 * It implements {@link GVRContext}. It handles Android application callbacks
 * like cycles such as the standard Android {@link Activity#onResume()},
 * {@link Activity#onPause()}, and {@link Activity#onDestroy()}.
 * 
 * <p>
 * Most importantly, {@link #onDrawFrame()} does the actual rendering, using the
 * current orientation from
 * {@link #onRotationSensor(long, float, float, float, float, float, float, float)
 * onRotationSensor()} to draw the scene graph properly.
 */
class GVRViewManager extends GVRContext implements RotationSensorListener {

    private static final String TAG = Log.tag(GVRViewManager.class);

    protected final Queue<Runnable> mRunnables = new LinkedBlockingQueue<Runnable>();

    protected final Object[] mFrameListenersLock = new Object[0];
    protected List<GVRDrawFrameListener> mFrameListeners = new ArrayList<GVRDrawFrameListener>();

    protected GVRScript mScript;
    protected RotationSensor mRotationSensor;

    protected SplashScreen mSplashScreen;

    protected GVRLensInfo mLensInfo;
    protected GVRRenderBundle mRenderBundle = null;
    protected GVRScene mMainScene = null;
    protected GVRScene mNextMainScene = null;
    protected Runnable mOnSwitchMainScene = null;
    protected GVRScene mSensoredScene = null;

    protected long mPreviousTimeNanos = 0l;
    protected float mFrameTime = 0.0f;
    protected final List<Integer> mDownKeys = new ArrayList<Integer>();

    protected final GVRReferenceQueue mReferenceQueue = new GVRReferenceQueue();
    protected final GVRRecyclableObjectProtector mRecyclableObjectProtector = new GVRRecyclableObjectProtector();
    GVRActivity mActivity;
    protected int mCurrentEye;

    private native void renderCamera(long appPtr, long scene, long camera,
            long renderTexture, long shaderManager,
            long postEffectShaderManager, long postEffectRenderTextureA,
            long postEffectRenderTextureB);

    /**
     * Constructs GVRViewManager object with GVRScript which controls GL
     * activities
     * 
     * @param gvrActivity
     *            Current activity object
     * @param gvrScript
     *            {@link GVRScript} which describes
     * @param distortionDataFileName
     *            distortion filename under assets folder
     */
    GVRViewManager(GVRActivity gvrActivity, GVRScript gvrScript,
            String distortionDataFileName) {
        super(gvrActivity);

        // Clear singletons and per-run data structures
        resetOnRestart();

        GVRAsynchronousResourceLoader.setup(this);

        /*
         * Links with the script.
         */
        mScript = gvrScript;
        mActivity = gvrActivity;

        /*
         * Starts listening to the sensor.
         */
        mRotationSensor = new RotationSensor(gvrActivity, this);

        /*
         * Sets things with the numbers in the xml.
         */
        GVRXMLParser xmlParser = new GVRXMLParser(gvrActivity.getAssets(),
                distortionDataFileName);

        DisplayMetrics metrics = new DisplayMetrics();
        gvrActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        final float INCH_TO_METERS = 0.0254f;
        int screenWidthPixels = metrics.widthPixels;
        int screenHeightPixels = metrics.heightPixels;
        float screenWidthMeters = (float) screenWidthPixels / metrics.xdpi
                * INCH_TO_METERS;
        float screenHeightMeters = (float) screenHeightPixels / metrics.ydpi
                * INCH_TO_METERS;

        mLensInfo = new GVRLensInfo(screenWidthPixels, screenHeightPixels,
                screenWidthMeters, screenHeightMeters, xmlParser);

        GVRPerspectiveCamera.setDefaultFovY(xmlParser.getFovY());
        // Different width/height aspect ratio makes the rendered screen warped
        // when the screen rotates
        // GVRPerspectiveCamera.setDefaultAspectRatio(mLensInfo
        // .getRealScreenWidthMeters()
        // / mLensInfo.getRealScreenHeightMeters());
    }

    /*
     * Android life cycle
     */

    /**
     * Called when the system is about to start resuming a previous activity.
     * This is typically used to commit unsaved changes to persistent data, stop
     * animations and other things that may be consuming CPU, etc.
     * Implementations of this method must be very quick because the next
     * activity will not be resumed until this method returns.
     */
    void onPause() {
        Log.v(TAG, "onPause");
        mRotationSensor.onPause();
    }

    /**
     * Called when the activity will start interacting with the user. At this
     * point your activity is at the top of the activity stack, with user input
     * going to it.
     */
    void onResume() {
        Log.v(TAG, "onResume");
        mRotationSensor.onResume();
    }

    /**
     * The final call you receive before your activity is destroyed.
     */
    void onDestroy() {
        Log.v(TAG, "onDestroy");
        mReferenceQueue.onDestroy();
        mRotationSensor.onDestroy();
    }

    /*
     * GL life cycle
     */

    /**
     * Called when the surface changed size. When
     * setPreserveEGLContextOnPause(true) is called in the surface, this is
     * called only once.
     */
    void onSurfaceCreated() {
        Log.v(TAG, "onSurfaceCreated");

        Thread currentThread = Thread.currentThread();

        // Reduce contention with other Android processes
        currentThread.setPriority(Thread.MAX_PRIORITY);

        // we know that the current thread is a GL one, so we store it to
        // prevent non-GL thread from calling GL functions
        mGLThreadID = currentThread.getId();

        mPreviousTimeNanos = GVRTime.getCurrentTime();

        /*
         * GL Initializations.
         */
        mRenderBundle = new GVRRenderBundle(this, mLensInfo);
        mMainScene = new GVRScene(this);
    }

    private void renderCamera(long activity_ptr, GVRScene scene,
            GVRCamera camera, GVRRenderTexture renderTexture,
            GVRRenderBundle renderBundle) {
        renderCamera(activity_ptr, scene.getPtr(), camera.getPtr(),
                renderTexture.getPtr(), renderBundle.getMaterialShaderManager()
                        .getPtr(), renderBundle.getPostEffectShaderManager()
                        .getPtr(), renderBundle.getPostEffectRenderTextureA()
                        .getPtr(), renderBundle.getPostEffectRenderTextureB()
                        .getPtr());
    }

    /**
     * Called when the surface is created or recreated. Avoided because this can
     * be called twice at the beginning.
     */
    void onSurfaceChanged(int width, int height) {
        Log.v(TAG, "onSurfaceChanged");
    }

    void beforeDrawEyes() {
        mFrameHandler.beforeDrawEyes();
    }

    void onDrawEyeView(int eye, float fovDegrees) {
        mCurrentEye = eye;
        if (!(mSensoredScene == null || !mMainScene.equals(mSensoredScene))) {
            GVRCameraRig mainCameraRig = mMainScene.getMainCameraRig();
            if (eye == 1) {
                mainCameraRig.predict(4.0f / 60.0f);
                GVRCamera rightCamera = mainCameraRig.getRightCamera();
                renderCamera(mActivity.appPtr, mMainScene, rightCamera,
                        mRenderBundle.getRightRenderTexture(), mRenderBundle);
                mActivity.setCamera(rightCamera);
            } else {
                mainCameraRig.predict(3.5f / 60.0f);
                GVRCamera leftCamera = mainCameraRig.getLeftCamera();
                renderCamera(mActivity.appPtr, mMainScene, leftCamera,
                        mRenderBundle.getLeftRenderTexture(), mRenderBundle);
                mActivity.setCamera(leftCamera);
            }
        }
    }


    /** Called once per frame, before {@link #onDrawEyeView(int, float)}. */
    void onDrawFrame() {
        if (mCurrentEye == 1) {
            mActivity.setCamera(mMainScene.getMainCameraRig().getLeftCamera());
        } else {
            mActivity.setCamera(mMainScene.getMainCameraRig().getRightCamera());
        }
    }

    void afterDrawEyes() {
        mFrameHandler.afterDrawEyes();
    }

    /*
     * Splash screen life cycle
     */

    /**
     * Efficient handling of the state machine.
     * 
     * We want to be able to show an animated splash screen after
     * {@link GVRScript#onInit(GVRContext) onInit().} That means our frame
     * handler acts differently on the very first frame than it does during
     * splash screen animations, and differently again when we get to normal
     * mode. If we used a state enum and a switch statement, we'd have to keep
     * the two in synch, and we'd be spending render microseconds in a switch
     * statement, vectoring to a call to a handler. Using a interface
     * implementation instead of a state enum, we just call the handler
     * directly.
     */
    protected interface FrameHandler {
        void beforeDrawEyes();
        void onDrawFrame();
        void afterDrawEyes();
    }

    private FrameHandler firstFrame = new FrameHandler() {

        @Override
        public void beforeDrawEyes() {
            mSplashScreen = mScript.createSplashScreen(GVRViewManager.this);
            if (mSplashScreen != null) {
                getMainScene().addSceneObject(mSplashScreen);
            }

            mScript.onInit(GVRViewManager.this);

            if (mSplashScreen == null) {
                mFrameHandler = normalFrames;
                firstFrame = splashFrames = null;
            } else {
                mFrameHandler = splashFrames;
                firstFrame = null;
            }
        }
        
        public void onDrawFrame() {
            //Log.v(TAG, "firstFrame, onDrawFrame()");
        }

        @Override
        public void afterDrawEyes() {
        }
    };

    private FrameHandler splashFrames = new FrameHandler() {

        @Override
        public void beforeDrawEyes() {
            // splash screen post-init animations
            long currentTime = doMemoryManagementAndPerFrameCallbacks();

            if (mSplashScreen != null && currentTime >= mSplashScreen.mTimeout) {
                if (mSplashScreen.closeRequested()
                        || mScript.getSplashMode() == SplashMode.AUTOMATIC) {

                    final SplashScreen splashScreen = mSplashScreen;
                    new GVROpacityAnimation(mSplashScreen,
                            mScript.getSplashFadeTime(), 0) //
                            .setOnFinish(new GVROnFinish() {

                                @Override
                                public void finished(GVRAnimation animation) {
                                    if (mNextMainScene != null) {
                                        setMainScene(mNextMainScene);
                                    } else {
                                        getMainScene().removeSceneObject(
                                                splashScreen);
                                    }

                                    mFrameHandler = normalFrames;
                                    splashFrames = null;
                                }
                            }) //
                            .start(getAnimationEngine());

                    mSplashScreen = null;
                }
            }
        }
        
        public void onDrawFrame() {
            //Log.v(TAG, "splashFrame, onDrawFrame()");

            drawFrame(false);
        }

        @Override
        public void afterDrawEyes() {
        }
    };

    private final FrameHandler normalFrames = new FrameHandler() {

        public void beforeDrawEyes() {
            GVRNotifications.notifyBeforeStep();

            doMemoryManagementAndPerFrameCallbacks();

            mScript.onStep();
        }
        
        public void onDrawFrame() {
            //Log.v(TAG, "normalFrame, onDrawFrame()");

            drawFrame(true);
        }

        @Override
        public void afterDrawEyes() {
            GVRNotifications.notifyAfterStep();
        }
    };

    private long drawFrame(boolean onStep) {
        long currentTime = doMemoryManagementAndPerFrameCallbacks();
        drawEyes();
        return currentTime;
    }
     /**
     * This is the code that needs to be executed before either eye is drawn.
     * 
     * @return Current time, from {@link GVRTime#getCurrentTime()}
     */
    private long doMemoryManagementAndPerFrameCallbacks() {
        /*
         * Native heap memory, GPU memory management.
         */
        mReferenceQueue.clean();
        mRecyclableObjectProtector.clean();

        long currentTime = GVRTime.getCurrentTime();
        mFrameTime = (currentTime - mPreviousTimeNanos) / 1e9f;
        mPreviousTimeNanos = currentTime;

        /*
         * Without the sensor data, can't draw a scene properly.
         */
        //Log.d(TAG, "MainScene = %s, mSensoredScene = %s", mMainScene, mSensoredScene);
        if (!(mSensoredScene == null || !mMainScene.equals(mSensoredScene))) {
            Runnable runnable = null;
            while ((runnable = mRunnables.poll()) != null) {
                runnable.run();
            }

            synchronized (mFrameListenersLock) {
                final List<GVRDrawFrameListener> frameListeners = mFrameListeners;
                for (GVRDrawFrameListener listener : frameListeners) {
                    listener.onDrawFrame(mFrameTime);
                }
            }
        }

        return currentTime;
    }

    protected void drawEyes() {
    }

    protected FrameHandler mFrameHandler = firstFrame;

    void closeSplashScreen() {
        if (mSplashScreen != null) {
            mSplashScreen.close();
        }
    }

    /*
     * GVRF APIs
     */

    /**
     * Called to reset current sensor data.
     * 
     * @param timeStamp
     *            current time stamp
     * @param rotationW
     *            Quaternion rotation W
     * @param rotationX
     *            Quaternion rotation X
     * @param rotationY
     *            Quaternion rotation Y
     * @param rotationZ
     *            Quaternion rotation Z
     * @param gyroX
     *            Gyro rotation X
     * @param gyroY
     *            Gyro rotation Y
     * @param gyroZ
     *            Gyro rotation Z
     */
    @Override
    public void onRotationSensor(long timeStamp, float rotationW,
            float rotationX, float rotationY, float rotationZ, float gyroX,
            float gyroY, float gyroZ) {
        GVRCameraRig cameraRig = null;
        if (mMainScene != null) {
            cameraRig = mMainScene.getMainCameraRig();
        }

        if (cameraRig != null) {
            cameraRig.setRotationSensorData(timeStamp, rotationW, rotationX,
                    rotationY, rotationZ, gyroX, gyroY, gyroZ);

            if (mSensoredScene == null || !mMainScene.equals(mSensoredScene)) {
                cameraRig.resetYaw();
                mSensoredScene = mMainScene;
            }
        }
    }

    /**
     * Called when a key was pressed down and not handled by any of the views
     * inside of the activity.
     * 
     * @param keyCode
     *            The value in {@link event#getKeyCode() event.getKeyCode()}
     * @param event
     *            Description of the key event
     */
    void onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            synchronized (mDownKeys) {
                mDownKeys.add(keyCode);
            }
        }
    }

    @Override
    public GVRScene getMainScene() {
        return mMainScene;
    }

    @Override
    public synchronized void setMainScene(GVRScene scene) {
        mMainScene = scene;
        if (mNextMainScene == scene) {
            mNextMainScene = null;
            if (mOnSwitchMainScene != null) {
                mOnSwitchMainScene.run();
                mOnSwitchMainScene = null;
            }
        }
    }

    @Override
    public synchronized GVRScene getNextMainScene(Runnable onSwitchMainScene) {
        if (mNextMainScene == null) {
            mNextMainScene = new GVRScene(this);
        }
        mOnSwitchMainScene = onSwitchMainScene;
        return mNextMainScene;
    }

    @Override
    public boolean isKeyDown(int keyCode) {
        synchronized (mDownKeys) {
            return mDownKeys.contains(keyCode);
        }
    }

    @Override
    public float getFrameTime() {
        return mFrameTime;
    }

    @Override
    public void runOnGlThread(Runnable runnable) {
        mRunnables.add(runnable);
    }

    @Override
    public void registerDrawFrameListener(GVRDrawFrameListener frameListener) {
        synchronized (mFrameListenersLock) {
            mFrameListeners = new ArrayList<GVRDrawFrameListener>(
                    mFrameListeners);
            mFrameListeners.add(frameListener);
        }
    }

    @Override
    public void unregisterDrawFrameListener(GVRDrawFrameListener frameListener) {
        synchronized (mFrameListenersLock) {
            mFrameListeners = new ArrayList<GVRDrawFrameListener>(
                    mFrameListeners);
            mFrameListeners.remove(frameListener);
        }
    }

    @Override
    GVRReferenceQueue getReferenceQueue() {
        return mReferenceQueue;
    }

    @Override
    GVRRenderBundle getRenderBundle() {
        return mRenderBundle;
    }

    @Override
    GVRRecyclableObjectProtector getRecyclableObjectProtector() {
        return mRecyclableObjectProtector;
    }
}
