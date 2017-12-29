package org.gearvrf;

interface MonoscopicActivityHandler {
    public void onPause();

    public void onResume();

    public void onSetScript();

    public boolean onBack();

    public boolean onBackLongPress();

    void setViewManager(GVRViewManager viewManager);
}
