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
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.yrom.screenrecorder.operate.RecorderBean;
import net.yrom.screenrecorder.operate.ScreenRecordOpt;
import net.yrom.demo.R;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScreenRecordActivity extends Activity implements View.OnClickListener {
    private static final int REQUEST_CODE = 1;
    private Button mButton;
    private EditText mRtmpAddET;
    private MediaProjectionManager mMediaProjectionManager;
    private String rtmpAddr;

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
        mRtmpAddET = (EditText) findViewById(R.id.et_rtmp_address);
        mButton.setOnClickListener(this);
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mRtmpAddET.setText("rtmp://live-api-a.facebook.com:80/rtmp/2025734320997576?ds=1&s_l=1&a=ATi9YUSsl-oPjFwr");

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
        bean.setWidth(1280);
        bean.setHeight(720);

        ScreenRecordOpt.getInstance().startScreenRecord(bean,mediaProjection);

        mButton.setText("Stop Recorder");
        Toast.makeText(this, "Screen recorder is running...", Toast.LENGTH_SHORT).show();
//        moveTaskToBack(true);
    }


    @Override
    public void onClick(View v) {
        if (ScreenRecordOpt.getInstance().isRecording()) {
            stopScreenRecord();
        } else {
            createScreenCapture();
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

    private void createScreenCapture() {
        Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, 1000);
    }

    private void stopScreenRecord() {
        ScreenRecordOpt.getInstance().stopScreenRecord();
        mButton.setText("Restart recorder");
    }
}
