package net.yrom.screenrecorder.tools;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import net.yrom.screenrecorder.camera.BlackListHelper;
import net.yrom.screenrecorder.camera.VideoConfiguration;
import net.yrom.screenrecorder.gl.SopCastConstant;

/**
 * @Title: VideoMediaCodec
 * @Package com.laifeng.sopcastsdk.hw
 * @Description:
 * @Author Jim
 * @Date 16/6/2
 * @Time 下午6:07
 * @Version
 */
@TargetApi(18)
public class VideoMediaCodec {

    public static MediaCodec getVideoMediaCodec(VideoConfiguration videoConfiguration) {
        int videoWidth = getVideoSize(videoConfiguration.width);
        int videoHeight = getVideoSize(videoConfiguration.height);
        MediaFormat format = MediaFormat.createVideoFormat(videoConfiguration.mime, videoWidth, videoHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, videoConfiguration.maxBps);
        int fps = videoConfiguration.fps;
        //设置摄像头预览帧率
        if(BlackListHelper.deviceInFpsBlacklisted()) {
            LogTools.d("Device in fps setting black list, so set mediacodec fps 15");
            fps = 15;
        }
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, videoConfiguration.ifi);
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        format.setInteger(MediaFormat.KEY_COMPLEXITY, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
        MediaCodec mediaCodec = null;

        try {
            mediaCodec = MediaCodec.createEncoderByType(videoConfiguration.mime);
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        }catch (Exception e) {
            e.printStackTrace();
            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            }
        }
        return mediaCodec;
    }

    // We avoid the device-specific limitations on width and height by using values that
    // are multiples of 16, which all tested devices seem to be able to handle.
    public static int getVideoSize(int size) {
        int multiple = (int) Math.ceil(size/16.0);
        return multiple*16;
    }
}
