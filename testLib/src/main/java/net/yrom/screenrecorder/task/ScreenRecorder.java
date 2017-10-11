/*
 * Copyright (c) 2014 Yrom Wang <http://www.yrom.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.yrom.screenrecorder.task;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import net.yrom.screenrecorder.core.Packager;
import net.yrom.screenrecorder.operate.RecorderBean;
import net.yrom.screenrecorder.rtmp.RESFlvData;
import net.yrom.screenrecorder.rtmp.RESFlvDataCollecter;
import net.yrom.screenrecorder.tools.LogTools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.yrom.screenrecorder.rtmp.RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;

/**
 * @author Yrom
 * Modified by raomengyang 2017-03-12
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class ScreenRecorder extends Thread {
    private static final String TAG = "ScreenRecorder";
    private MediaProjection mMediaProjection;
    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int TIMEOUT_US = 10000;

    private MediaCodec mEncoder;
    private Surface mSurface;
    private long startTime = 0;
    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private VirtualDisplay mVirtualDisplay;
    private RESFlvDataCollecter mDataCollecter;

    private MediaMuxer mMuxer;
    private int mVideoTrackIndex = -1;
    private RecorderBean recorderBean;

    public ScreenRecorder(RESFlvDataCollecter dataCollecter, RecorderBean bean, MediaProjection mp) {
        super(TAG);
        this.recorderBean = bean;
        mMediaProjection = mp;
        startTime = 0;
        mDataCollecter = dataCollecter;
    }

    /**
     * stop task
     */
    public final void quit() {
        mQuit.set(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void run() {
        try {
            try {
                prepareEncoder();
//                mMuxer = new MediaMuxer(Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display",
                    recorderBean.getWidth(), recorderBean.getHeight(), recorderBean.getDpi(), DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    mSurface, null, null);
            Log.d(TAG, "created virtual display: " + mVirtualDisplay);
            recordVirtualDisplay();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            release();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void prepareEncoder() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, recorderBean.getWidth(), recorderBean.getHeight());
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, recorderBean.getBitrate());
        format.setInteger(MediaFormat.KEY_FRAME_RATE, recorderBean.getFps());
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, recorderBean.getIframe_interval());
        Log.d(TAG, "created video format: " + format);
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mEncoder.createInputSurface();
        Log.d(TAG, "created input surface: " + mSurface);
        mEncoder.start();
    }

    private void recordVirtualDisplay() {
        while (!mQuit.get()) {
            int eobIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
            switch (eobIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    LogTools.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
//                    LogTools.e("VideoSenderThread,MediaCodec.INFO_TRY_AGAIN_LATER");
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    LogTools.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:" +
                            mEncoder.getOutputFormat().toString());
                    sendAVCDecoderConfigurationRecord(0, mEncoder.getOutputFormat());

//                    if(mMuxer != null) {
//                       mVideoTrackIndex = mMuxer.addTrack(mEncoder.getOutputFormat());
//                        mMuxer.start();
//                    }
                    break;
                default:
                    LogTools.d("VideoSenderThread,MediaCode,eobIndex=" + eobIndex);
                    if (startTime == 0) {
                        startTime = mBufferInfo.presentationTimeUs / 1000;
                    }
                    /**
                     * we send sps pps already in INFO_OUTPUT_FORMAT_CHANGED
                     * so we ignore MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                     */
                    if (mBufferInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG && mBufferInfo.size != 0) {
                        ByteBuffer realData = mEncoder.getOutputBuffers()[eobIndex];
                        realData.position(mBufferInfo.offset + 4);
                        realData.limit(mBufferInfo.offset + mBufferInfo.size);
                        sendRealData((mBufferInfo.presentationTimeUs / 1000) - startTime, realData);

//                        if(mMuxer != null) {
//                            mMuxer.writeSampleData(mVideoTrackIndex,realData,mBufferInfo);
//                        }
                    }
                    mEncoder.releaseOutputBuffer(eobIndex, false);
                    break;
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void release() {
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }

//        if(mMuxer != null) {
//            mMuxer.stop();
//            mMuxer.release();
//            mMuxer = null;
//        }
    }


    public final boolean getStatus() {
        return !mQuit.get();
    }


    private void sendAVCDecoderConfigurationRecord(long tms, MediaFormat format) {
        byte[] AVCDecoderConfigurationRecord = Packager.H264Packager.generateAVCDecoderConfigurationRecord(format);
        int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                AVCDecoderConfigurationRecord.length;
        byte[] finalBuff = new byte[packetLen];
        Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                0,
                true,
                true,
                AVCDecoderConfigurationRecord.length);
        System.arraycopy(AVCDecoderConfigurationRecord, 0,
                finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH, AVCDecoderConfigurationRecord.length);
        RESFlvData resFlvData = new RESFlvData();
        resFlvData.droppable = false;
        resFlvData.byteBuffer = finalBuff;
        resFlvData.size = finalBuff.length;
        resFlvData.dts = (int) tms;
        resFlvData.flvTagType = FLV_RTMP_PACKET_TYPE_VIDEO;
        resFlvData.videoFrameType = RESFlvData.NALU_TYPE_IDR;
        mDataCollecter.collect(resFlvData, FLV_RTMP_PACKET_TYPE_VIDEO);
    }

    private void sendRealData(long tms, ByteBuffer realData) {
        int realDataLength = realData.remaining();
        int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                Packager.FLVPackager.NALU_HEADER_LENGTH +
                realDataLength;
        byte[] finalBuff = new byte[packetLen];
        realData.get(finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                        Packager.FLVPackager.NALU_HEADER_LENGTH,
                realDataLength);
        int frameType = finalBuff[Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                Packager.FLVPackager.NALU_HEADER_LENGTH] & 0x1F;
        Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                0,
                false,
                frameType == 5,
                realDataLength);
        RESFlvData resFlvData = new RESFlvData();
        resFlvData.droppable = false;
        resFlvData.byteBuffer = finalBuff;
        resFlvData.size = finalBuff.length;
        resFlvData.dts = (int) tms;
        resFlvData.flvTagType = FLV_RTMP_PACKET_TYPE_VIDEO;
        resFlvData.videoFrameType = frameType;
        mDataCollecter.collect(resFlvData, FLV_RTMP_PACKET_TYPE_VIDEO);
    }
}
