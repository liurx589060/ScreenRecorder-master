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
package net.yrom.demo.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.yrom.screenrecorder.operate.RecorderBean;
import net.yrom.screenrecorder.operate.ScreenRecordOpt;
import net.yrom.demo.R;
import net.yrom.screenrecorder.task.RtmpStreamingSender;

import java.util.Arrays;

public class ScreenRecordActivity extends Activity implements View.OnClickListener {
    private static final int REQUEST_CODE = 1;
    private Button mButton;
    private Button mPauseBtn;
    private Button mResumeBtn;
    private Button mMuteAudio;
    private EditText mRtmpAddET;
    private MediaProjectionManager mMediaProjectionManager;
    private String rtmpAddr;

    private WindowManager windowManager;
    private View floatView;
    private TextView floatTextView;
    private Handler mHandler;
    private long time;
    private Runnable runnable;

    public static void launchActivity(Context ctx) {
        Intent it = new Intent(ctx, ScreenRecordActivity.class);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(it);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton = (Button) findViewById(R.id.button);
        mPauseBtn = (Button) findViewById(R.id.pause);
        mResumeBtn = (Button) findViewById(R.id.resume);
        mMuteAudio = (Button) findViewById(R.id.mute);
        mRtmpAddET = (EditText) findViewById(R.id.et_rtmp_address);
        mButton.setOnClickListener(this);
        mPauseBtn.setOnClickListener(this);
        mResumeBtn.setOnClickListener(this);
        mMuteAudio.setOnClickListener(this);
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mRtmpAddET.setText("rtmp://10.10.15.19/live/stream");

        String str = "10,20,30,60";
        String[] strArray = str.split(",");
        int length = strArray.length;
        int[] levelArray = new int[length + 1];
        for (int i= 0 ; i < length ; i++) {
            levelArray[i] = Integer.valueOf(strArray[i]);
        }
        int level = 5;
        levelArray[length] = level;
        Arrays.sort(levelArray);

        int index = 0;
        for (int i = 0;i<levelArray.length;i++) {
            if(level == levelArray[i]) {
                index = i>0?i -1:i;
            }
        }

        mHandler = new Handler();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        floatView = LayoutInflater.from(this).inflate(R.layout.float_layout,null);
        floatTextView = (TextView) floatView.findViewById(R.id.textView);
    }

    private void addFloatView() {
        WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();
        mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;// 系统提示window
//        mParams.format = PixelFormat.TRANSLUCENT;// 支持透明
        mParams.format = PixelFormat.RGBA_8888;
        mParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;// 焦点
        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;//窗口的宽和高
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.gravity = Gravity.RIGHT| Gravity. CENTER_VERTICAL; // 调整悬浮窗口至右侧中间
        mParams.x = 0;
        mParams.y = 0;
        windowManager.addView(floatView,mParams);
    }

    private void removeFloatView() {
        windowManager.removeView(floatView);
        if(runnable != null) {
            mHandler.removeCallbacks(runnable);
        }
        time = 0;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("yy","onActivityResult=" + requestCode + "---->>>" + resultCode);
        MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e("@@", "media projection is null");
            return;
        }
        rtmpAddr = mRtmpAddET.getText().toString().trim();
        if (TextUtils.isEmpty(rtmpAddr)) {
            Toast.makeText(this, "rtmp address cannot be null", Toast.LENGTH_SHORT).show();
            return;
        }
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        RecorderBean bean = new RecorderBean();
        bean.setRtmpAddr(rtmpAddr);
        bean.setBitrate(5000000);
        bean.setWidth(1920);
        bean.setHeight(1080);

        ScreenRecordOpt.getInstance().startScreenRecord(bean, mediaProjection, new RtmpStreamingSender.IRtmpSendCallBack() {
            @Override
            public void sendError() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ScreenRecordActivity.this,"直播发送数据失败",Toast.LENGTH_LONG).show();
                        stopScreenRecord();
                    }
                });
            }

            @Override
            public void connectError() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ScreenRecordActivity.this,"直播连接失败",Toast.LENGTH_LONG).show();
                        stopScreenRecord();
                    }
                });
            }

            @Override
            public void netBad() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ScreenRecordActivity.this,"网络较差",Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onStart(String rtmpAddress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ScreenRecordActivity.this,"开始",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        if(ScreenRecordOpt.getInstance().isMic()) {
            mMuteAudio.setText("静音");
        }else {
            mMuteAudio.setText("打开声音");
        }

        mButton.setText("Stop Recorder");
        Toast.makeText(this, "Screen recorder is running...", Toast.LENGTH_SHORT).show();

        addFloatView();
        runnable = new Runnable() {
            @Override
            public void run() {
                time++;
                floatTextView.setText("第" + time + "秒");
                mHandler.postDelayed(this,100);
            }
        };
        mHandler.post(runnable);
//        moveTaskToBack(true);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                if (ScreenRecordOpt.getInstance().isRecording()) {
                    stopScreenRecord();
                } else {
                    createScreenCapture();
                }
                break;
            case R.id.pause:
                ScreenRecordOpt.getInstance().pause();
                break;

            case R.id.resume:
                ScreenRecordOpt.getInstance().resume();
                break;

            case R.id.mute:
                if(ScreenRecordOpt.getInstance().isMic()) {
                    ScreenRecordOpt.getInstance().setMic(false);
                    mMuteAudio.setText("打开声音");
                }else {
                    ScreenRecordOpt.getInstance().setMic(true);
                    mMuteAudio.setText("静音");
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ScreenRecordOpt.getInstance().isRecording()) {
            stopScreenRecord();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createScreenCapture() {
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, 1000);
    }

    private void stopScreenRecord() {
        ScreenRecordOpt.getInstance().stopRecord();
        removeFloatView();
        mButton.setText("Restart recorder");
    }
}
