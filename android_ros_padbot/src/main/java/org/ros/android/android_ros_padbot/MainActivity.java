package org.ros.android.android_ros_padbot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

  }

  public void onClick(View view) {

    if (view.getId() == R.id.ut_mode_bt) {
      Intent intent = new Intent();
      intent.setClass(MainActivity.this, ScanActivity.class);
      intent.putExtra("model", 1);
      startActivity(intent);
    }
    else if (view.getId() == R.id.px_mode_bt) {
      Intent intent = new Intent();
      intent.setClass(MainActivity.this, ControlActivity.class);
      intent.putExtra("model", 2);
      startActivity(intent);
    }
  }
}
