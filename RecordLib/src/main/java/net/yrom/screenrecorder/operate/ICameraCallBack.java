package net.yrom.screenrecorder.operate;

/**
 * Created by daven.liu on 2017/10/18 0018.
 */

public interface ICameraCallBack {
    public void onSuccess();
    public void onError();
    public void onSwitchCamera();
    public void onLiveStart();
    public void onLiveStop();
}
