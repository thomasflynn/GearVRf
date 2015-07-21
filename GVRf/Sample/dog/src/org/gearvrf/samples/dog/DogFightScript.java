package org.gearvrf.samples.dog;

import java.util.ArrayList;
import java.util.concurrent.Future;

import android.app.Activity;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.gearvrf.GVRActivity;
import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRCameraRig;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRPerspectiveCamera;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRScript;
import org.gearvrf.GVRTexture;
import org.gearvrf.scene_objects.GVRCubeSceneObject;


public class DogFightScript extends GVRScript
{
    private GVRCubeSceneObject mSkyBox;
    private GVRCameraRig mCamera;
    private static final String TAG = "DogFightScript";
    private Aircraft myAircraft = new Aircraft();

    @Override
    public void onInit(GVRContext gvrContext) {

        GVRScene scene = gvrContext.getNextMainScene();
        mCamera = scene.getMainCameraRig();

        // load texture asynchronously
        Future<GVRTexture> futureTexture = gvrContext.loadFutureTexture(new GVRAndroidResource(gvrContext, R.raw.side));
        Future<GVRTexture> futureTextureTop = gvrContext.loadFutureTexture(new GVRAndroidResource(gvrContext, R.raw.top));
        Future<GVRTexture> futureTextureBottom = gvrContext.loadFutureTexture(new GVRAndroidResource(gvrContext, R.raw.ground));

        ArrayList<Future<GVRTexture>> futureTextureList = new ArrayList<Future<GVRTexture>>(6);

        futureTextureList.add(futureTexture);
        futureTextureList.add(futureTexture);
        futureTextureList.add(futureTexture);
        futureTextureList.add(futureTexture);
        futureTextureList.add(futureTextureTop);
        futureTextureList.add(futureTextureBottom);


        mSkyBox = new GVRCubeSceneObject(gvrContext, false, futureTextureList);
        mSkyBox.getTransform().setScale(100000, 100000, 100000);
        mSkyBox.getTransform().setPosition(0, 40000, 0);

        scene.addSceneObject(mSkyBox);

        GVRPerspectiveCamera lPersp = (GVRPerspectiveCamera) mCamera.getLeftCamera();
        GVRPerspectiveCamera rPersp = (GVRPerspectiveCamera) mCamera.getRightCamera();
        lPersp.setFarClippingDistance(100000);
        rPersp.setFarClippingDistance(100000);

        mCamera.getTransform().setPosition(0, 0,  0);
    }

    @Override
    public void onStep() {
    	
    	myAircraft.step(16.67f);
    	
    	float[] pos = myAircraft.getPos();
    	float[] hpr = myAircraft.getHpr();
    	
    	mCamera.getTransform().rotateByAxis(hpr[2], 0, 0, 1);
    	mCamera.getTransform().rotateByAxis(-hpr[1], 1, 0, 0);
    	mCamera.getTransform().rotateByAxis(hpr[0], 0, 1, 0);
    	mCamera.getTransform().rotateByAxis(-90.0f, 1, 0, 0);
    	mCamera.getTransform().translate(pos[2], pos[0], -pos[1]);
    	/*
    	android.util.Log.d(TAG, "pos[0]="+pos[0]);
    	android.util.Log.d(TAG, "pos[1]="+pos[1]);
    	android.util.Log.d(TAG, "pos[2]="+pos[2]);
        */
    	android.util.Log.d(TAG, "hpr[0]="+hpr[0]);
    	android.util.Log.d(TAG, "hpr[1]="+hpr[1]);
    	android.util.Log.d(TAG, "hpr[2]="+hpr[2]);
    }

    public void processKeyEvent(int keyCode) {
    	android.util.Log.d(TAG, "got keyCode: " + keyCode);

    	 switch(keyCode) {
    	 	case KeyEvent.KEYCODE_BUTTON_X:  
    	 		android.util.Log.d(TAG, "button ...");
    	 		myAircraft.decThrust();
    	 		break;
    	 	case KeyEvent.KEYCODE_BUTTON_Y:  
    	 		android.util.Log.d(TAG, "button ....");
    	 		break;
    	 	case KeyEvent.KEYCODE_BUTTON_A:  
    	 		android.util.Log.d(TAG, "button .");
    	 		myAircraft.incThrust();
    	 		break;
    	 	case KeyEvent.KEYCODE_BUTTON_B:  
    	 		android.util.Log.d(TAG, "button ..");
    	 		break;
    	 	case KeyEvent.KEYCODE_BUTTON_R1:
    	 		android.util.Log.d(TAG, "button R1");
    	 		break;
    	 	case KeyEvent.KEYCODE_BUTTON_L1:
    	 		android.util.Log.d(TAG, "button L1");
    	 		break;
    	 	case KeyEvent.KEYCODE_BUTTON_START:
    	 		android.util.Log.d(TAG, "button START");
    	 		break;
    	 	case KeyEvent.KEYCODE_BUTTON_SELECT:
    	 		android.util.Log.d(TAG, "button SELECT");
    	 		break;
    	 	case 310:
    	 		android.util.Log.d(TAG, "Play");
    	 		break;

    	 }
     
    }
    
    private static float getCenteredAxis(MotionEvent event,
            InputDevice device, int axis, int historyPos) {
        final InputDevice.MotionRange range =
                device.getMotionRange(axis, event.getSource());

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        if (range != null) {
            final float flat = range.getFlat();
            final float value =
                    historyPos < 0 ? event.getAxisValue(axis):
                    event.getHistoricalAxisValue(axis, historyPos);

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }
    
    public void processJoystickInput(MotionEvent event, int historyPos) {
        InputDevice mInputDevice = event.getDevice();

        // Calculate the horizontal distance to move by
        // using the input value from one of these physical controls:
        // the left control stick, hat axis, or the right control stick.
        float x = getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_X, historyPos);
        float hatx = getCenteredAxis(event, mInputDevice,
                    MotionEvent.AXIS_HAT_X, historyPos);
        float z = getCenteredAxis(event, mInputDevice,
                    MotionEvent.AXIS_RX, historyPos);

        // Calculate the vertical distance to move by
        // using the input value from one of these physical controls:
        // the left control stick, hat switch, or the right control stick.
        float y = getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_Y, historyPos);
        
        float haty = getCenteredAxis(event, mInputDevice,
                    MotionEvent.AXIS_HAT_Y, historyPos);
        
        float rz = getCenteredAxis(event, mInputDevice,
                    MotionEvent.AXIS_RY, historyPos);
        
        if(x > 0) {
        	myAircraft.rollRight(x);
        } else if(x < 0) {
        	myAircraft.rollLeft(x);
        } else {
        	myAircraft.zeroAilerons();
        }
        
        if(y > 0) {
        	myAircraft.pitchDown(y);
        } else if(y < 0) {
        	myAircraft.pitchUp(y);
        } else {
        	myAircraft.zeroElevators();
        }
        
        /*
        android.util.Log.d(TAG, "x = " + x);
        android.util.Log.d(TAG, "hatx = " + hatx);
        android.util.Log.d(TAG, "rz = " + z);

        android.util.Log.d(TAG, "y = " + y);
        android.util.Log.d(TAG, "haty = " + haty);
        android.util.Log.d(TAG, "ry = " + rz);
        */
    }

}
