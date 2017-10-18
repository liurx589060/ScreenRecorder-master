package net.yrom.screenrecorder.ui;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.os.Build;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.util.Log;

import net.yrom.screenrecorder.camera.CameraConfiguration;
import net.yrom.screenrecorder.camera.CameraData;
import net.yrom.screenrecorder.camera.CameraHolder;
import net.yrom.screenrecorder.camera.CameraListener;
import net.yrom.screenrecorder.camera.CameraVideoController;
import net.yrom.screenrecorder.camera.VideoConfiguration;
import net.yrom.screenrecorder.gl.Watermark;
import net.yrom.screenrecorder.gl.effect.Effect;
import net.yrom.screenrecorder.rtmp.RESFlvDataCollecter;
import net.yrom.screenrecorder.tools.LogTools;
import net.yrom.screenrecorder.tools.MediaCodecHelper;
import net.yrom.screenrecorder.tools.ThreadUtils;
import net.yrom.screenrecorder.tools.VideoMediaCodec;
import net.yrom.screenrecorder.tools.WeakHandler;

public class CameraLivingView extends CameraView {
    public static final int NO_ERROR = 0;
    public static final int VIDEO_TYPE_ERROR = 1;
    public static final int AUDIO_TYPE_ERROR = 2;
    public static final int VIDEO_CONFIGURATION_ERROR = 3;
    public static final int AUDIO_CONFIGURATION_ERROR = 4;
    public static final int CAMERA_ERROR = 5;
    public static final int AUDIO_ERROR = 6;
    public static final int AUDIO_AEC_ERROR = 7;
    public static final int SDK_VERSION_ERROR = 8;

    private Context mContext;
    private PowerManager.WakeLock mWakeLock;
    private VideoConfiguration mVideoConfiguration = VideoConfiguration.createDefault();
    private CameraListener mOutCameraOpenListener;
    private LivingStartListener mLivingStartListener;
    private WeakHandler mHandler = new WeakHandler();
    private CameraVideoController videoController;

    public interface LivingStartListener {
        void startError(int error);
        void startSuccess();
    }

    public CameraLivingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        mContext = context;
    }

    public CameraLivingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
        mContext = context;
    }

    public CameraLivingView(Context context) {
        super(context);
        initView();
        mContext = context;
    }

    private void initView() {
        videoController = new CameraVideoController(mRenderer);
        mRenderer.setCameraOpenListener(mCameraOpenListener);
    }

    public void init() {
        PowerManager mPowerManager = ((PowerManager) mContext.getSystemService(getContext().POWER_SERVICE));
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                PowerManager.ON_AFTER_RELEASE, "CameraLivingView");
    }

    public void setVideoConfiguration(VideoConfiguration videoConfiguration) {
        mVideoConfiguration = videoConfiguration;
        videoController.setVideoConfiguration(mVideoConfiguration);
    }

    public void setCameraConfiguration(CameraConfiguration cameraConfiguration) {
        CameraHolder.instance().setConfiguration(cameraConfiguration);
    }

    private int check() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            LogTools.d("Android sdk version error");
            return SDK_VERSION_ERROR;
        }

        if(!isCameraOpen()) {
            LogTools.d("The camera have not open");
            return CAMERA_ERROR;
        }
        MediaCodecInfo videoMediaCodecInfo = MediaCodecHelper.selectCodec(mVideoConfiguration.mime);
        if(videoMediaCodecInfo == null) {
            LogTools.d("Video type error");
            return VIDEO_TYPE_ERROR;
        }

        MediaCodec videoMediaCodec = VideoMediaCodec.getVideoMediaCodec(mVideoConfiguration);
        if(videoMediaCodec == null) {
            LogTools.d("Video mediacodec configuration error");
            return VIDEO_CONFIGURATION_ERROR;
        }
        return NO_ERROR;
    }

    public void start(final RESFlvDataCollecter collecter) {
        ThreadUtils.processNotUI(new ThreadUtils.INotUIProcessor() {
            @Override
            public void process() {
                final int result = check();
                if(result == NO_ERROR) {
                    if(mLivingStartListener != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mLivingStartListener.startSuccess();
                            }
                        });
                    }
                    screenOn();
                    videoController.setVideoEncoderListener(collecter);
                    videoController.start();
                } else {
                    if(mLivingStartListener != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mLivingStartListener.startError(result);
                            }
                        });
                    }
                }
            }
        });
    }

    public void stop() {
        screenOff();
        videoController.stop();
    }

    private void screenOn() {
        if(mWakeLock != null) {
            if (!mWakeLock.isHeld()) {
                mWakeLock.acquire();
            }
        }
    }

    private void screenOff() {
        if(mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }

    public void setEffect(Effect effect) {
        mRenderSurfaceView.setEffect(effect);
    }

    public void setWatermark(Watermark watermark) {
        mRenderer.setWatermark(watermark);
    }

    private boolean isCameraOpen() {
        return mRenderer.isCameraOpen();
    }

    public void setCameraOpenListener(CameraListener cameraOpenListener) {
        mOutCameraOpenListener = cameraOpenListener;
    }

    public void switchCamera() {
        boolean change = CameraHolder.instance().switchCamera();
        if(change) {
            changeFocusModeUI();
            if(mOutCameraOpenListener != null) {
                mOutCameraOpenListener.onCameraChange();
            }
        }
    }

    public CameraData getCameraData() {
        return CameraHolder.instance().getCameraData();
    }

    public void switchFocusMode() {
        CameraHolder.instance().switchFocusMode();
        changeFocusModeUI();
    }

    public void switchTorch(boolean isFlight) {
        CameraHolder.instance().switchLight(isFlight);
    }

    public void release() {
        screenOff();
        mWakeLock = null;
        CameraHolder.instance().releaseCamera();
        CameraHolder.instance().release();
    }

    private CameraListener mCameraOpenListener = new CameraListener() {
        @Override
        public void onOpenSuccess() {
            changeFocusModeUI();
            if(mOutCameraOpenListener != null) {
                mOutCameraOpenListener.onOpenSuccess();
            }
        }

        @Override
        public void onOpenFail(int error) {
            if(mOutCameraOpenListener != null) {
                mOutCameraOpenListener.onOpenFail(error);
            }
        }

        @Override
        public void onCameraChange() {
            // Won't Happen
        }
    };
}
