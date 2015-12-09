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

import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRScript;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRTexture;
import org.gearvrf.scene_objects.GVRViewSceneObject;
import org.gearvrf.scene_objects.view.GVRView;
import org.gearvrf.scene_objects.view.GVRWebView;


public class VrBrowserViewManager extends GVRScript {
    private VrBrowser mActivity;
    private GVRContext mGVRContext;
    private GVRScene mScene;

    VrBrowserViewManager(VrBrowser activity) {
        mActivity = activity;
    }

    @Override
    public void onInit(GVRContext gvrContext) {
        mGVRContext = gvrContext;
        
        mScene = gvrContext.getNextMainScene();

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
    }

    private void createSkybox() {

        mScene.getMainCameraRig().getTransform().setPosition(-0f, 0.85f, 0f);

        GVRMesh spaceMesh = mGVRContext.loadMesh(new GVRAndroidResource(mGVRContext, R.drawable.skybox_esphere));
        GVRTexture spaceTexture = mGVRContext.loadTexture(new GVRAndroidResource(mGVRContext, R.drawable.skybox));

        GVRSceneObject mSpaceSceneObject = new GVRSceneObject(mGVRContext, spaceMesh, spaceTexture);
        mScene.addSceneObject(mSpaceSceneObject);
        mSpaceSceneObject.getRenderData().setRenderingOrder(0);

    }

    private void addCursorPosition() {

        GVRSceneObject headTracker = new GVRSceneObject(mGVRContext,
                mGVRContext.createQuad(0.5f, 0.5f), mGVRContext.loadTexture(new GVRAndroidResource(
                        mGVRContext, R.drawable.head_tracker)));

        headTracker.getTransform().setPositionZ(-9.0f);
        headTracker.getRenderData().setRenderingOrder(
                GVRRenderData.GVRRenderingOrder.OVERLAY);
        headTracker.getRenderData().setDepthTest(false);
        headTracker.getRenderData().setRenderingOrder(100000);
        mScene.getMainCameraRig().getRightCamera().addChildObject(headTracker);
    }



    private GVRViewSceneObject createWebViewObject(GVRContext gvrContext) {
        GVRView webView = mActivity.getWebView();
        GVRViewSceneObject webObject = new GVRViewSceneObject(gvrContext, webView, 8.0f, 4.0f);
        webObject.setName("web view object");
        webObject.getRenderData().getMaterial().setOpacity(1.0f);
        webObject.getTransform().setPosition(0.0f, 0.0f, -4.0f);

        return webObject;
    }

    public void onPause() {
    }

    public void onTap() {
    }

    @Override
    public void onStep() {
    }


}

