/* Copyright 2016 Samsung Electronics Co., LTD
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


/**
 */
class DesktopRenderer implements GLSurfaceView.Renderer {
    static {
        System.loadLibrary("gvr");
        System.loadLibrary("gvrf-xlib");
    }

    private DesktopViewManager mViewManager;
    private long nativeDesktopRenderer;
    private GVRCameraRig cameraRig;
    private final boolean[] mBlendEnabled = new boolean[1];

    public DesktopRenderer(DesktopViewManager viewManager, long nativeGvrContext) {
        mViewManager = viewManager;
        nativeDesktopRenderer = nativeCreateRenderer(nativeGvrContext);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        nativeInitializeGl(nativeDesktopRenderer);
        mViewManager.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //Do nothing
    }
    public long getNativeDesktopRenderer(){
        return nativeDesktopRenderer;
    }
    @Override
    public void onDrawFrame(GL10 gl) {
        if (cameraRig == null) {
            return;
        }
        GLES30.glGetBooleanv(GLES30.GL_BLEND, mBlendEnabled, 0);
        GLES30.glDisable(GLES30.GL_BLEND);
        mViewManager.beforeDrawEyes();
        mViewManager.onDrawFrame();
        nativeDrawFrame(nativeDesktopRenderer);

        mViewManager.afterDrawEyes();
        if (mBlendEnabled[0]) {
            GLES30.glEnable(GLES30.GL_BLEND);
        }
    }

    public void setCameraRig(GVRCameraRig cameraRig) {
        this.cameraRig = cameraRig;
        nativeSetCameraRig(nativeDesktopRenderer, cameraRig.getNative());
    }

    void onResume() {
        nativeOnResume(nativeDesktopRenderer);
    }

    void onPause() {
        nativeOnPause(nativeDesktopRenderer);
    }

    void onDestroy() {
        nativeDestroyRenderer(nativeDesktopRenderer);
    }

    // called from the native side
    void onDrawEye(int eye) {
        mViewManager.onDrawEye(eye);
    }

    private native long nativeCreateRenderer(long nativeDesktopRenderer);

    private native void nativeDestroyRenderer(long nativeDesktopRenderer);

    private native void nativeInitializeGl(long nativeDesktopRenderer);

    private native long nativeDrawFrame(long nativeDesktopRenderer);

    private native void nativeOnPause(long nativeDesktopRenderer);

    private native void nativeOnResume(long nativeDesktopRenderer);

    private native void nativeSetCameraRig(long nativeDesktopRenderer, long nativeCamera);
}
