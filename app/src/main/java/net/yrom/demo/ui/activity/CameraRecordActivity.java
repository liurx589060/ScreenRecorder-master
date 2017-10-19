package net.yrom.demo.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import net.yrom.demo.R;
import net.yrom.screenrecorder.operate.CameraRecordOpt;
import net.yrom.screenrecorder.ui.CameraUIHelper;

public class CameraRecordActivity extends Activity {
    private Button mMicBtn;
    private Button mFlashBtn;
    private Button mFaceBtn;
    private Button mBeautyBtn;
    private Button mFocusBtn;
    private Button mRecordBtn;

    private CameraUIHelper mCameraUIHelper;

    private class ButtonState {
        public boolean isMicPress;
        public boolean isFlashPress;
        public boolean isFacePress;
        public boolean isFocusPress;
        public boolean isBeautyPress;
    }

    private ButtonState mButtonState = new ButtonState();

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
        mCameraUIHelper.setContentView(R.layout.activity_camera_record);

        initViews();
        initListeners();
    }

    private void initViews() {
        mMicBtn = (Button) findViewById(R.id.record_mic_button);
        mFlashBtn = (Button) findViewById(R.id.camera_flash_button);
        mFaceBtn = (Button) findViewById(R.id.camera_switch_button);
        mBeautyBtn = (Button) findViewById(R.id.camera_render_button);
        mFocusBtn = (Button) findViewById(R.id.camera_focus_button);
        mRecordBtn = (Button) findViewById(R.id.btnRecord);

        setButtonState();
    }

    private void setButtonState() {
        mMicBtn.setTag(CameraRecordOpt.getInstance().getRecorderBean().isMic());
        mFlashBtn.setTag(CameraRecordOpt.getInstance().getRecorderBean().isFlight());
        mFaceBtn.setTag(CameraRecordOpt.getInstance().getRecorderBean().getCameraType()==CameraUIHelper.CAMERA_FRONT?false:true);
        mBeautyBtn.setTag(CameraRecordOpt.getInstance().getRecorderBean().getEffectType()==CameraUIHelper.EFFECT_GRAY?false:true);
        mFocusBtn.setTag(CameraRecordOpt.getInstance().getRecorderBean().getFocusType()==CameraUIHelper.FOCUS_TOUCH?false:true);

        if((boolean)mMicBtn.getTag()) {
            mMicBtn.setBackgroundResource(R.drawable.ic_mic_on_normal);
        }else {
            mMicBtn.setBackgroundResource(R.drawable.ic_mic_off_normal);
        }

        if((boolean)mFlashBtn.getTag()) {
            mFlashBtn.setBackgroundResource(R.drawable.ic_flash_on_normal);
        }else {
            mFlashBtn.setBackgroundResource(R.drawable.ic_flash_off_normal);
        }

        if((boolean)mFaceBtn.getTag()) {
            mFaceBtn.setBackgroundResource(R.drawable.ic_switch_camera_back_normal);
        }else {
            mFaceBtn.setBackgroundResource(R.drawable.ic_switch_camera_front_normal);
        }

        if((boolean)mBeautyBtn.getTag()) {
            mBeautyBtn.setBackgroundResource(R.drawable.ic_render_on_normal);
        }else {
            mBeautyBtn.setBackgroundResource(R.drawable.ic_render_off_normal);
        }

        if((boolean)mFocusBtn.getTag()) {
            mFocusBtn.setBackgroundResource(R.drawable.ic_focus_on_normal);
        }else {
            mFocusBtn.setBackgroundResource(R.drawable.ic_focus_off_normal);
        }
    }

    private void initListeners() {
        mMicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMicBtn.setTag(!(boolean)mMicBtn.getTag());
                if((boolean)mMicBtn.getTag()) {
                    mMicBtn.setBackgroundResource(R.drawable.ic_mic_on_normal);
                }else {
                    mMicBtn.setBackgroundResource(R.drawable.ic_mic_off_normal);
                }

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
                if((boolean)mFlashBtn.getTag()) {
                    mFlashBtn.setBackgroundResource(R.drawable.ic_flash_on_normal);
                }else {
                    mFlashBtn.setBackgroundResource(R.drawable.ic_flash_off_normal);
                }
            }
        });

        mFaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraUIHelper.switchCamera();

                mFaceBtn.setTag(!(boolean)mFaceBtn.getTag());
                if((boolean)mFaceBtn.getTag()) {
                    mFaceBtn.setBackgroundResource(R.drawable.ic_switch_camera_back_normal);
                }else {
                    mFaceBtn.setBackgroundResource(R.drawable.ic_switch_camera_front_normal);
                }
            }
        });

        mBeautyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBeautyBtn.setTag(!(boolean)mBeautyBtn.getTag());
                if((boolean)mBeautyBtn.getTag()) {
                    mBeautyBtn.setBackgroundResource(R.drawable.ic_render_on_normal);
                }else {
                    mBeautyBtn.setBackgroundResource(R.drawable.ic_render_off_normal);
                }

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
                if((boolean)mFocusBtn.getTag()) {
                    mFocusBtn.setBackgroundResource(R.drawable.ic_focus_on_normal);
                }else {
                    mFocusBtn.setBackgroundResource(R.drawable.ic_focus_off_normal);
                }
            }
        });

        mRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCameraUIHelper.isRecording()) {
                    mRecordBtn.setBackgroundResource(R.drawable.ic_record_start);
                    mCameraUIHelper.stopRecord();
                } else {
                    mRecordBtn.setBackgroundResource(R.drawable.ic_record_stop);
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
