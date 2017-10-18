package net.yrom.screenrecorder.ui;

import android.app.Activity;
import android.graphics.Color;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.yrom.screenrecorder.R;
import net.yrom.screenrecorder.camera.CameraConfiguration;
import net.yrom.screenrecorder.camera.CameraListener;
import net.yrom.screenrecorder.camera.VideoConfiguration;
import net.yrom.screenrecorder.core.RESAudioClient;
import net.yrom.screenrecorder.core.RESCoreParameters;
import net.yrom.screenrecorder.gl.Watermark;
import net.yrom.screenrecorder.gl.WatermarkPosition;
import net.yrom.screenrecorder.gl.effect.GrayEffect;
import net.yrom.screenrecorder.gl.effect.NullEffect;
import net.yrom.screenrecorder.operate.CameraRecordOpt;
import net.yrom.screenrecorder.operate.RecorderBean;
import net.yrom.screenrecorder.rtmp.RESFlvData;
import net.yrom.screenrecorder.rtmp.RESFlvDataCollecter;
import net.yrom.screenrecorder.task.RtmpStreamingSender;
import net.yrom.screenrecorder.tools.LogTools;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by daven.liu on 2017/10/18 0018.
 */

public class CameraUIHelper {
    public static final int EFFECT_NORMAL = 0x01;//滤镜正常
    public static final int EFFECT_GRAY = 0x02;//去色滤镜
    public static final int CAMERA_BACK = 0x03;//背后摄像机
    public static final int CAMERA_FRONT = 0x04;//前置摄像头
    public static final int FOCUS_AUTO = 0x05;//自动对焦
    public static final int FOCUS_TOUCH = 0x06;//手动对焦

    private Activity context;
    private View liveContentView;
    private View originContentView;
    private View[] views;
    private CameraLivingView cameraLivingView;
    private RecorderBean recorderBean;
    private VideoConfiguration mVideoConfiguration;
    private GestureDetector mGestureDetector;
    private boolean isRecording;

    private GrayEffect mGrayEffect;
    private NullEffect mNullEffect;

    private RtmpStreamingSender streamingSender;
    private RESCoreParameters coreParameters;
    private RESAudioClient audioClient;
    private ExecutorService executorService;

    private boolean isMic;//是否使用麦克风
    private boolean isFlight;//是否使用闪光灯
    private int cameraType;//相机方位
    private int effectType;//滤镜方式
    private int focusType;//对焦方式

    public CameraUIHelper(Activity context,RecorderBean bean) {
        this.context = context;
        this.recorderBean = bean;
        this.isMic = recorderBean.isMic();
        this.isFlight = recorderBean.isFlight();
        this.effectType = recorderBean.getEffectType();
        this.cameraType = recorderBean.getCameraType();
        this.focusType = recorderBean.getFocusType();

        initView();

        CameraRecordOpt.getInstance().setmCameraUIHelper(this);
    }

    private void initView() {
        LayoutInflater inflater = LayoutInflater.from(context);
        liveContentView = inflater.inflate(R.layout.layout_camera_living,null);
        cameraLivingView = (CameraLivingView) liveContentView.findViewById(R.id.liveView);

        initEffects();
        initLiveView();
    }

    /**
     * 设置Activity的布局
     * @param originView
     */
    public void setContentView(View originView) {
        this.originContentView = originView;
        context.setContentView(liveContentView);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        context.addContentView(originView,params);
    }

    /**
     * 设置Activity的布局
     * @param originViewId
     */
    public void setContentView(int originViewId) {
        context.setContentView(liveContentView);
        LayoutInflater inflater = LayoutInflater.from(context);
        originContentView = inflater.inflate(originViewId,null);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        context.addContentView(originContentView,params);
    }

    /**
     * 在Activity中add View
     */
    public void addContentViewWithSelf(View[] views) {
        this.views = views;
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        context.addContentView(liveContentView,params);

        if(views != null && views.length > 0) {
            for(View view:views) {
                context.addContentView(view,params);
            }
        }
    }

    /**
     * 开始直播
     * @return
     */
    public int startRecord() {
        if(recorderBean.getRtmpAddr() == null) {return -1;}

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

        //音频
        audioClient = new RESAudioClient(coreParameters);
        audioClient.setMic(recorderBean.isMic());
        if (!audioClient.prepare()) {
            LogTools.e("!!!!!audioClient.prepare()failed");
            return -1;
        }
        audioClient.start(collecter);

        executorService.execute(streamingSender);
        isRecording = true;
        cameraLivingView.start(collecter);
        return 0;
    }

    /**
     * 停止直播
     */
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
        cameraLivingView.stop();
    }

    /**
     * 释放页面，一般在onDestroy中调用
     */
    public void release() {
       cameraLivingView.stop();
       cameraLivingView.release();
   }

    /**
     * 关闭Activity
     */
   public void destroyWithActivity() {
       context.finish();
   }

    /**
     * 关闭
     */
    public void destroyNoActivity() {
        ((ViewGroup)cameraLivingView.getParent()).removeView(cameraLivingView);
        if(views != null && views.length > 0) {
            for (View view:views) {
                ((ViewGroup)view.getParent()).removeView(view);
            }
        }
    }

    /**
     * 初始化滤镜
     */
    private void initEffects() {
        mGrayEffect = new GrayEffect(context);
        mNullEffect = new NullEffect(context);
    }

    /**
     * 初始化直播页面
     */
    private void initLiveView() {
        cameraLivingView.init();
        CameraConfiguration.Builder cameraBuilder = new CameraConfiguration.Builder();
        if(recorderBean.getWidth() > recorderBean.getHeight()) {
            cameraBuilder.setOrientation(CameraConfiguration.Orientation.LANDSCAPE);
        }else {
            cameraBuilder.setOrientation(CameraConfiguration.Orientation.PORTRAIT);
        }

        if(recorderBean.getCameraType() == CameraUIHelper.CAMERA_BACK) {
            cameraBuilder.setFacing(CameraConfiguration.Facing.BACK);
        }else {
            cameraBuilder.setFacing(CameraConfiguration.Facing.FRONT);
        }

        cameraBuilder.setPreview(recorderBean.getHeight(),recorderBean.getWidth())
                     .setFps(recorderBean.getFps());

        if(recorderBean.getFocusType() == CameraUIHelper.FOCUS_AUTO) {
            cameraBuilder.setFocusMode(CameraConfiguration.FocusMode.AUTO);
        }else {
            cameraBuilder.setFocusMode(CameraConfiguration.FocusMode.TOUCH);
        }
        CameraConfiguration cameraConfiguration = cameraBuilder.build();
        cameraLivingView.setCameraConfiguration(cameraConfiguration);

        VideoConfiguration.Builder videoBuilder = new VideoConfiguration.Builder();
        videoBuilder.setSize(recorderBean.getWidth(), recorderBean.getHeight())
                    .setBps(VideoConfiguration.DEFAULT_MIN_BPS,recorderBean.getBitrate())
                    .setFps(recorderBean.getFps())
                    .setIfi(recorderBean.getIframe_interval());
        mVideoConfiguration = videoBuilder.build();
        cameraLivingView.setVideoConfiguration(mVideoConfiguration);

//        //设置水印
        if(recorderBean.getWaterMakerImg() != null) {
            Watermark watermark = new Watermark(recorderBean.getWaterMakerImg(), 50, 25, WatermarkPosition.WATERMARK_ORIENTATION_BOTTOM_RIGHT, 8, 8);
            cameraLivingView.setWatermark(watermark);
        }

        //设置预览监听
        cameraLivingView.setCameraOpenListener(new CameraListener() {
            @Override
            public void onOpenSuccess() {
                //将上层View设置透明以防遮住camera预览页面
                if(originContentView != null) {
                    originContentView.setBackgroundColor(Color.TRANSPARENT);
                }

                //设置闪光灯
                if(recorderBean.isFlight()) {
                    switchLight(true);
                }else {
                    switchLight(false);
                }

                //设置滤镜
                setEffect(recorderBean.getEffectType());

                if(CameraRecordOpt.getInstance().getCameraCallBack() != null) {
                    CameraRecordOpt.getInstance().getCameraCallBack().onSuccess();
                }
            }

            @Override
            public void onOpenFail(int error) {
                if(CameraRecordOpt.getInstance().getCameraCallBack() != null) {
                    CameraRecordOpt.getInstance().getCameraCallBack().onError();
                }
            }

            @Override
            public void onCameraChange() {
                if(CameraRecordOpt.getInstance().getCameraCallBack() != null) {
                    CameraRecordOpt.getInstance().getCameraCallBack().onSwitchCamera();
                }
            }
        });

        //设置手势识别
        mGestureDetector = new GestureDetector(context, new GestureListener());
        cameraLivingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                return false;
            }
        });
    }

    public boolean isMic() {
        return isMic;
    }

    /**
     * 是否静音
     * @param isMic
     */
    public void setMic(boolean isMic) {
        this.isMic = isMic;
        if(audioClient != null) {
            audioClient.setMic(isMic);
        }
    }

    /**
     * 是否打开闪光灯
     */
    public void switchLight(boolean isFlight) {
        this.isFlight = isFlight;
        cameraLivingView.switchTorch(isFlight);
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if(this.cameraType == CAMERA_BACK) {
            this.cameraType = CAMERA_FRONT;
        }else {
            this.cameraType = CAMERA_BACK;
        }
        cameraLivingView.switchCamera();
    }

    /**
     * 切换滤镜
     */
    public void setEffect(int type) {
        this.effectType = type;
        if(type == EFFECT_NORMAL) {
            cameraLivingView.setEffect(mNullEffect);
        } else {
            cameraLivingView.setEffect(mGrayEffect);
        }
    }

    /**
     * 切换聚焦方式
     */
    public void switchFocusMode() {
        if(this.focusType == FOCUS_AUTO) {
            this.focusType = FOCUS_TOUCH;
        }else {
            this.focusType = FOCUS_TOUCH;
        }
        cameraLivingView.switchFocusMode();
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isFlight() {
        return isFlight;
    }

    public int getCameraType() {
        return cameraType;
    }

    public int getEffectType() {
        return effectType;
    }

    public int getFocusType() {
        return focusType;
    }

    public class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() - e2.getX() > 100
                    && Math.abs(velocityX) > 200) {
                // Fling left
                Toast.makeText(context, "Fling Left", Toast.LENGTH_SHORT).show();
            } else if (e2.getX() - e1.getX() > 100
                    && Math.abs(velocityX) > 200) {
                // Fling right
                Toast.makeText(context, "Fling Right", Toast.LENGTH_SHORT).show();
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    public CameraLivingView getCameraLivingView() {
        return cameraLivingView;
    }
}
