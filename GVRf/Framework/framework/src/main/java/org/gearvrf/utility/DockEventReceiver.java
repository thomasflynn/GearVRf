
package org.gearvrf.utility;

import org.gearvrf.GVRContext;
import java.lang.Runnable;

public final class DockEventReceiver {
    public DockEventReceiver(final GVRContext context, final Runnable runOnDock,
                             final Runnable runOnUndock) {
        mRunOnDock = runOnDock;
        mRunOnUndock = runOnUndock;
    }

    public void start() {
        if (!mIsStarted) {
            mIsStarted = true;
        }
    }

    public void stop() {
        if (mIsStarted) {
            mIsStarted = false;
        }
    }

    /*
    private final class BroadcastReceiverImpl extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (ACTION_HMT_DISCONNECT.equals(intent.getAction())) {
                mRunOnUndock.run();
            } else if (ACTION_HMT_CONNECT.equals(intent.getAction())) {
                mRunOnDock.run();
            }
        }
    }
    */

    private boolean mIsStarted;
    private final Runnable mRunOnDock;
    private final Runnable mRunOnUndock;

    private final static String ACTION_HMT_DISCONNECT = "com.samsung.intent.action.HMT_DISCONNECTED";
    private final static String ACTION_HMT_CONNECT = "com.samsung.intent.action.HMT_CONNECTED";

    private static final String TAG = "DockEventReceiver";
}
