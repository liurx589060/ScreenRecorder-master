package net.yrom.screenrecorder.operate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import net.yrom.screenrecorder.ui.CameraUIHelper;

/**
 * Created by daven.liu on 2017/9/14 0014.
 */

public class CameraRecordOpt implements IRecordOpt{
    private static CameraRecordOpt instance = null;
    private RecorderBean recorderBean;
    private ICameraCallBack cameraCallBack;
    private boolean isExtraActivity;


    private CameraUIHelper mCameraUIHelper;

    public static CameraRecordOpt getInstance() {
        if(instance == null) {
            instance = new CameraRecordOpt();
        }
        return instance;
    }

    /**
     * 开始Camera录制,已打开一个新的Activity的方式，自定义
     * @param bean
     */
    public void startCameraRecordWithActivity(Context context,RecorderBean bean,Class<? extends Activity> newActivity) {
        this.recorderBean = bean;
        Intent intent = new Intent(context,newActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
        isExtraActivity = true;
    }

    /**
     * 开始Camera录制,在原有的Activity
     * @param bean
     */
    public void startCameraRecordNoActivity(Activity activity,RecorderBean bean,View[] views) {
        this.recorderBean = bean;
        CameraUIHelper helper = new CameraUIHelper(activity,bean);
        helper.addContentViewWithSelf(views);
    }

    /**
     * 停止直播
     */
    public void stopCameraRecord() {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.stopRecord();
        }
    }

    /**
     * 是否静音
     * @param isMic
     */
    @Override
    public void setMic(boolean isMic) {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.setMic(isMic);
        }
    }

    /**
     * 是否打开闪光灯
     */
    public void switchLight(boolean isFlight) {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.switchLight(isFlight);
        }
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.switchCamera();
        }
    }

    /**
     * 切换滤镜
     */
    public void setEffect(int type) {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.setEffect(type);
        }
    }

    /**
     * 切换聚焦方式
     */
    public void switchFocusMode() {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.switchFocusMode();
        }
    }

    /**
     * 关闭Activity的方式结束
     */
    public void destroyWithActivity() {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.destroyWithActivity();
        }
    }

    public void destroyNoActivity() {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.destroyNoActivity();
        }
    }

    public RecorderBean getRecorderBean() {
        if(recorderBean == null) {
            recorderBean = new RecorderBean();
        }
        return recorderBean;
    }

    public void setmCameraUIHelper(CameraUIHelper mCameraUIHelper) {
        this.mCameraUIHelper = mCameraUIHelper;
    }

    @Override
    public void stopRecord() {
        if(isExtraActivity) {
            destroyWithActivity();
        }else {
            destroyNoActivity();
        }
    }

    @Override
    public boolean isRecording() {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.isRecording();
        }
        return false;
    }

    @Override
    public void pause() {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.pause();
        }
    }

    @Override
    public void resume() {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.resume();
        }
    }

    @Override
    public boolean isMic() {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.isMic();
        }
        return false;
    }

    public boolean isFlight() {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.isFlight();
        }
        return false;
    }

    public int getCameraType() {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.getCameraType();
        }
        return -1;
    }

    public int getEffectType() {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.getEffectType();
        }
        return -1;
    }

    public int getFocusType() {
        if(mCameraUIHelper != null) {
            mCameraUIHelper.getFocusType();
        }
        return -1;
    }

    public ICameraCallBack getCameraCallBack() {
        return cameraCallBack;
    }

    public void setCameraCallBack(ICameraCallBack cameraCallBack) {
        this.cameraCallBack = cameraCallBack;
    }
}
