package net.yrom.screenrecorder.task;

import android.text.TextUtils;
import android.util.Log;

import net.yrom.screenrecorder.core.RESCoreParameters;
import net.yrom.screenrecorder.operate.RecorderBean;
import net.yrom.screenrecorder.rtmp.FLvMetaData;
import net.yrom.screenrecorder.rtmp.RESFlvData;
import net.yrom.screenrecorder.rtmp.RtmpClient;
import net.yrom.screenrecorder.tools.LogTools;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static net.yrom.screenrecorder.rtmp.RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;

/**
 * Created by raomengyang on 12/03/2017.
 */

public class RtmpStreamingSender implements Runnable {

    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private LinkedBlockingDeque<RESFlvData> frameQueue = new LinkedBlockingDeque<>();
    private final Object syncWriteMsgNum = new Object();
    private FLvMetaData fLvMetaData;
    private RESCoreParameters coreParameters;
    private volatile int state;

    private long jniRtmpPointer = 0;
    private AtomicInteger writeMsgNum = new AtomicInteger(0);
    private String rtmpAddr = null;

    private static class STATE {
        private static final int START = 0;
        private static final int RUNNING = 1;
        private static final int STOPPED = 2;
    }

    public AtomicInteger getWriteMsgNum() {
        return writeMsgNum;
    }

    public RtmpStreamingSender() {
        coreParameters = new RESCoreParameters();
        coreParameters.mediacodecAACBitRate = RESFlvData.AAC_BITRATE;
        coreParameters.mediacodecAACSampleRate = RESFlvData.AAC_SAMPLE_RATE;
        coreParameters.mediacodecAVCFrameRate = RESFlvData.FPS;
        coreParameters.videoWidth = RESFlvData.VIDEO_WIDTH;
        coreParameters.videoHeight = RESFlvData.VIDEO_HEIGHT;

        fLvMetaData = new FLvMetaData(coreParameters);
    }

    public RtmpStreamingSender(RecorderBean bean) {
        coreParameters = new RESCoreParameters();
        coreParameters.mediacodecAACBitRate = RESFlvData.AAC_BITRATE;
        coreParameters.mediacodecAACSampleRate = RESFlvData.AAC_SAMPLE_RATE;
        coreParameters.mediacodecAVCFrameRate = bean.getFps();
        coreParameters.videoWidth = bean.getWidth();
        coreParameters.videoHeight = bean.getHeight();

        fLvMetaData = new FLvMetaData(coreParameters);
    }

    @Override
    public void run() {
        while (!mQuit.get()) {
            if (frameQueue.size() > 0) {
                switch (state) {
                    case STATE.START:
                        LogTools.d("RESRtmpSender,WorkHandler,tid=" + Thread.currentThread().getId());
                        if (TextUtils.isEmpty(rtmpAddr)) {
                            LogTools.e("rtmp address is null!");
                            break;
                        }
                        jniRtmpPointer = RtmpClient.open(rtmpAddr, true);
                        final int openR = jniRtmpPointer == 0 ? 1 : 0;
                        String serverIpAddr = null;
                        if (openR == 0) {
                            serverIpAddr = RtmpClient.getIpAddr(jniRtmpPointer);
                            LogTools.d("server ip address = " + serverIpAddr);
                        }
                        if (jniRtmpPointer == 0) {
                            break;
                        } else {
                            byte[] MetaData = fLvMetaData.getMetaData();
                            RtmpClient.write(jniRtmpPointer,
                                    MetaData,
                                    MetaData.length,
                                    RESFlvData.FLV_RTMP_PACKET_TYPE_INFO, 0);
                            state = STATE.RUNNING;
                        }
                        break;
                    case STATE.RUNNING:
                        if (state != STATE.RUNNING) {
                            break;
                        }
//                        RESFlvData flvData = frameQueue.pop();
                        RESFlvData flvData = frameQueue.poll();
                        if (writeMsgNum.get() > 5 && flvData.flvTagType == RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO && flvData.droppable) {
                            LogTools.d("senderQueue is crowded,abandon video");
                            break;
                        }
                        final int res = RtmpClient.write(jniRtmpPointer, flvData.byteBuffer, flvData.byteBuffer.length, flvData.flvTagType, flvData.dts);
                        if (res == 0) {
                            if (flvData.flvTagType == RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO) {
                                LogTools.d("video frame sent = " + flvData.size);
                                writeMsgNum.getAndDecrement();
                            } else {
                                LogTools.d("audio frame sent = " + flvData.size);
                            }
                        } else {
                            LogTools.e("writeError = " + res);
                        }

                        break;
                    case STATE.STOPPED:
                        if (state == STATE.STOPPED || jniRtmpPointer == 0) {
                            break;
                        }
                        final int closeR = RtmpClient.close(jniRtmpPointer);
                        serverIpAddr = null;
                        LogTools.e("close result = " + closeR);
                        break;
                }

            }

        }
        final int closeR = RtmpClient.close(jniRtmpPointer);
        LogTools.e("close result = " + closeR);
    }

    public void sendStart(String rtmpAddr) {
        synchronized (syncWriteMsgNum) {
            writeMsgNum.set(0);
        }
        this.rtmpAddr = rtmpAddr;
        state = STATE.START;
    }

    public void sendStop() {
        synchronized (syncWriteMsgNum) {
            writeMsgNum.set(0);
        }
        state = STATE.STOPPED;
    }

    public void sendFood(RESFlvData flvData, int type) {
        synchronized (syncWriteMsgNum) {
            //LAKETODO optimize
            frameQueue.add(flvData);
            if(type == FLV_RTMP_PACKET_TYPE_VIDEO) {
                writeMsgNum.getAndIncrement();
            }
        }
    }


    public final void quit() {
        mQuit.set(true);
    }
}
