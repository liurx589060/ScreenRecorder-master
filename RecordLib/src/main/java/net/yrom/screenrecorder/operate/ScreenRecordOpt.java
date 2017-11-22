package net.yrom.screenrecorder.operate;

import android.media.projection.MediaProjection;
import android.os.Build;
import android.support.annotation.RequiresApi;

import net.yrom.screenrecorder.core.RESAudioClient;
import net.yrom.screenrecorder.core.RESCoreParameters;
import net.yrom.screenrecorder.rtmp.RESFlvData;
import net.yrom.screenrecorder.rtmp.RESFlvDataCollecter;
import net.yrom.screenrecorder.task.RtmpStreamingSender;
import net.yrom.screenrecorder.task.ScreenRecorder;
import net.yrom.screenrecorder.tools.LogTools;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by daven.liu on 2017/9/13 0013.
 */

public class ScreenRecordOpt {
    private static ScreenRecordOpt instance = null;

    private RtmpStreamingSender streamingSender;
    private RESCoreParameters coreParameters;
    private RESAudioClient audioClient;
    private ScreenRecorder mVideoRecorder;
    private ExecutorService executorService;

    private boolean isRecording;

    public static ScreenRecordOpt getInstance() {
        if(instance == null) {
            instance = new ScreenRecordOpt();
        }
        return instance;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void startScreenRecord(RecorderBean recorderBean, MediaProjection projection,RtmpStreamingSender.IRtmpSendCallBack callBack) {
        streamingSender = new RtmpStreamingSender(recorderBean);
        coreParameters = new RESCoreParameters();
        executorService = Executors.newCachedThreadPool();

        streamingSender.sendStart(recorderBean.getRtmpAddr());
        RESFlvDataCollecter collecter = new RESFlvDataCollecter() {
            @Override
            public void collect(RESFlvData flvData, int type) {
                if(streamingSender != null) {
                    streamingSender.sendFood(flvData, type);
                }
            }
        };

        //音频
        audioClient = new RESAudioClient(coreParameters);
        audioClient.setMic(recorderBean.isMic());
        if (!audioClient.prepare()) {
            LogTools.e("!!!!!audioClient.prepare()failed");
            return;
        }
        audioClient.start(collecter);

        //视频
        if(mVideoRecorder == null) {
            mVideoRecorder = new ScreenRecorder(collecter, recorderBean, projection);
        }
        mVideoRecorder.start();

        executorService.execute(streamingSender);
        streamingSender.setRtmpSendCallBack(callBack);

        isRecording = true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void stopScreenRecord() {
        if(mVideoRecorder != null) {
            mVideoRecorder.quit();
            mVideoRecorder = null;
        }

        if(audioClient != null) {
            audioClient.stop();
            audioClient.destroy();
            audioClient = null;
        }

        if (streamingSender != null) {
            streamingSender.sendStop();
            streamingSender.quit();
            streamingSender = null;
        }
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }

        isRecording = false;
    }

    public boolean isRecording() {
        return isRecording;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void pause() {
        if(streamingSender != null) {
            streamingSender.pause();
        }

        if(mVideoRecorder != null) {
            mVideoRecorder.pause(true);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void resume() {
        if(streamingSender != null) {
            streamingSender.resume();
        }

        if(mVideoRecorder != null) {
            mVideoRecorder.pause(false);
        }
    }

    public boolean isMic() {
        if(audioClient == null) return false;
        return audioClient.isMic();
    }

    /**
     * 是否静音，true 为使用麦克风，false为不是用麦克风
     * @param mute
     */
    public void setMic(boolean mute) {
        if(audioClient != null) {
            audioClient.setMic(mute);
        }
    }
}
