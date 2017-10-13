package net.yrom.screenrecorder.operate;

/**
 * Created by daven.liu on 2017/9/14 0014.
 */

public class CameraRecordOpt {
    private static CameraRecordOpt instance = null;

    private boolean isRecording;

    public static CameraRecordOpt getInstance() {
        if(instance == null) {
            instance = new CameraRecordOpt();
        }
        return instance;
    }

    public void startCameraRecord() {

    }
}
