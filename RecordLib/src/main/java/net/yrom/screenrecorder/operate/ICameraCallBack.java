package net.yrom.screenrecorder.operate;

/**
 * Created by daven.liu on 2017/10/18 0018.
 */

public abstract class ICameraCallBack {
    public void onCameraOpenSuccess() {}
    public abstract void onCameraOpenError();

    /**
     * 可使用CameraUIHelper.CAMERA_BACK和CameraUIHelper.CAMERA_FRONT比较
     * @param cameraType
     */
    public void onSwitchCamera(int cameraType) {}
    public abstract void onLiveStart(String rtmpAddress);
    public abstract void onLiveStop();

    public abstract void sendError();
    public abstract void connectError();
    public void netBad() {}
}
