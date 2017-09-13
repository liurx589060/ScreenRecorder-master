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
}
