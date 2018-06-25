package org.ros.android.android_ros_padbot;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.util.Log;

import com.tbruyelle.rxpermissions.RxPermissions;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import cn.inbot.padbotsdk.Robot;
import cn.inbot.padbotsdk.RobotManager;
import cn.inbot.padbotsdk.constant.RobotDisconnectType;
import cn.inbot.padbotsdk.listener.RobotConnectionListener;
import cn.inbot.padbotsdk.listener.RobotListener;
import cn.inbot.padbotsdk.model.ObstacleDistanceData;
import geometry_msgs.Twist;
import geometry_msgs.Vector3;
import rx.functions.Action1;
import std_msgs.Bool;
import std_msgs.Int8;
import std_msgs.String;

public class PadbotNode extends AbstractNodeMain implements RobotConnectionListener,RobotListener {

    // Node variables
    private static final java.lang.String TAG = PadbotNode.class.getSimpleName();
    private java.lang.String nodeName;
    private Boolean nodeState = Boolean.TRUE;

    // Padbot variables
    private int robotIndex = 0;
    private java.lang.String serialNumber = "";
    private Robot robot;
    private int battery = 0;
    private java.lang.String obstacle = "";

    private double linearSpeed = 0.0;
    private double angularSpeed = 0.0;
    private int vel_cmd = 0;
    private double[] vel_linear = {0, 0.0779, 0.0763, 0.0762, 0.0753, 0.0752, 0};
    private double[] vel_angular = {0, 0, 0.0312, 0.0585, 0.0896, 0.1212, 1.8};

    public PadbotNode() {
        this.nodeName = "Padbot";
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(nodeName);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {

        // Publishers and subscribers
        final Publisher<std_msgs.Int8> batteryPub = connectedNode.newPublisher("padbot/battery_percentage", Int8._TYPE);
        final Publisher<std_msgs.String> obstaclePub = connectedNode.newPublisher("padbot/obstacle", String._TYPE);
        final Subscriber<geometry_msgs.Twist> velSub = connectedNode.newSubscriber("cmd_vel", Twist._TYPE);
        final Subscriber<Bool> stateSub = connectedNode.newSubscriber("padbot/state",Bool._TYPE);
        final Subscriber<String> cmdSub = connectedNode.newSubscriber("padbot/cmd",String._TYPE);

        TryConnectRobot();

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void loop() throws InterruptedException {
                // Publish battery and obstacle data
                std_msgs.Int8 batteryData = batteryPub.newMessage();
                std_msgs.String obstacleData = obstaclePub.newMessage();
                batteryData.setData((byte) battery);
                obstacleData.setData(obstacle);
                batteryPub.publish(batteryData);
                obstaclePub.publish(obstacleData);
            }
        });

        stateSub.addMessageListener(new MessageListener<Bool>() {
            @Override
            public void onNewMessage(Bool bool) {
                nodeState = bool.getData();
                if (nodeState) {
                    TryConnectRobot();
                }
                else {
                    RobotManager.getInstance(MainActivity.mainApp).disconnectRobot();
                    Log.d(TAG, "Disconnect robot");
                }
            }
        });

        cmdSub.addMessageListener(new MessageListener<String>() {
            @Override
            public void onNewMessage(String string) {
                if (robot != null) {
                    switch(string.getData()) {
                        case "up":
                            robot.headRise();
                            break;
                        case "down":
                            robot.headDown();
                            break;
                        case "stop":
                            robot.stop();
                            break;
                        default:
                            break;
                    }
                }
            }
        });

        velSub.addMessageListener(new MessageListener<Twist>() {
            @Override
            public void onNewMessage(Twist twist) {
                linearSpeed = twist.getLinear().getX();
                angularSpeed = twist.getAngular().getZ();
                double v = Math.abs(linearSpeed);
                double w = Math.abs(angularSpeed);
                vel_cmd = 0;
                double dist = 255.0;
                for (int i=0; i < vel_linear.length; i++ ) {
                    if( Math.sqrt((v-vel_linear[i])*(v-vel_linear[i])+(w-vel_angular[i])*(w-vel_angular[i])) < dist ) {
                        dist = Math.sqrt((v-vel_linear[i])*(v-vel_linear[i])+(w-vel_angular[i])*(w-vel_angular[i]));
                        vel_cmd = i;
                    }
                }

                if (robot != null) {
                    switch (vel_cmd){
                        case 1:
                            if(linearSpeed > 0)
                                robot.goForward();
                            else
                                robot.goBackward();
                            break;
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                            if((linearSpeed > 0)&&(angularSpeed > 0))
                                robot.goForwardLeft(vel_cmd-1);
                            else if((linearSpeed > 0)&&(angularSpeed < 0))
                                robot.goForwardRight(vel_cmd-1);
                            else if((linearSpeed < 0)&&(angularSpeed > 0))
                                robot.goBackwardRight(vel_cmd-1);
                            else
                                robot.goBackwardLeft(vel_cmd-1);
                            break;
                        case 6:
                            if(angularSpeed > 0)
                                robot.turnLeft();
                            else
                                robot.turnRight();
                            break;
                        default:
                            robot.stop();
                            break;
                    }
                }
            }
        });
    }

    private void TryConnectRobot() {
        if (robotIndex < MainActivity.robotList.size()) {
            serialNumber = MainActivity.robotList.get(robotIndex);
            RobotManager.getInstance(MainActivity.mainApp).connectRobotByBluetooth(serialNumber);
            Log.d(TAG, "Connecting to " + serialNumber);
        }
    }

    @Override
    public void onShutdown(Node node) {

    }

    @Override
    public void onShutdownComplete(Node node) {

    }

    @Override
    public void onError(Node node, Throwable throwable) {

    }

    @Override
    public void onRobotConnected(Robot r) {
        this.robot = r;
        this.robot.setListener(this);
        Log.d(TAG, "Connected to " + serialNumber);
        if (robot != null)
        {
            robot.setMovementSpeed(1);
            robot.queryBatteryPercentage();
            robot.queryObstacleDistanceData();
        }
    }

    @Override
    public void onRobotConnectFailed(java.lang.String s) {
        this.robot = null;
        robotIndex += 1;
        if (robotIndex >= MainActivity.robotList.size()) {
            robotIndex = 0;
        }
        Log.d(TAG, "Connecting failed, keep trying");
        TryConnectRobot();
    }

    @Override
    public void onRobotDisconnected(java.lang.String s, RobotDisconnectType robotDisconnectType) {
        this.robot = null;
        robotIndex = 0;
        Log.d(TAG, "Robot disconnected");
    }

    @Override
    public void onReceivedRobotObstacleDistanceData(ObstacleDistanceData obstacleDistanceData) {
        obstacle = obstacleDistanceData.toString();
    }

    @Override
    public void onReceivedRobotBatteryPercentage(int i) {
        battery = i;
    }

    @Override
    public void onReceivedRobotHardwareVersion(int i) {

    }

    @Override
    public void onReceivedRobotSerialNumber(java.lang.String s) {

    }

    @Override
    public void onReceivedCustomData(java.lang.String s) {

    }

    @Override
    public void onReceivedSoundSourceAngle(int i) {

    }
}
