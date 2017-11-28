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

/**
 * Created by raomengyang on 12/03/2017.
 */

public class RtmpStreamingSender implements Runnable {

    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private LinkedBlockingDeque<RESFlvData> frameQueue = new LinkedBlockingDeque<RESFlvData>();
    private final Object syncWriteMsgNum = new Object();
    private FLvMetaData fLvMetaData;
    private RESCoreParameters coreParameters;
    private volatile int state;
    private IRtmpSendCallBack rtmpSendCallBack;
    private int sendErrorCount = 0;

    private long jniRtmpPointer = 0;
    private String rtmpAddr = null;
    private RtmpClient rtmpClient;
    private boolean isPause;
    private boolean isFull;
    private long totalSize = 0;//数据所占内存
    private final long customRestSize = 80*1024*1024;//需要余留的内存数80M
    private final long defautRerestMaxSize =  80*1024*1024;
    private final int LIMIT_REST_WRITENUM = 500;//达到500提示网络差
    private AtomicInteger restWriteNum = new AtomicInteger(0);

    public void setRtmpSendCallBack(IRtmpSendCallBack rtmpSendCallBack) {
        this.rtmpSendCallBack = rtmpSendCallBack;
    }

    private static class STATE {
        private static final int START = 0;
        private static final int RUNNING = 1;
        private static final int STOPPED = 2;
    }

    public interface IRtmpSendCallBack {
        public void sendError();
        public void connectError();
        public void netBad();
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

        getRestMemory();
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

        getRestMemory();
    }

    @Override
    public void run() {
        while (!mQuit.get() || (mQuit.get() && frameQueue.size() > 0)) {
            if (frameQueue.size() > 0) {
                if(isPause || isFull) {//暂停
                    if(totalSize < customRestSize) {
                        isFull = false;
                        clearQueue();
                    }else {
                        RESFlvData flvData = frameQueue.poll();
                        if(flvData != null) {
                            totalSize -= flvData.byteBuffer.length;
                        }
                    }
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
                            Log.e(LogTools.TAG,"openfail=" + e.getMessage());
                            e.printStackTrace();
                            if(this.rtmpSendCallBack != null) {//连接失败
                                this.rtmpSendCallBack.connectError();
                            }
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
                                e.printStackTrace();
                            }
                            state = STATE.RUNNING;
                        }
                        break;
                    case STATE.RUNNING:
                        if (state != STATE.RUNNING) {
                            break;
                        }
                        RESFlvData flvData = frameQueue.poll();
                        if(flvData == null) break;
                        totalSize -= flvData.byteBuffer.length;
                        if (totalSize > getRestMemory()) {
                            LogTools.d("senderQueue is crowded,abandon video");
                            Log.e("yy","senderQueue is crowded,abandon video");
                            isFull = true;
                            break;
                        }
                        int res = -1;
                        try {
                            res = rtmpClient.write(jniRtmpPointer, flvData.byteBuffer, flvData.byteBuffer.length, flvData.flvTagType, flvData.dts);
                        } catch (IOException e) {
                            Log.e(LogTools.TAG,"writeError = " + res);
                            e.printStackTrace();
                        }

                        if(res != 0) {//不成功
                            sendErrorCount ++;
                        }else {
                            sendErrorCount = 0;
                            if(flvData.flvTagType == RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO) {
                                restWriteNum.getAndDecrement();
                            }
                        }

                        Log.e("zz","sendErrorCount=" + sendErrorCount);
                        if(this.rtmpSendCallBack != null && sendErrorCount == 1000) {
                            this.rtmpSendCallBack.sendError();
                        }

                        if(this.rtmpSendCallBack != null && restWriteNum.get() >= LIMIT_REST_WRITENUM) {//网络较差
                            this.rtmpSendCallBack.netBad();
                            restWriteNum.set(0);
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
        rtmpClient.close();
    }

    public void sendStart(String rtmpAddr) {
        this.rtmpAddr = rtmpAddr;
        state = STATE.START;
    }

    public void sendStop() {
        state = STATE.STOPPED;
    }

    public void sendFood(RESFlvData flvData, int type) {
        if(flvData == null) return;
        synchronized (syncWriteMsgNum) {
            //LAKETODO optimize
            if(isPause || isFull) return;
            LogTools.d("restMemory=" + (getRestMemory() - totalSize)/1024/1024);
            frameQueue.add(flvData);
            totalSize += flvData.byteBuffer.length;
            LogTools.d("restWriteNum=" + restWriteNum.get());
            if(type == RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO) {
                restWriteNum.getAndIncrement();
            }
        }
    }


    public final void quit() {
        mQuit.set(true);
        isPause = false;
    }

    public void pause() {
        isPause = true;
    }

    public void resume() {
        isPause = false;
        clearQueue();
    }

    private void clearQueue() {
        if(frameQueue == null) return;
        frameQueue.clear();
        totalSize = 0;
        restWriteNum.set(0);
    }

    public long getRestMemory() {
        try {
            Runtime rt=Runtime.getRuntime();
            long maxMemory=rt.maxMemory();
//            Log.e(LogTools.TAG,"maxMemory=" + maxMemory + "---" + maxMemory/1024/1024 + "M");
            long totalMemory = rt.totalMemory();
//            Log.e(LogTools.TAG,"totalMemory=" + totalMemory + "---" + totalMemory/1024/1024 + "M");
//            Log.e(LogTools.TAG,"restMemory=" + (maxMemory - totalMemory) + "---" + (maxMemory - totalMemory)/1024/1024 + "M");
            return (maxMemory - totalMemory) - customRestSize;
        }catch (Exception e) {
            Log.e(LogTools.TAG,e.getMessage());
            return defautRerestMaxSize;
        }
    }
}
