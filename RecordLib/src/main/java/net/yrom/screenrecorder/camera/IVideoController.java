package net.yrom.screenrecorder.camera;

import net.yrom.screenrecorder.rtmp.RESFlvDataCollecter;

public interface IVideoController {
    void start();
    void stop();
    void pause();
    void resume();
    boolean setVideoBps(int bps);
    void setVideoEncoderListener(RESFlvDataCollecter listener);
    void setVideoConfiguration(VideoConfiguration configuration);
}
