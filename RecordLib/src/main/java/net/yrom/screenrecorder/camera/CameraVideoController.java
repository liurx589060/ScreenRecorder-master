package net.yrom.screenrecorder.camera;

import android.os.Build;
import android.util.Log;

import net.yrom.screenrecorder.gl.MyRecorder;
import net.yrom.screenrecorder.gl.MyRenderer;
import net.yrom.screenrecorder.rtmp.RESFlvDataCollecter;
import net.yrom.screenrecorder.tools.LogTools;

public class CameraVideoController implements IVideoController {
    private MyRecorder mRecorder;
    private MyRenderer mRenderer;
    private RESFlvDataCollecter mListener;
    private VideoConfiguration mVideoConfiguration = VideoConfiguration.createDefault();

    public CameraVideoController(MyRenderer renderer) {
        mRenderer = renderer;
        mRenderer.setVideoConfiguration(mVideoConfiguration);
    }

    public void setVideoConfiguration(VideoConfiguration configuration) {
        mVideoConfiguration = configuration;
        mRenderer.setVideoConfiguration(mVideoConfiguration);
    }

    public void setVideoEncoderListener(RESFlvDataCollecter listener) {
        mListener = listener;
    }

    public void start() {
        if(mListener == null) return;

        mRecorder = new MyRecorder(mVideoConfiguration);
        mRecorder.setRESFlvDataCollecter(mListener);
        mRecorder.prepareEncoder();
        mRenderer.setRecorder(mRecorder);
    }

    public void stop() {
        mRenderer.setRecorder(null);
        if(mRecorder != null) {
            mRecorder.setRESFlvDataCollecter(null);
            mRecorder.stop();
            mRecorder = null;
        }
    }

    public void pause() {
        if(mRecorder != null) {
            mRecorder.setPause(true);
        }
    }

    public void resume() {
        if(mRecorder != null) {
            mRecorder.setPause(false);
        }
    }

    public boolean setVideoBps(int bps) {
        //重新设置硬编bps，在低于19的版本需要重启编码器
        boolean result = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            //由于重启硬编编码器效果不好，此次不做处理
            LogTools.d("Bps need change, but MediaCodec do not support.");
        }else {
            if (mRecorder != null) {
                LogTools.d("Bps change, current bps: " + bps);
                mRecorder.setRecorderBps(bps);
                result = true;
            }
        }
        return result;
    }
}
