package net.yrom.screenrecorder.operate;

import android.os.Build;
import android.support.annotation.RequiresApi;

import net.yrom.screenrecorder.core.RESAudioClient;
import net.yrom.screenrecorder.core.RESCoreParameters;
import net.yrom.screenrecorder.rtmp.RESFlvData;
import net.yrom.screenrecorder.rtmp.RESFlvDataCollecter;
import net.yrom.screenrecorder.task.RtmpStreamingSender;
import net.yrom.screenrecorder.tools.LogTools;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by daven.liu on 2018/2/27 0027.
 */

public class AudioRecordOpt implements IRecordOpt{
    private static AudioRecordOpt instance = null;
    private RtmpStreamingSender streamingSender;
    private RESCoreParameters coreParameters;
    private RESAudioClient audioClient;
    private ExecutorService executorService;
    private boolean isRecording;

    public static AudioRecordOpt getInstance() {
        if(instance == null) {
            instance = new AudioRecordOpt();
        }
        return instance;
    }

    public void startAudioRecord(RecorderBean recorderBean, RtmpStreamingSender.IRtmpSendCallBack callBack) {
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

        audioClient = new RESAudioClient(coreParameters);
        audioClient.setMic(recorderBean.isMic());
        if (!audioClient.prepare()) {
            LogTools.e("!!!!!audioClient.prepare()failed");
            return;
        }
        audioClient.start(collecter);

        executorService.execute(streamingSender);
        streamingSender.setRtmpSendCallBack(callBack);

        isRecording = true;
    }

    @Override
    public boolean isRecording() {
        return isRecording;
    }

    @Override
    public void stopRecord() {
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void pause() {
        if(streamingSender != null) {
            streamingSender.pause();
        }
    }

    @Override
    public void resume() {
        if(streamingSender != null) {
            streamingSender.resume();
        }
    }

    @Override
    public boolean isMic() {
        if(audioClient == null) return false;
        return audioClient.isMic();
    }

    /**
     * 是否静音，true 为使用麦克风，false为不是用麦克风
     * @param mute
     */
    @Override
    public void setMic(boolean mute) {
        if(audioClient != null) {
            audioClient.setMic(mute);
        }
    }
}
