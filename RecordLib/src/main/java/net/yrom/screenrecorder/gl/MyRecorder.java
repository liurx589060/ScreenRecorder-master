package net.yrom.screenrecorder.gl;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import net.yrom.screenrecorder.camera.OnVideoEncodeListener;
import net.yrom.screenrecorder.camera.VideoConfiguration;
import net.yrom.screenrecorder.core.Packager;
import net.yrom.screenrecorder.rtmp.RESFlvData;
import net.yrom.screenrecorder.rtmp.RESFlvDataCollecter;
import net.yrom.screenrecorder.tools.LogTools;
import net.yrom.screenrecorder.tools.VideoMediaCodec;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

import static net.yrom.screenrecorder.rtmp.RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;

@TargetApi(18)
public class MyRecorder {
	private MediaCodec mMediaCodec;
	private InputSurface mInputSurface;
	private RESFlvDataCollecter mListener;
	private boolean mPause;
	private MediaCodec.BufferInfo mBufferInfo;
	private VideoConfiguration mConfiguration;
	private HandlerThread mHandlerThread;
	private Handler mEncoderHandler;
	private ReentrantLock encodeLock = new ReentrantLock();
	private volatile boolean isStarted;

	private static final int TIMEOUT_US = 10000;
	private long startTime = 0;


	public MyRecorder(VideoConfiguration configuration) {
		mConfiguration = configuration;
	}

	public void setRESFlvDataCollecter(RESFlvDataCollecter listener) {
		mListener = listener;
	}

	public void setPause(boolean pause) {
		mPause = pause;
	}

	public void prepareEncoder() {
		if (mMediaCodec != null || mInputSurface != null) {
			throw new RuntimeException("prepareEncoder called twice?");
		}
		mMediaCodec = VideoMediaCodec.getVideoMediaCodec(mConfiguration);
		mHandlerThread = new HandlerThread("SopCastEncode");
		mHandlerThread.start();
		mEncoderHandler = new Handler(mHandlerThread.getLooper());
		mBufferInfo = new MediaCodec.BufferInfo();
		isStarted = true;
	}

	public boolean firstTimeSetup() {
		if (mMediaCodec == null || mInputSurface != null) {
			return false;
		}
		try {
			mInputSurface = new InputSurface(mMediaCodec.createInputSurface());
			mMediaCodec.start();
		} catch (Exception e) {
			releaseEncoder();
			throw (RuntimeException)e;
		}
		return true;
	}

	public void startSwapData() {
		mEncoderHandler.post(swapDataRunnable);
	}

	public void makeCurrent() {
		mInputSurface.makeCurrent();
	}

	public void swapBuffers() {
		if (mMediaCodec == null || mPause) {
			return;
		}
		mInputSurface.swapBuffers();
		mInputSurface.setPresentationTime(System.nanoTime());
	}

	private Runnable swapDataRunnable = new Runnable() {
		@Override
		public void run() {
			drainEncoder();
		}
	};

	public void stop() {
		if (!isStarted) {
			return;
		}
		isStarted = false;
		mEncoderHandler.removeCallbacks(null);
		mHandlerThread.quit();
		encodeLock.lock();
		releaseEncoder();
		encodeLock.unlock();
	}

	private void releaseEncoder() {
		if (mMediaCodec != null) {
			mMediaCodec.signalEndOfInputStream();
			mMediaCodec.stop();
			mMediaCodec.release();
			mMediaCodec = null;
		}
		if (mInputSurface != null) {
			mInputSurface.release();
			mInputSurface = null;
		}
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	public boolean setRecorderBps(int bps) {
		if (mMediaCodec == null || mInputSurface == null) {
			return false;
		}
		LogTools.d("bps :" + bps * 1024);
		Bundle bitrate = new Bundle();
		bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bps * 1024);
		mMediaCodec.setParameters(bitrate);
		return true;
	}

	private void drainEncoder() {
		ByteBuffer[] outBuffers = mMediaCodec.getOutputBuffers();
		while (isStarted) {
			encodeLock.lock();
			if(mMediaCodec != null) {
				int eobIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
				switch (eobIndex) {
					case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
						LogTools.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
						break;
					case MediaCodec.INFO_TRY_AGAIN_LATER:
//                    LogTools.e("VideoSenderThread,MediaCodec.INFO_TRY_AGAIN_LATER");
						break;
					case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
						LogTools.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:" +
								mMediaCodec.getOutputFormat().toString());
						sendAVCDecoderConfigurationRecord(0, mMediaCodec.getOutputFormat());

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
							ByteBuffer realData = mMediaCodec.getOutputBuffers()[eobIndex];
							realData.position(mBufferInfo.offset + 4);
							realData.limit(mBufferInfo.offset + mBufferInfo.size);
							sendRealData((mBufferInfo.presentationTimeUs / 1000) - startTime, realData);
						}
						mMediaCodec.releaseOutputBuffer(eobIndex, false);
						break;
				}
				encodeLock.unlock();
			} else {
				encodeLock.unlock();
				break;
			}
		}
	}

	private void sendAVCDecoderConfigurationRecord(long tms, MediaFormat format) {
		if(mListener == null) return;
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
		mListener.collect(resFlvData, FLV_RTMP_PACKET_TYPE_VIDEO);
	}

	private void sendRealData(long tms, ByteBuffer realData) {
		if(mListener == null) return;
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
		mListener.collect(resFlvData, FLV_RTMP_PACKET_TYPE_VIDEO);
	}
}
