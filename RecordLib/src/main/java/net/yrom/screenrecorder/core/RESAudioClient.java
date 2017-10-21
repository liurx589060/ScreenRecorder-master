package net.yrom.screenrecorder.core;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.os.Build;
import android.util.Log;

import com.example.libspeex.SpeexNative;

import net.yrom.screenrecorder.operate.ScreenRecordOpt;
import net.yrom.screenrecorder.rtmp.RESFlvDataCollecter;
import net.yrom.screenrecorder.tools.LogTools;

import java.util.Arrays;


/**
 * Created by lake on 16-5-24.
 */
public class RESAudioClient {
    RESCoreParameters resCoreParameters;
    private final Object syncOp = new Object();
    private AudioRecordThread audioRecordThread;
    private AudioRecord audioRecord;
    private byte[] audioBuffer;
    private RESSoftAudioCore softAudioCore;
    private AcousticEchoCanceler mAcousticEchoCanceler;
    private boolean isMic = true;

    private final int SPEEX_FRAME_SIZE = 160;
    private final int SPEEX_FILTER_LENGTH = 160*25;
    private final int SPEEX_SIMPLING_RATE = 16000;

    public RESAudioClient(RESCoreParameters parameters) {
        resCoreParameters = parameters;
    }

    public boolean prepare() {
        synchronized (syncOp) {
            resCoreParameters.audioBufferQueueNum = 5;
            softAudioCore = new RESSoftAudioCore(resCoreParameters);
            if (!softAudioCore.prepare()) {
                LogTools.e("RESAudioClient,prepare");
                return false;
            }
            resCoreParameters.audioRecoderFormat = AudioFormat.ENCODING_PCM_16BIT;
            resCoreParameters.audioRecoderChannelConfig = AudioFormat.CHANNEL_IN_MONO;
            resCoreParameters.audioRecoderSliceSize = resCoreParameters.mediacodecAACSampleRate / 10;
            resCoreParameters.audioRecoderBufferSize = resCoreParameters.audioRecoderSliceSize * 2;
            resCoreParameters.audioRecoderSource = MediaRecorder.AudioSource.DEFAULT;
            resCoreParameters.audioRecoderSampleRate = resCoreParameters.mediacodecAACSampleRate;
            prepareAudio();
            return true;
        }
    }

    public boolean start(RESFlvDataCollecter flvDataCollecter) {
        synchronized (syncOp) {
            softAudioCore.start(flvDataCollecter);
            audioRecord.startRecording();
            audioRecordThread = new AudioRecordThread();
            audioRecordThread.start();
            //LibSpeex的使用,初始化
            SpeexNative.nativeInitEcho(SPEEX_FRAME_SIZE,SPEEX_FILTER_LENGTH,SPEEX_SIMPLING_RATE);
            SpeexNative.nativeInitDeNose(SPEEX_FRAME_SIZE,SPEEX_FILTER_LENGTH,SPEEX_SIMPLING_RATE);
            LogTools.d("RESAudioClient,start()");
            return true;
        }
    }

    public boolean stop() {
        synchronized (syncOp) {
            audioRecordThread.quit();
            try {
                audioRecordThread.join();
            } catch (InterruptedException ignored) {
            }
            softAudioCore.stop();
            audioRecordThread = null;
            audioRecord.stop();
            //LibSpeex的使用,停止
            SpeexNative.nativeCloseEcho();
            SpeexNative.nativeCloseDeNose();
            return true;
        }
    }

    public boolean destroy() {
        synchronized (syncOp) {
            audioRecord.release();
            return true;
        }
    }
    public void setSoftAudioFilter(BaseSoftAudioFilter baseSoftAudioFilter) {
        softAudioCore.setAudioFilter(baseSoftAudioFilter);
    }
    public BaseSoftAudioFilter acquireSoftAudioFilter() {
        return softAudioCore.acquireAudioFilter();
    }

    public void releaseSoftAudioFilter() {
        softAudioCore.releaseAudioFilter();
    }

    /**
     * 消除回声
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void enableAcousticEchoCanceler() {
        mAcousticEchoCanceler = AcousticEchoCanceler.create(audioRecord .getAudioSessionId() );
        if( mAcousticEchoCanceler.isAvailable() ) {
            // enable echo canceller
            mAcousticEchoCanceler.setEnabled( true );
        }
    }

    private boolean prepareAudio() {
        int minBufferSize = AudioRecord.getMinBufferSize(resCoreParameters.audioRecoderSampleRate,
                resCoreParameters.audioRecoderChannelConfig,
                resCoreParameters.audioRecoderFormat);
        audioRecord = new AudioRecord(resCoreParameters.audioRecoderSource,
                resCoreParameters.audioRecoderSampleRate,
                resCoreParameters.audioRecoderChannelConfig,
                resCoreParameters.audioRecoderFormat,
                minBufferSize * 5);
        audioBuffer = new byte[resCoreParameters.audioRecoderBufferSize];
        if (AudioRecord.STATE_INITIALIZED != audioRecord.getState()) {
            LogTools.e("audioRecord.getState()!=AudioRecord.STATE_INITIALIZED!");
            return false;
        }
        if (AudioRecord.SUCCESS != audioRecord.setPositionNotificationPeriod(resCoreParameters.audioRecoderSliceSize)) {
            LogTools.e("AudioRecord.SUCCESS != audioRecord.setPositionNotificationPeriod(" + resCoreParameters.audioRecoderSliceSize + ")");
            return false;
        }
        enableAcousticEchoCanceler();
        return true;
    }

    public boolean isMic() {
        return isMic;
    }

    public void setMic(boolean mic) {
        isMic = mic;
    }

    class AudioRecordThread extends Thread {
        private boolean isRunning = true;

        AudioRecordThread() {
            isRunning = true;
        }

        public void quit() {
            isRunning = false;
            setMic(true);
        }

        @Override
        public void run() {
            LogTools.d("AudioRecordThread,tid=" + Thread.currentThread().getId());
            while (isRunning) {
                int size = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                if (!isMic()) {
                    byte clearM = 0;
                    Arrays.fill(audioBuffer, clearM);
                }
                if (isRunning && softAudioCore != null && size > 0) {
//                    byte[] outBuffer = new byte[audioBuffer.length];
//                    byte[] playBuffer = new byte[audioBuffer.length];
//                    byte clearM = 0;
//                    Arrays.fill(playBuffer, clearM);
//                    Arrays.fill(outBuffer, clearM);
//                    SpeexNative.nativeProcEcho(audioBuffer,playBuffer,outBuffer);
//                    softAudioCore.queueAudio(audioBuffer);

//                    SpeexNative.nativeProcDeNose16K(audioBuffer);
                    softAudioCore.queueAudio(audioBuffer);

//                    softAudioCore.queueAudio(audioBuffer);
                }
            }
        }
    }
}
