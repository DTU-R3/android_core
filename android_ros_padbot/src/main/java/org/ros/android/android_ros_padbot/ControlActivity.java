package org.ros.android.android_ros_padbot;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.ros.android.RosActivity;
import org.ros.android.view.camera.RosCameraPreviewView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import cn.inbot.padbotsdk.Robot;
import cn.inbot.padbotsdk.RobotManager;
import cn.inbot.padbotsdk.constant.RobotDisconnectType;
import cn.inbot.padbotsdk.listener.RobotConnectionListener;
import cn.inbot.padbotsdk.listener.RobotListener;
import cn.inbot.padbotsdk.model.ObstacleDistanceData;

public class ControlActivity extends RosActivity implements RobotConnectionListener,RobotListener {

    static public Robot robot;
    private String serialNumber;
    private int model;
    static public Boolean stopBtnClicked = false;

    private TextView name_tv;
    private TextView connection_tv;

    private TextView obstacle_tv;
    private TextView battery_tv;

    // Camera
    static public int cameraId;
    static public RosCameraPreviewView rosCameraPreviewView;

    // ROS messages
    public PadbotNode node;
    static public int batteryData = 100;
    static public String obstacleData = "";

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

        rosCameraPreviewView = (RosCameraPreviewView) findViewById(R.id.ros_camera_preview_view);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        node = new PadbotNode();

        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        nodeConfiguration.setMasterUri(getMasterUri());

        // Run the node
        nodeMainExecutor.execute(node,nodeConfiguration);

        cameraId = 0;
        rosCameraPreviewView.setCamera(getCamera());
        nodeMainExecutor.execute(rosCameraPreviewView, nodeConfiguration);

        // Run the node
        VirtualEncoder virtual_encoder = new VirtualEncoder();
        nodeMainExecutor.execute(virtual_encoder, nodeConfiguration);
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

    static public Camera getCamera() {
        Camera cam = Camera.open(cameraId);
        Camera.Parameters camParams = cam.getParameters();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (camParams.getSupportedFocusModes().contains(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                camParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else {
                camParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
        }
        cam.setParameters(camParams);
        return cam;
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
