package net.yrom.screenrecorder.task;

import android.text.TextUtils;
import android.util.Log;

import net.yrom.screenrecorder.core.RESCoreParameters;
import net.yrom.screenrecorder.operate.RecorderBean;
import net.yrom.screenrecorder.rtmp.FLvMetaData;
import net.yrom.screenrecorder.rtmp.RESFlvData;
import net.yrom.screenrecorder.rtmp.RtmpClient;
import net.yrom.screenrecorder.tools.LogTools;

import java.io.IOException;
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
    private RtmpClient rtmpClient;
    private boolean isPause;

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
        rtmpClient = new RtmpClient();
        Log.e("yy","" + rtmpClient);
    }

    public RtmpStreamingSender(RecorderBean bean) {
        coreParameters = new RESCoreParameters();
        coreParameters.mediacodecAACBitRate = RESFlvData.AAC_BITRATE;
        coreParameters.mediacodecAACSampleRate = RESFlvData.AAC_SAMPLE_RATE;
        coreParameters.mediacodecAVCFrameRate = bean.getFps();
        coreParameters.videoWidth = bean.getWidth();
        coreParameters.videoHeight = bean.getHeight();

        fLvMetaData = new FLvMetaData(coreParameters);
        rtmpClient = new RtmpClient();
        Log.e("yy","" + rtmpClient);
    }

    @Override
    public void run() {
        while (!mQuit.get()) {
            if (frameQueue.size() > 0) {
                if(isPause) {//暂停
                    continue;
                }
                switch (state) {
                    case STATE.START:
                        LogTools.d("RESRtmpSender,WorkHandler,tid=" + Thread.currentThread().getId());
                        if (TextUtils.isEmpty(rtmpAddr)) {
                            LogTools.e("rtmp address is null!");
                            break;
                        }
                        try {
                           rtmpClient.open(rtmpAddr, true);
                        } catch (RtmpClient.RtmpIOException e) {
                            Log.e("yy","openfail=" + e.toString());
                            e.printStackTrace();
                        }

                        if (rtmpClient.getRtmpPointer() == 0) {
                            break;
                        } else {
                            byte[] MetaData = fLvMetaData.getMetaData();
                            try {
                                rtmpClient.write(jniRtmpPointer,
                                        MetaData,
                                        MetaData.length,
                                        RESFlvData.FLV_RTMP_PACKET_TYPE_INFO, 0);
                            } catch (IOException e) {
                                Log.e("yy","writefail=" + e.toString());
                                e.printStackTrace();
                            }
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
                        int res = -1;
                        try {
                            res = rtmpClient.write(jniRtmpPointer, flvData.byteBuffer, flvData.byteBuffer.length, flvData.flvTagType, flvData.dts);
                        } catch (IOException e) {
                            Log.e("yy","running--writefail=" + e.toString());
                            e.printStackTrace();
                        }
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
//                        if (state == STATE.STOPPED || jniRtmpPointer == 0) {
//                            Log.e("zz","stop=" + jniRtmpPointer);
//                            final int closeR = RtmpClient.close(jniRtmpPointer);
//                            serverIpAddr = null;
//                            LogTools.e("close result = " + closeR);
//                            quit();
//                        }
                        break;
                }

            }

        }
        rtmpClient.close();;
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
            if(isPause) return;
            frameQueue.add(flvData);
            if(type == FLV_RTMP_PACKET_TYPE_VIDEO) {
                writeMsgNum.getAndIncrement();
            }
        }
    }


    public final void quit() {
        mQuit.set(true);
        isPause = false;
    }

    public void pause() {
        isPause = true;
        rtmpClient.pause(true);
    }

    public void resume() {
        isPause = false;
        rtmpClient.pause(false);
    }
}
