package net.yrom.screenrecorder.operate;

import android.graphics.Bitmap;

import net.yrom.screenrecorder.camera.CameraData;
import net.yrom.screenrecorder.camera.CameraHolder;
import net.yrom.screenrecorder.rtmp.RESFlvData;
import net.yrom.screenrecorder.rtmp.RESFlvDataCollecter;
import net.yrom.screenrecorder.ui.CameraUIHelper;

/**
 * Created by daven.liu on 2017/9/13 0013.
 */

public class RecorderBean {
    private int width = RESFlvData.VIDEO_WIDTH;
    private int height = RESFlvData.VIDEO_HEIGHT;
    private int bitrate = RESFlvData.VIDEO_BITRATE;
    private int dpi = 1;
    private int fps = RESFlvData.FPS;
    private int iframe_interval = RESFlvData.IFRAME_INTERVAL; // 2 seconds between I-frames
    private String rtmpAddr;
    private boolean isMic = true;//默认为使用麦克风

    //Camera
    private Bitmap waterMakerImg;
    private boolean isFlight = false;//是否使用闪光灯
    private int effectType = CameraUIHelper.EFFECT_NORMAL;
    private int cameraType = CameraUIHelper.CAMERA_BACK;
    private int focusType = CameraUIHelper.FOCUS_AUTO;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public int getDpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }

    public String getRtmpAddr() {
        return rtmpAddr;
    }

    public void setRtmpAddr(String rtmpAddr) {
        this.rtmpAddr = rtmpAddr;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public int getIframe_interval() {
        return iframe_interval;
    }

    public void setIframe_interval(int iframe_interval) {
        this.iframe_interval = iframe_interval;
    }

    public boolean isMic() {
        return isMic;
    }

    public void setMic(boolean mic) {
        isMic = mic;
    }

    public Bitmap getWaterMakerImg() {
        return waterMakerImg;
    }

    public void setWaterMakerImg(Bitmap waterMakerImg) {
        this.waterMakerImg = waterMakerImg;
    }

    public int getEffectType() {
        return effectType;
    }

    public void setEffectType(int effectType) {
        this.effectType = effectType;
    }

    public int getCameraType() {
        return cameraType;
    }

    public void setCameraType(int cameraType) {
        this.cameraType = cameraType;
    }

    public boolean isFlight() {
        return isFlight;
    }

    public void setFlight(boolean flight) {
        isFlight = flight;
    }

    public int getFocusType() {
        return focusType;
    }

    public void setFocusType(int focusType) {
        this.focusType = focusType;
    }
}
