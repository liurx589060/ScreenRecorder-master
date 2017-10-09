package net.yrom.screenrecorder.operate;

import net.yrom.screenrecorder.rtmp.RESFlvData;
import net.yrom.screenrecorder.rtmp.RESFlvDataCollecter;

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
}
