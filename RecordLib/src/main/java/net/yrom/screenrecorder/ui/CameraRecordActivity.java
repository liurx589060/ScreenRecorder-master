package net.yrom.screenrecorder.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import net.yrom.screenrecorder.R;
import net.yrom.screenrecorder.operate.CameraRecordOpt;
import net.yrom.screenrecorder.operate.RecorderBean;

public class CameraRecordActivity extends Activity {
    private MultiToggleImageButton mMicBtn;
    private MultiToggleImageButton mFlashBtn;
    private MultiToggleImageButton mFaceBtn;
    private MultiToggleImageButton mBeautyBtn;
    private MultiToggleImageButton mFocusBtn;
    private ImageButton mRecordBtn;

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
        mCameraUIHelper.setContentView(R.layout.activity_camera_record);

        initViews();
        initListeners();
    }

    private void initViews() {
        mMicBtn = (MultiToggleImageButton) findViewById(R.id.record_mic_button);
        mFlashBtn = (MultiToggleImageButton) findViewById(R.id.camera_flash_button);
        mFaceBtn = (MultiToggleImageButton) findViewById(R.id.camera_switch_button);
        mBeautyBtn = (MultiToggleImageButton) findViewById(R.id.camera_render_button);
        mFocusBtn = (MultiToggleImageButton) findViewById(R.id.camera_focus_button);
        mRecordBtn = (ImageButton) findViewById(R.id.btnRecord);
    }

    private void initListeners() {
        mMicBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                if(mCameraUIHelper.isMic()) {
                    mCameraUIHelper.setMic(false);
                }else {
                    mCameraUIHelper.setMic(true);
                }
            }
        });
        mFlashBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                if(state == 0) {
                    mCameraUIHelper.switchLight(false);
                }else {
                    mCameraUIHelper.switchLight(true);
                }
            }
        });
        mFaceBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                mCameraUIHelper.switchCamera();
            }
        });
        mBeautyBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                if(state == 0) {
                    mCameraUIHelper.setEffect(CameraUIHelper.EFFECT_NORMAL);
                }else {
                    mCameraUIHelper.setEffect(CameraUIHelper.EFFECT_GRAY);
                }
            }
        });
        mFocusBtn.setOnStateChangeListener(new MultiToggleImageButton.OnStateChangeListener() {
            @Override
            public void stateChanged(View view, int state) {
                mCameraUIHelper.switchFocusMode();
            }
        });
        mRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCameraUIHelper.isRecording()) {
                    Toast.makeText(CameraRecordActivity.this, "stop living", Toast.LENGTH_SHORT).show();
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
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //停止直播
        mCameraUIHelper.release();
    }
}
