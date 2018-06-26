package org.ros.android.android_ros_padbot;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.tbruyelle.rxpermissions.RxPermissions;

import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.util.ArrayList;
import java.util.List;

import cn.inbot.padbotsdk.RobotManager;
import cn.inbot.padbotsdk.listener.RobotScanListener;
import rx.functions.Action1;

public class MainActivity extends RosActivity implements RobotScanListener {

  private static final String TAG = MainActivity.class.getSimpleName();
  private boolean installRequested;
  static public Application mainApp;

  // ARCore
  static public ArSceneView arSceneView;
  private static final int RC_PERMISSIONS = 0x123;

  // Padbot
  static public List<String> robotList = new ArrayList<String>();


  public MainActivity() {
    super("PadBot", "PadBot");
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    // ARCore
    arSceneView = findViewById(R.id.ar_scene_view);

    if (arSceneView.getSession() == null) {
      // If the session wasn't created yet, don't resume rendering.
      // This can happen if ARCore needs to be updated or permissions are not granted yet.
      try {
        Session session = DemoUtils.createArSession(this, installRequested);
        if (session == null) {
          installRequested = DemoUtils.hasCameraPermission(this);
        } else {
          arSceneView.setupSession(session);
        }
      } catch (UnavailableException e) {
        DemoUtils.handleSessionException(this, e);
      }
    }

    try {
      arSceneView.resume();
    } catch (CameraNotAvailableException ex) {
      DemoUtils.displayError(this, "Unable to get camera", ex);
      finish();
    }

    // Lastly request CAMERA permission which is required by ARCore.
    DemoUtils.requestCameraPermission(this, RC_PERMISSIONS);

    // Padbot
    // Search and connect the robot
    RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
    rxPermissions
            .request(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN
                    , Manifest.permission.ACCESS_COARSE_LOCATION)
            .subscribe(new Action1<Boolean>() {
              @Override
              public void call(Boolean aBoolean) {
                if (aBoolean) {
                  // Set the scan listener
                  RobotManager.getInstance(getApplication()).setRobotScanListener(MainActivity.this);
                  // The argument is scan time, the unit is second
                  RobotManager.getInstance(getApplication()).scanForRobots(8);
                } else {

                }
              }
            });
  }

  @Override
  protected void init(NodeMainExecutor nodeMainExecutor) {
    NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
    nodeConfiguration.setMasterUri(getMasterUri());

    // Run the node
    PadbotNode padbot_node = new PadbotNode();
    nodeMainExecutor.execute(padbot_node,nodeConfiguration);

    // ARCore node
    ARCore ar_core = new ARCore();
    nodeMainExecutor.execute(ar_core, nodeConfiguration);
  }

  @Override
  public void onRobotDiscovered(String s, int i) {
    boolean isExist = false;
    if (null != robotList && !robotList.isEmpty()) {
      for (String serialNumber : robotList) {
        if (s.equals(serialNumber)) {
          isExist = true;
          break;
        }
      }
    }

    if (!isExist) {
      robotList.add(s);
    }
  }

  @Override
  public void onRobotScanCompleted() {

  }
}