package org.ros.android.android_ros_padbot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;

import org.apache.commons.io.IOUtils;
import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import cn.inbot.padbotsdk.Robot;
import cn.inbot.padbotsdk.RobotManager;
import cn.inbot.padbotsdk.constant.RobotDisconnectType;
import cn.inbot.padbotsdk.listener.RobotConnectionListener;
import cn.inbot.padbotsdk.listener.RobotListener;
import cn.inbot.padbotsdk.model.ObstacleDistanceData;

public class MainActivity extends RosActivity {

  private static final String TAG = MainActivity.class.getSimpleName();
  private boolean installRequested;

  // ARCore
  static public ArSceneView arSceneView;
  private static final int RC_PERMISSIONS = 0x123;
  static public Pose cameraPose;
  static public Frame frame;

  // ROS messages
  static public int batteryData = 100;
  static public String obstacleData = "";

  // ROS parameter
  static public String urdf = "";

  public MainActivity() {
    super("PadBot", "PadBot");
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

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
  }

  @Override
  protected void init(NodeMainExecutor nodeMainExecutor) {

    NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
    nodeConfiguration.setMasterUri(getMasterUri());

    // Run the node
    // PadbotNode padbot_node = new PadbotNode();
    // nodeMainExecutor.execute(padbot_node,nodeConfiguration);

    // ARCore node
    ARCore ar_core = new ARCore();
    nodeMainExecutor.execute(ar_core, nodeConfiguration);
  }
}