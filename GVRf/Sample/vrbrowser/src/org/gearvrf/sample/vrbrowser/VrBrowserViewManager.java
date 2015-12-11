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

package org.gearvrf.sample.vrbrowser;

import java.util.ArrayList;
import java.util.List;

import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRMeshEyePointee;
import org.gearvrf.GVRPicker;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRScript;
import org.gearvrf.GVRContext;
import org.gearvrf.GVREyePointeeHolder;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRTexture;
import org.gearvrf.scene_objects.GVRViewSceneObject;
import org.gearvrf.scene_objects.view.GVRView;
import org.gearvrf.scene_objects.view.GVRWebView;

import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.widget.Toast;


public class VrBrowserViewManager extends GVRScript {
    private VrBrowser mActivity;
    private GVRContext mGVRContext;
    private GVRScene mScene;
    
    private GVRWebView mWebView;
    
    private GVRSceneObject mContainer;
    
    private float mScreenDistance = 2.5f;
    
    private boolean browserFocused = true;
    
    private List<GVRPicker.GVRPickedObject> pickedObjects;
    private float[] hitLocation = new float[3];
    
    private List<Button> uiButtons = new ArrayList<Button>();
    private Button focusedButton = null;
    private boolean buttonFocused = false;

    VrBrowserViewManager(VrBrowser activity) {
        mActivity = activity;
    }

    @Override
    public void onInit(GVRContext gvrContext) {
        mGVRContext = gvrContext;
        
        mScene = gvrContext.getNextMainScene();
        
        mContainer = new GVRSceneObject(gvrContext);
        mScene.addSceneObject(mContainer);
        
        GVRViewSceneObject webViewObject = createWebViewObject(gvrContext);

        GVRSceneObject floor = new GVRSceneObject(mGVRContext,
                mGVRContext.createQuad(120.0f, 120.0f),
                mGVRContext.loadTexture(new GVRAndroidResource(mGVRContext, R.drawable.floor)));

        floor.getTransform().setRotationByAxis(-90, 1, 0, 0);
        floor.getTransform().setPositionY(-10.0f);
        mScene.addSceneObject(floor);
        floor.getRenderData().setRenderingOrder(0);

        createSkybox();

        addCursorPosition();
        
        mScene.addSceneObject(webViewObject);
        
        addButtons();
    }

    private void createSkybox() {

        mScene.getMainCameraRig().getTransform().setPosition(-0f, 0.0f, 0f);

        GVRMesh spaceMesh = mGVRContext.loadMesh(new GVRAndroidResource(mGVRContext, R.drawable.skybox_esphere));
        GVRTexture spaceTexture = mGVRContext.loadTexture(new GVRAndroidResource(mGVRContext, R.drawable.skybox));

        GVRSceneObject mSpaceSceneObject = new GVRSceneObject(mGVRContext, spaceMesh, spaceTexture);
        mScene.addSceneObject(mSpaceSceneObject);
        mSpaceSceneObject.getRenderData().setRenderingOrder(0);

    }

    private void addCursorPosition() {

        GVRSceneObject headTracker = new GVRSceneObject(mGVRContext,
                mGVRContext.createQuad(0.10f, 0.10f), mGVRContext.loadTexture(new GVRAndroidResource(mGVRContext, R.raw.gaze_cursor_dot2)));

        headTracker.getTransform().setPositionZ(-mScreenDistance);
        headTracker.getRenderData().setRenderingOrder(GVRRenderData.GVRRenderingOrder.OVERLAY);
        headTracker.getRenderData().setDepthTest(false);
        headTracker.getRenderData().setRenderingOrder(100000);
        mScene.getMainCameraRig().getRightCamera().addChildObject(headTracker);
    }



    private GVRViewSceneObject createWebViewObject(GVRContext gvrContext) {
        
        mWebView = (GVRWebView)mActivity.getWebView();

        float aspect = (float)mWebView.getView().getWidth() / mWebView.getView().getHeight();
        float size = 2f;
        
        GVRViewSceneObject webObject = new GVRViewSceneObject(gvrContext, mWebView, 4.0f, 3.0f);
        webObject.setName("webview");
        webObject.getRenderData().getMaterial().setOpacity(1.0f);
        
        //webObject.getTransform().setPosition(0.0f, 0.0f, -mScreenDistance);
        webObject.getTransform().setPosition(0.0f, 0.0f, -3.0f);

        attachDefaultEyePointee(webObject);
        
        return webObject;
    }
    
    private void addButtons() {
        // add buttons to container
        GVRSceneObject uiContainerObject = new GVRSceneObject(mGVRContext);
        //uiContainerObject.getTransform().setPosition(-1.3f, 0f, -mScreenDistance);
        uiContainerObject.getTransform().setPosition(-2.3f, 0f, -3.0f);
        uiContainerObject.getTransform().rotateByAxis(20f, 0f, 1f, 0f);
        mContainer.addChildObject(uiContainerObject);

        float width = 2f;
        float height = 2f;
        
        // nav buttons
        String[] buttons = { "home", "reload", "back", "forward" };
        int[] buttonTextures = { R.raw.home_button, R.raw.refresh_button,
                R.raw.back_button, R.raw.forward_button };

        int numButtons = buttons.length;
        float buttonSize = 0.3f;

        float yInitial = height / 2 - (buttonSize/2);
        float ySpacing = (height-buttonSize) / (numButtons-1);

        for (int i = 0; i < buttons.length; i++) {
            Button button = new Button(mGVRContext, buttons[i], buttonTextures[i], buttonSize);
            uiButtons.add(button);

            GVRSceneObject buttonObject = button.getSceneObject();
            attachDefaultEyePointee(buttonObject);

            float y = yInitial - ySpacing * i;
            buttonObject.getTransform().setPositionY(y);

            uiContainerObject.addChildObject(buttonObject);
        }
    }
    
    protected void attachDefaultEyePointee(GVRSceneObject sceneObject) {
        GVREyePointeeHolder eyePointeeHolder = new GVREyePointeeHolder(mGVRContext);
        GVRMesh mesh = sceneObject.getRenderData().getMesh();
        GVRMeshEyePointee eyePointee = new GVRMeshEyePointee(mGVRContext, mesh);
        eyePointeeHolder.addPointee(eyePointee);
        sceneObject.attachEyePointeeHolder(eyePointeeHolder);
    }
    
    public void navigateHome() {
       mWebView.loadUrl("http://oculus.com");
    }

    public void refreshWebView() {
        mWebView.reload();
    }

    public void navigateForward() {
        if (mWebView.canGoForward())
            mWebView.goForward();
    }

    public void navigateBack() {
        if (mWebView.canGoBack())
            mWebView.goBack();
    }
    
    // browser click
    public void click() {
        final long uMillis = SystemClock.uptimeMillis();

        int width = 900,
           height = 900;

        float hitX = this.hitLocation[0];
        float hitY = this.hitLocation[1] * -1f;

        float x = (hitX + 1f) * width/2;
        float y = (hitY + 1f) * height/2;

        mWebView.dispatchTouchEvent(MotionEvent.obtain(uMillis, uMillis,
                MotionEvent.ACTION_DOWN, x, y, 0));
        mWebView.dispatchTouchEvent(MotionEvent.obtain(uMillis, uMillis,
                MotionEvent.ACTION_UP, x, y, 0));    	
    }

    public void onPause() {
    }

    public void onTap() {
        if (browserFocused) {
        	this.showMessage("click browser");
            click();
        } else if (buttonFocused) {
            String buttonAction = focusedButton.name;

            if (buttonAction.equals("reload")) {
                refreshWebView();
            } else if (buttonAction.equals("forward")) {
                navigateForward();
            } else if (buttonAction.equals("back")) {
                navigateBack();
            } else if (buttonAction.equals("home")) {
                navigateHome();
            }
        } else {

        }
    }
    
    private boolean skip = false;

    @Override
    public void onStep() {
    	if (skip) return;
    	
    	pickedObjects = GVRPicker.findObjects(mScene, 0f,0f,0f, 0f,0f,-1f);

        for (GVRPicker.GVRPickedObject pickedObject : pickedObjects) {
            GVRSceneObject obj = pickedObject.getHitObject();

            hitLocation = pickedObject.getHitLocation();

            // fix: probably issue with this, not triggering
            if (obj.getName().equals("webview")) {
                browserFocused = true;
                buttonFocused = false;

                /*String coords =
                        String.format("%.3g%n", hitLocation[0]) + "," +
                        String.format("%.3g%n", hitLocation[1]);

                editText.setText(coords);*/
            } else { // NOTE: buttons only for now

                browserFocused = false;
                buttonFocused = true;

                for (int i = 0; i < uiButtons.size(); i++) {
                    Button button = uiButtons.get(i);
                    if ( button.name.equals( obj.getName() ) ) {
                        focusedButton = button;
                        button.setFocus(true);
                    }
                }
            }

            break;
        }

        // reset
        if (pickedObjects.size() == 0) {
            browserFocused = false;
            buttonFocused = false;

            if (focusedButton != null)
                focusedButton.setFocus(false);
        }
    	
    }

    // debug
    private void showMessage(String message) {
        Toast bread = Toast.makeText(mActivity, message, Toast.LENGTH_SHORT);
        bread.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
        //toast.setGravity(Gravity.TOP|Gravity.LEFT, 0, 0);
        bread.setMargin(0f, 0f);
        bread.show();
    }

}

