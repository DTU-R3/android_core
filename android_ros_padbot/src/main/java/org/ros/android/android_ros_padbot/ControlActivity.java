package org.ros.android.android_ros_padbot;

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

public class ControlActivity extends RosActivity implements RobotConnectionListener,RobotListener {

    private static final String TAG = ControlActivity.class.getSimpleName();
    private boolean installRequested;

    static public Robot robot;
    private String serialNumber;
    private int model;
    static public Boolean stopBtnClicked = false;

    private TextView name_tv;
    private TextView connection_tv;

    private TextView obstacle_tv;
    private TextView battery_tv;

    // ARCore
    private ArSceneView arSceneView;
    private static final int RC_PERMISSIONS = 0x123;
    static public Pose cameraPose;
    static public Frame frame;

    // ROS messages
    static public int batteryData = 100;
    static public String obstacleData = "";

    // ROS parameter
    static public String urdf = "";

    public ControlActivity() {
        super("PadBot", "PadBot");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        setTitle("Control Robot");

        RobotManager.getInstance(getApplication()).setRobotConnectionListener(this);
        RobotManager.getInstance(getApplication()).openSoundSourceAngleListener();

        Intent intent = getIntent();
        model = intent.getIntExtra("model", 0);
        serialNumber = intent.getStringExtra("serialNumber");

        name_tv = (TextView) findViewById(R.id.robot_name_value_tv);
        connection_tv = (TextView) findViewById(R.id.connection_value_tv);

        obstacle_tv = (TextView) findViewById(R.id.obstacle_value_tv);
        battery_tv = (TextView) findViewById(R.id.battery_value_tv);

        InputStream is = getResources().openRawResource(R.raw.padbot_t1);
        try {
            urdf = IOUtils.toString(is, String.valueOf(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        // Set an update listener on the Scene that will hide the loading message once a Plane is
        arSceneView
                .getScene()
                .setOnUpdateListener(
                        frameTime -> {

                            frame = arSceneView.getArFrame();
                            if (frame == null) {
                                return;
                            }
                            cameraPose = frame.getCamera().getPose();
                        });

        // Lastly request CAMERA permission which is required by ARCore.
        DemoUtils.requestCameraPermission(this, RC_PERMISSIONS);
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

    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.connect_robot_btn:
                connection_tv.setText("Connecting...");
                if (1 == model) {
                    RobotManager.getInstance(getApplication()).connectRobotByBluetooth(serialNumber);
                }
                else if (2 == model) {
                    RobotManager.getInstance(getApplication()).connectRobotBySerialPort();
                }
                break;

            case R.id.disconnect_robot_btn:

                connection_tv.setText("Disconnecting...");

                RobotManager.getInstance(getApplication()).disconnectRobot();

                break;

            case R.id.stop_robot_btn:
                stopBtnClicked = true;
                break;

            default:
                break;
        }
    }

    @Override
    public void onRobotConnected(Robot robot) {
        this.robot = robot;
        this.robot.setListener(this);

        name_tv.setText(robot.getRobotSerialNumber());
        connection_tv.setText("Connected");

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
        connection_tv.setText("Connect failed");
    }

    @Override
    public void onRobotDisconnected(String s, RobotDisconnectType robotDisconnectType) {
        this.robot = null;
        connection_tv.setText("Disconnected");
    }

    @Override
    public void onReceivedRobotObstacleDistanceData(ObstacleDistanceData obstacleDistanceData) {
        obstacleData = "" + obstacleDistanceData.getFirstDistance() + ","
                + obstacleDistanceData.getSecondDistance() + ","
                + obstacleDistanceData.getThirdDistance() + ","
                + obstacleDistanceData.getFourthDistance() + ","
                + obstacleDistanceData.getFifthDistance();
        obstacle_tv.setText(obstacleData);
    }

    @Override
    public void onReceivedRobotBatteryPercentage(int i) {
        batteryData = i;
        battery_tv.setText("" + batteryData);
    }

    @Override
    public void onReceivedRobotHardwareVersion(int i) {

    }

    @Override
    public void onReceivedRobotSerialNumber(String s) {
        name_tv.setText(s);
    }

    @Override
    public void onReceivedCustomData(String s) {

    }

    @Override
    public void onReceivedSoundSourceAngle(int i) {

    }
}
