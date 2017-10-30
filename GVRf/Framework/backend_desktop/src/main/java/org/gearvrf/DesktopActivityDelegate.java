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

import org.gearvrf.utility.VrAppSettings;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;


/**
 * {@inheritDoc}
 */
final class DesktopActivityDelegate implements GVRActivity.GVRActivityDelegate, IActivityNative {
    private GVRActivity mActivity;
    private DesktopViewManager xlibViewManager;

    @Override
    public void onCreate(GVRActivity activity) {
        mActivity = activity;
    }

    @Override
    public IActivityNative getActivityNative() {
        return this;
    }

    @Override
    public GVRViewManager makeViewManager() {
        return new DesktopViewManager(mActivity, mActivity.getMain());
    }

    @Override
    public GVRViewManager makeMonoscopicViewManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GVRCameraRig makeCameraRig(GVRContext context) {
        return new DesktopCameraRig(context);
    }

    @Override
    public GVRConfigurationManager makeConfigurationManager(GVRActivity activity) {
        return new GVRConfigurationManager(activity) {
            @Override
            public boolean isHmtConnected() {
                return false;
            }

            public boolean usingMultiview() {
                return false;
            }
        };
    }

    @Override
    public void onInitAppSettings(VrAppSettings appSettings) {
        // This is the only place where the setDockListenerRequired flag can be set before
        // the check in GVRActivity.
        mActivity.getConfigurationManager().setDockListenerRequired(false);
    }

    @Override
    public void parseXmlSettings(AssetManager assetManager, String dataFilename) {
        new DesktopXMLParser(assetManager, dataFilename, mActivity.getAppSettings());
    }

    @Override
    public boolean onBackPress() {
        return false;
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public boolean setMain(GVRMain gvrMain, String dataFileName) {
        return true;
    }

    @Override
    public void setViewManager(GVRViewManager viewManager) {
        xlibViewManager = (DesktopViewManager) viewManager;
    }

    @Override
    public VrAppSettings makeVrAppSettings() {
        final VrAppSettings settings = new VrAppSettings();
        final VrAppSettings.EyeBufferParams params = settings.getEyeBufferParams();
        params.setResolutionHeight(VrAppSettings.DEFAULT_FBO_RESOLUTION);
        params.setResolutionWidth(VrAppSettings.DEFAULT_FBO_RESOLUTION);
        return settings;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void setCameraRig(GVRCameraRig cameraRig) {
        if (xlibViewManager != null) {
            xlibViewManager.setCameraRig(cameraRig);
        }
    }

    @Override
    public void onUndock() {

    }

    @Override
    public void onDock() {

    }

    @Override
    public long getNative() {
        return 0;
    }

    /**
     * The class ignores the perspective camera and attaches a custom left and right camera instead.
     * Daydream uses the glFrustum call to create the projection matrix. Using the custom camera
     * allows us to set the projection matrix from the glFrustum call against the custom camera
     * using the set_projection_matrix call in the native renderer.
     */
    static class DesktopCameraRig extends GVRCameraRig {
        protected DesktopCameraRig(GVRContext gvrContext) {
            super(gvrContext);
        }

        @Override
        public void attachLeftCamera(GVRCamera camera) {
            GVRCamera leftCamera = new GVRCustomCamera(getGVRContext());
            leftCamera.setRenderMask(GVRRenderData.GVRRenderMaskBit.Left);
            super.attachLeftCamera(leftCamera);
        }

        @Override
        public void attachRightCamera(GVRCamera camera) {
            GVRCamera rightCamera = new GVRCustomCamera(getGVRContext());
            rightCamera.setRenderMask(GVRRenderData.GVRRenderMaskBit.Right);
            super.attachRightCamera(rightCamera);
        }
    }

	// The window handle
	private long window;

	public void run() {
		System.out.println("Hello LWJGL " + Version.getVersion() + "!");

		init();
		loop();

		// Free the window callbacks and destroy the window
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);

		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	private void init() {
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if ( !glfwInit() )
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

		// Create the window
		window = glfwCreateWindow(300, 300, "Hello World!", NULL, NULL);
		if ( window == NULL )
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback. It will be called every time a key is pressed, repeated or released.
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
		});

		// Get the thread stack and push a new frame
		try ( MemoryStack stack = stackPush() ) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(
				window,
				(vidmode.width() - pWidth.get(0)) / 2,
				(vidmode.height() - pHeight.get(0)) / 2
			);
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);

		// Make the window visible
		glfwShowWindow(window);
	}

	private void loop() {
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		// Set the clear color
		glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while ( !glfwWindowShouldClose(window) ) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

			glfwSwapBuffers(window); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
		}
	}


	public static void main(String[] args) {
		new DesktopActivityDelegate().run();
	}

}
