package org.ros.android.android_ros_padbot;

import android.Manifest;
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
import java.util.Timer;
import java.util.TimerTask;

import cn.inbot.padbotsdk.Robot;
import cn.inbot.padbotsdk.RobotManager;
import cn.inbot.padbotsdk.constant.RobotDisconnectType;
import cn.inbot.padbotsdk.listener.RobotConnectionListener;
import cn.inbot.padbotsdk.listener.RobotListener;
import cn.inbot.padbotsdk.listener.RobotScanListener;
import cn.inbot.padbotsdk.model.ObstacleDistanceData;
import rx.functions.Action1;

public class MainActivity extends RosActivity implements RobotScanListener, RobotConnectionListener,RobotListener {

  private static final String TAG = MainActivity.class.getSimpleName();

  // ARCore
  static public ArSceneView arSceneView;
  private static final int RC_PERMISSIONS = 0x123;
  private boolean installRequested;

  // Padbot
  private List<String> robotList = new ArrayList<String>();
  private boolean robot_state = false;
  private int robotIndex = 0;
  private java.lang.String serialNumber = "";
  static public Robot robot;
  static public int battery_data = 0;
  static public String obstacle_data = "";

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

    RobotManager.getInstance(getApplication()).setRobotConnectionListener(this);
    RobotManager.getInstance(getApplication()).openSoundSourceAngleListener();

    TryConnectRobot();
    
    // Timer to check robot state
    Timer timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        // Connect the robot
        if ((!robot_state)&&(PadbotNode.nodeState)) {
          robot_state = true;
          TryConnectRobot();
        }
        else if ((robot_state)&&(!PadbotNode.nodeState)) {
          robot_state = false;
          RobotManager.getInstance(getApplication()).disconnectRobot();
          Log.d(TAG, "Disconnect robot");
        }
      }
    }, 0,1);
  }

  private void TryConnectRobot() {
    if (robotIndex < robotList.size()) {
      serialNumber = robotList.get(robotIndex);
      RobotManager.getInstance(getApplication()).connectRobotByBluetooth(serialNumber);
      Log.d(TAG, "Connecting to " + serialNumber);
    }
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

  @Override
  public void onRobotConnected(Robot r) {
    Log.d(TAG, "Connected to " + serialNumber);
    this.robot = r;
    this.robot.setListener(this);
    robot_state = true;
    if (robot != null)
    {
      robot.setMovementSpeed(1);
      robot.queryBatteryPercentage();
      robot.queryObstacleDistanceData();
    }
  }

  @Override
  public void onRobotConnectFailed(String s) {
    this.robot = null;
    robotIndex += 1;
    if (robotIndex >= robotList.size()) {
      robotIndex = 0;
    }
    Log.d(TAG, "Connecting failed, keep trying");
    TryConnectRobot();
  }

  @Override
  public void onRobotDisconnected(String s, RobotDisconnectType robotDisconnectType) {
    Log.d(TAG, "Robot disconnected");
    this.robot = null;
    robotIndex = 0;
    robot_state = false;
  }

  @Override
  public void onReceivedRobotObstacleDistanceData(ObstacleDistanceData d) {
    obstacle_data = d.getFirstDistance() + "," + d.getSecondDistance() + "," + d.getThirdDistance() + "," + d.getFourthDistance() + "," + d.getFifthDistance();
  }

  @Override
  public void onReceivedRobotBatteryPercentage(int i) {
    battery_data = i;
  }

  @Override
  public void onReceivedRobotHardwareVersion(int i) {

  }

  @Override
  public void onReceivedRobotSerialNumber(String s) {

  }

  @Override
  public void onReceivedCustomData(String s) {

  }

  @Override
  public void onReceivedSoundSourceAngle(int i) {

  }
}