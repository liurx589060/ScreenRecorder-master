package net.yrom.screenrecorder.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import net.yrom.screenrecorder.operate.CameraRecordOpt;
import net.yrom.screenrecorder.tools.ResourceUtil;

public class CameraRecordActivity extends Activity {
    private Button mMicBtn;
    private Button mFlashBtn;
    private Button mFaceBtn;
    private Button mBeautyBtn;
    private Button mFocusBtn;
    private Button mRecordBtn;

    private CameraUIHelper mCameraUIHelper;

    /**
     * 启动Activity
     * @param context
     */
    public static void LaunchActivity(Context context) {
        Intent intent = new Intent(context, CameraRecordActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCameraUIHelper = new CameraUIHelper(this, CameraRecordOpt.getInstance().getRecorderBean());
        mCameraUIHelper.setContentView(ResourceUtil.getLayoutId(this,"activity_camera_record"));

        initViews();
        initListeners();
    }

    private void initViews() {
        mMicBtn = (Button) findViewById(ResourceUtil.getId(this,"record_mic_button"));
        mFlashBtn = (Button) findViewById(ResourceUtil.getId(this,"camera_flash_button"));
        mFaceBtn = (Button) findViewById(ResourceUtil.getId(this,"camera_switch_button"));
        mBeautyBtn = (Button) findViewById(ResourceUtil.getId(this,"camera_render_button"));
        mFocusBtn = (Button) findViewById(ResourceUtil.getId(this,"camera_focus_button"));
        mRecordBtn = (Button) findViewById(ResourceUtil.getId(this,"btnRecord"));

        setButtonState();
    }

    private void setButtonState() {
        mMicBtn.setTag(CameraRecordOpt.getInstance().getRecorderBean().isMic());
        mFlashBtn.setTag(CameraRecordOpt.getInstance().getRecorderBean().isFlight());
        mFaceBtn.setTag(CameraRecordOpt.getInstance().getRecorderBean().getCameraType()==CameraUIHelper.CAMERA_FRONT?false:true);
        mBeautyBtn.setTag(CameraRecordOpt.getInstance().getRecorderBean().getEffectType()==CameraUIHelper.EFFECT_GRAY?false:true);
        mFocusBtn.setTag(CameraRecordOpt.getInstance().getRecorderBean().getFocusType()==CameraUIHelper.FOCUS_TOUCH?false:true);

        setButtonBg(mMicBtn);
        setButtonBg(mFlashBtn);
        setButtonBg(mFaceBtn);
        setButtonBg(mBeautyBtn);
        setButtonBg(mFocusBtn);
    }

    private void setButtonBg(Button btn) {
        int resId = 0;
        boolean flag = (boolean) btn.getTag();
        if(btn == mMicBtn) {
            resId = flag?ResourceUtil.getDrawableId(this,"ic_mic_on_normal"):ResourceUtil.getDrawableId(this,"ic_mic_off_normal");
        }else if (btn == mFlashBtn) {
            resId = flag?ResourceUtil.getDrawableId(this,"ic_flash_on_normal"):ResourceUtil.getDrawableId(this,"ic_flash_off_normal");
        }else if (btn == mFaceBtn) {
            resId = flag?ResourceUtil.getDrawableId(this,"ic_switch_camera_back_normal"):ResourceUtil.getDrawableId(this,"ic_switch_camera_front_normal");
        }else if (btn == mBeautyBtn) {
            resId = flag?ResourceUtil.getDrawableId(this,"ic_render_on_normal"):ResourceUtil.getDrawableId(this,"ic_render_off_normal");
        }else if (btn == mFocusBtn) {
            resId = flag?ResourceUtil.getDrawableId(this,"ic_focus_on_normal"):ResourceUtil.getDrawableId(this,"ic_focus_off_normal");
        }
        btn.setBackgroundResource(resId);
    }


    private void initListeners() {
        mMicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMicBtn.setTag(!(boolean)mMicBtn.getTag());
                setButtonBg(mMicBtn);

                if(mCameraUIHelper.isMic()) {
                    mCameraUIHelper.setMic(false);
                }else {
                    mCameraUIHelper.setMic(true);
                }
            }
        });

        mFlashBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCameraUIHelper.isFlight()) {
                    mCameraUIHelper.switchLight(false);
                }else {
                    mCameraUIHelper.switchLight(true);
                }
                mFlashBtn.setTag(!(boolean)mFlashBtn.getTag());
                setButtonBg(mFlashBtn);
            }
        });

        mFaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraUIHelper.switchCamera();

                mFaceBtn.setTag(!(boolean)mFaceBtn.getTag());
                setButtonBg(mFaceBtn);
            }
        });

        mBeautyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBeautyBtn.setTag(!(boolean)mBeautyBtn.getTag());
                setButtonBg(mBeautyBtn);

                if(mCameraUIHelper.getEffectType() == CameraUIHelper.EFFECT_GRAY) {
                    mCameraUIHelper.setEffect(CameraUIHelper.EFFECT_NORMAL);
                }else {
                    mCameraUIHelper.setEffect(CameraUIHelper.EFFECT_GRAY);
                }
            }
        });

        mFocusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraUIHelper.switchFocusMode();

                mFocusBtn.setTag(!(boolean)mFocusBtn.getTag());
                setButtonBg(mFocusBtn);
            }
        });

        mRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCameraUIHelper.isRecording()) {
                    mRecordBtn.setBackgroundResource(ResourceUtil.getDrawableId(CameraRecordActivity.this,"ic_record_start"));
                    mCameraUIHelper.stopRecord();
                } else {
                    mRecordBtn.setBackgroundResource(ResourceUtil.getDrawableId(CameraRecordActivity.this,"ic_record_stop"));
                    //开始直播
                    mCameraUIHelper.startRecord();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCameraUIHelper.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCameraUIHelper.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //停止直播
        mCameraUIHelper.onDestroy();
    }
}
