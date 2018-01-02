package org.ros.android.android_ros_padbot;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.ros.android.RosActivity;
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
    static public Boolean stopBtnClicked = false;

    private TextView name_tv;
    private TextView connection_tv;

    private TextView obstacle_tv;
    private TextView battery_tv;

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

        name_tv = (TextView) findViewById(R.id.robot_name_value_tv);
        connection_tv = (TextView) findViewById(R.id.connection_value_tv);

        obstacle_tv = (TextView) findViewById(R.id.obstacle_value_tv);
        battery_tv = (TextView) findViewById(R.id.battery_value_tv);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        node = new PadbotNode();
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        nodeConfiguration.setMasterUri(getMasterUri());

        // Run the node
        nodeMainExecutor.execute(node,nodeConfiguration);
    }

    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.connect_robot_btn:
                connection_tv.setText("Connecting...");
                RobotManager.getInstance(getApplication()).connectRobotBySerialPort();
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
