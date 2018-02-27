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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.yrom.demo.R;
import net.yrom.screenrecorder.operate.AudioRecordOpt;
import net.yrom.screenrecorder.operate.RecorderBean;
import net.yrom.screenrecorder.task.RtmpStreamingSender;

import java.util.Arrays;

public class AudioRecordActivity extends Activity implements View.OnClickListener {
    private Button mButton;
    private Button mPauseBtn;
    private Button mResumeBtn;
    private Button mMuteAudio;
    private EditText mRtmpAddET;

    public static void launchActivity(Context ctx) {
        Intent it = new Intent(ctx, AudioRecordActivity.class);
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
        mRtmpAddET.setText("rtmp://10.10.15.19/live/stream");
        mButton.setOnClickListener(this);
        mPauseBtn.setOnClickListener(this);
        mResumeBtn.setOnClickListener(this);
        mMuteAudio.setOnClickListener(this);

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
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                if (AudioRecordOpt.getInstance().isRecording()) {
                    stopAudioRecord();
                } else {
                    startAudioRecord();
                }
                break;
            case R.id.pause:
                AudioRecordOpt.getInstance().pause();
                break;

            case R.id.resume:
                AudioRecordOpt.getInstance().resume();
                break;

            case R.id.mute:
                if(AudioRecordOpt.getInstance().isMic()) {
                    AudioRecordOpt.getInstance().setMic(false);
                    mMuteAudio.setText("打开声音");
                }else {
                    AudioRecordOpt.getInstance().setMic(true);
                    mMuteAudio.setText("静音");
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (AudioRecordOpt.getInstance().isRecording()) {
            stopAudioRecord();
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

    private void startAudioRecord() {
        RecorderBean audioBean = new RecorderBean();
        audioBean.setRtmpAddr(mRtmpAddET.getText().toString());
        AudioRecordOpt.getInstance().startAudioRecord(audioBean, new RtmpStreamingSender.IRtmpSendCallBack() {
            @Override
            public void sendError() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AudioRecordActivity.this,"AudioRecordOpt--sendError",Toast.LENGTH_SHORT).show();
                        Log.e("yy","AudioRecordOpt--sendError");
                    }
                });
            }

            @Override
            public void connectError() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AudioRecordActivity.this,"AudioRecordOpt--connectError",Toast.LENGTH_SHORT).show();
                        Log.e("yy","AudioRecordOpt--connectError");
                    }
                });
            }

            @Override
            public void netBad() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AudioRecordActivity.this,"AudioRecordOpt--netBad",Toast.LENGTH_SHORT).show();
                        Log.e("yy","AudioRecordOpt--netBad");
                    }
                });
            }

            @Override
            public void onStart(String rtmpAddress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AudioRecordActivity.this,"start",Toast.LENGTH_SHORT).show();
                        Log.e("yy","AudioRecordOpt--start");
                    }
                });
            }
        });
        mButton.setText("Stop Recorder");
    }

    private void stopAudioRecord() {
        AudioRecordOpt.getInstance().stopRecord();
        mButton.setText("Restart recorder");
    }
}
