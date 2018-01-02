package org.ros.android.android_ros_padbot;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.tbruyelle.rxpermissions.RxPermissions;

public class ScanActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
    }

    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.scan_robot_btn:
                RxPermissions rxPermissions = new RxPermissions(ScanActivity.this);
                break;
            default:
                break;
        }
    }
}
