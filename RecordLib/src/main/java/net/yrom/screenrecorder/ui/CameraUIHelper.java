package net.yrom.screenrecorder.ui;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
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
    public static final int EFFECT_NORMAL = 0x01;//???????
    public static final int EFFECT_GRAY = 0x02;//?????
    public static final int CAMERA_BACK = 0x03;//?????????
    public static final int CAMERA_FRONT = 0x04;//????????
    public static final int FOCUS_AUTO = 0x05;//??????
    public static final int FOCUS_TOUCH = 0x06;//??????

    private Activity context;
    private View liveContentView;
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

    private boolean isMic;//??????????
    private boolean isFlight;//???????????
    private int cameraType;//?????λ
    private int effectType;//??????
    private int focusType;//??????

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
     * ????Activity?????
     * @param originView
     */
    public void setContentView(View originView) {
        context.setContentView(liveContentView);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if(originView!= null) {
            originView.setBackgroundColor(Color.TRANSPARENT);
        }
        context.addContentView(originView,params);
    }

    /**
     * ????Activity?????
     * @param originViewId
     */
    public void setContentView(int originViewId) {
        context.setContentView(liveContentView);
        LayoutInflater inflater = LayoutInflater.from(context);
        View originView = inflater.inflate(originViewId,null);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if(originView!= null) {
            originView.setBackgroundColor(Color.TRANSPARENT);
        }
        context.addContentView(originView,params);
    }

    /**
     * ??Activity??add View
     */
    public void addContentViewWithSelf(View[] views) {
        this.views = views;
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        context.addContentView(liveContentView,params);

        if(views != null && views.length > 0) {
            for(View view:views) {
                context.addContentView(view,params);
                view.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }

    /**
     * ??????
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

        //???
        audioClient = new RESAudioClient(coreParameters);
        audioClient.setMic(this.isMic());
        if (!audioClient.prepare()) {
            LogTools.e("!!!!!audioClient.prepare()failed");
            return -1;
        }
        audioClient.start(collecter);

        executorService.execute(streamingSender);
        isRecording = true;
        cameraLivingView.start(collecter);

        if(CameraRecordOpt.getInstance().getCameraCallBack() != null) {
            CameraRecordOpt.getInstance().getCameraCallBack().onLiveStart();
        }

        return 0;
    }

    /**
     * ?????
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

        cameraLivingView.stop();

        if(CameraRecordOpt.getInstance().getCameraCallBack() != null && isRecording()) {
            CameraRecordOpt.getInstance().getCameraCallBack().onLiveStop();
        }

        isRecording = false;
    }

    /**
     * ?????棬?????onDestroy?е???
     */
    public void onDestroy() {
       stopRecord();
       cameraLivingView.stop();
       cameraLivingView.release();
   }

    /**
     * ???Activity
     */
   public void destroyWithActivity() {
       context.finish();
   }

    /**
     * ???
     */
    public void destroyNoActivity() {
        onDestroy();
        ((ViewGroup)cameraLivingView.getParent()).removeView(cameraLivingView);
        if(views != null && views.length > 0) {
            for (View view:views) {
                ((ViewGroup)view.getParent()).removeView(view);
            }
        }
    }

    /**
     * ????????
     */
    private void initEffects() {
        mGrayEffect = new GrayEffect(context);
        mNullEffect = new NullEffect(context);
    }

    /**
     * ???????????
     */
    private void initLiveView() {
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

//        //??????
        if(recorderBean.getWaterMakerImg() != null) {
            Watermark watermark = new Watermark(recorderBean.getWaterMakerImg(), 50, 25, WatermarkPosition.WATERMARK_ORIENTATION_BOTTOM_RIGHT, 8, 8);
            cameraLivingView.setWatermark(watermark);
        }

        //???????????
        cameraLivingView.setCameraOpenListener(new CameraListener() {
            @Override
            public void onOpenSuccess() {
//                //?????View?????????????camera??????
                if(cameraLivingView!= null) {
                    cameraLivingView.setBackgroundColor(Color.TRANSPARENT);
                }

                //?????????
                if(isFlight) {
                    switchLight(true);
                }else {
                    switchLight(false);
                }

                //???????
                setEffect(effectType);

                if(CameraRecordOpt.getInstance().getCameraCallBack() != null) {
                    CameraRecordOpt.getInstance().getCameraCallBack().onCameraOpenSuccess();
                }
            }

            @Override
            public void onOpenFail(int error) {
                if(CameraRecordOpt.getInstance().getCameraCallBack() != null) {
                    CameraRecordOpt.getInstance().getCameraCallBack().onCameraOpenError();
                }
            }

            @Override
            public void onCameraChange() {
                if(CameraRecordOpt.getInstance().getCameraCallBack() != null) {
                    CameraRecordOpt.getInstance().getCameraCallBack().onSwitchCamera();
                }
            }
        });

        //???????????
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
        return this.isMic;
    }

    /**
     * ?????
     * @param isMic
     */
    public void setMic(boolean isMic) {
        this.isMic = isMic;
        if(audioClient != null) {
            audioClient.setMic(isMic);
        }
    }

    /**
     * ?????????
     */
    public void switchLight(boolean isFlight) {
        this.isFlight = isFlight;
        cameraLivingView.switchTorch(isFlight);
    }

    /**
     * ?л??????
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
     * ?л????
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
     * ?л???????
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
                // Fling left;
            } else if (e2.getX() - e1.getX() > 100
                    && Math.abs(velocityX) > 200) {
                // Fling right
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    /**
     * ???
     */
    private void pause() {
        if(streamingSender != null) {
            streamingSender.pause();
        }
    }

    /**
     * ????
     */
    private void resume() {
        if(streamingSender != null) {
            streamingSender.resume();
        }
    }

    public CameraLivingView getCameraLivingView() {
        return cameraLivingView;
    }

    /****************????????********************/
    public void onStart() {
        resume();
    }

    public void onStop() {
        pause();
    }
}
