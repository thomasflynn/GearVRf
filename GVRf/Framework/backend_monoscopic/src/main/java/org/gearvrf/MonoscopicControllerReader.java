package org.gearvrf;

import android.graphics.PointF;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

class MonoscopicControllerReader implements org.gearvrf.io.GearCursorController.ControllerReader {

    private FloatBuffer readbackBuffer;
    private final long mPtr;

    MonoscopicControllerReader(long ptrActivityNative) {
        mPtr = ptrActivityNative;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isTouched() {
        return false;
    }

    @Override
    public void updateRotation(Quaternionf quat) {
    }

    @Override
    public void updatePosition(Vector3f vec) {
    }

    @Override
    public int getKey() {
        return 0;
    }

    @Override
    public float getHandedness() {
        return 0;
    }

    @Override
    public void updateTouchpad(PointF pt) {
    }

    @Override
    protected void finalize() throws Throwable {
    }

}


