package net.yrom.demo.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import net.yrom.demo.R;
import net.yrom.screenrecorder.operate.CameraRecordOpt;
import net.yrom.screenrecorder.operate.ICameraCallBack;
import net.yrom.screenrecorder.operate.RecorderBean;
import net.yrom.screenrecorder.ui.*;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LaunchActivity extends AppCompatActivity {

    @BindView(R.id.btn_screen_record)
    Button btnScreenRecord;
    @BindView(R.id.btn_camera_record)
    Button btnCameraRecord;

    EditText mAddress;

    private static final int REQUEST_STREAM = 1;
    private static String[] PERMISSIONS_STREAM = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    boolean authorized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        mAddress = (EditText) findViewById(R.id.address);
//        mAddress.setText("rtmp://live-api-a.facebook.com:80/rtmp/480153555718088?ds=1&a=ATgU_gVweXkoCDT1");
        ButterKnife.bind(this);
        verifyPermissions();
    }

    @OnClick({R.id.btn_screen_record, R.id.btn_camera_record})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_screen_record:
                ScreenRecordActivity.launchActivity(this);
                break;
            case R.id.btn_camera_record:
//                CameraActivity.launchActivity(this);

                RecorderBean recorderBean = new RecorderBean();
//                recorderBean.setRtmpAddr("rtmp://live-api-a.facebook.com:80/rtmp/2030426157195059?ds=1&a=ATiiDcUL-ry7317K");
                recorderBean.setRtmpAddr(mAddress.getText().toString());
                recorderBean.setWidth(1080);
                recorderBean.setHeight(1920);
                CameraRecordOpt.getInstance().setCameraCallBack(new ICameraCallBack() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(LaunchActivity.this,"success",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError() {
                        Toast.makeText(LaunchActivity.this,"error",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSwitchCamera() {
                        Toast.makeText(LaunchActivity.this,"onSwitchCamera",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLiveStart() {
                        Toast.makeText(LaunchActivity.this,"onLiveStart",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLiveStop() {
                        Toast.makeText(LaunchActivity.this,"onLiveStop",Toast.LENGTH_SHORT).show();
                    }
                });
                CameraRecordOpt.getInstance().startCameraRecordWithActivity(this,recorderBean,CameraRecordActivity.class);
//                CameraRecordOpt.getInstance().startCameraRecordNoActivity(this,recorderBean,null);
                break;
        }
    }

    public void verifyPermissions() {
        int CAMERA_permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int RECORD_AUDIO_permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int WRITE_EXTERNAL_STORAGE_permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (CAMERA_permission != PackageManager.PERMISSION_GRANTED ||
                RECORD_AUDIO_permission != PackageManager.PERMISSION_GRANTED ||
                WRITE_EXTERNAL_STORAGE_permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STREAM,
                    REQUEST_STREAM
            );
            authorized = false;
        } else {
            authorized = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STREAM) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                authorized = true;
            }
        }
    }
}
