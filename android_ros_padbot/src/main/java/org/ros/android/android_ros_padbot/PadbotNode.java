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
    private int battery_data = 0;
    private java.lang.String obstacle_data = "";

    private double linearSpeed = 0.0;
    private double angularSpeed = 0.0;
    private int vel_cmd = 0;

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
        final Publisher<Int8> batteryPub = connectedNode.newPublisher("padbot/battery_percentage", Int8._TYPE);
        final Publisher<String> obstaclePub = connectedNode.newPublisher("padbot/obstacle", String._TYPE);
        final Subscriber<geometry_msgs.Twist> velSub = connectedNode.newSubscriber("cmd_vel", Twist._TYPE);
        final Subscriber<Bool> stateSub = connectedNode.newSubscriber("padbot/state",Bool._TYPE);
        final Subscriber<String> cmdSub = connectedNode.newSubscriber("padbot/cmd",String._TYPE);
        final Subscriber<Int8> speedSub = connectedNode.newSubscriber("padbot/speed_level",Int8._TYPE);
        final Subscriber<String> robotSub = connectedNode.newSubscriber("padbot/robot",String._TYPE);

        RobotManager.getInstance(MainActivity.mainApp).setRobotConnectionListener(this);
        TryConnectRobot();

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void loop() throws InterruptedException {
                // Publish battery and obstacle data
                std_msgs.Int8 batteryData = batteryPub.newMessage();
                std_msgs.String obstacleData = obstaclePub.newMessage();
                batteryData.setData((byte) battery_data);
                obstacleData.setData(obstacle_data);
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
                    Log.d(TAG, "Disconnecting robot");
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
                if ((v==0)&&(w==0)) {
                    vel_cmd = 6;
                }
                else {
                    vel_cmd = (int) (Math.atan2(w,v) / (Math.PI/12));
                }

                if (robot != null) {
                    switch (vel_cmd){
                        case 0:
                            if(linearSpeed > 0)
                                robot.goForward();
                            else
                                robot.goBackward();
                            break;
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                            if((linearSpeed > 0)&&(angularSpeed > 0))
                                robot.goForwardLeft(vel_cmd);
                            else if((linearSpeed > 0)&&(angularSpeed < 0))
                                robot.goForwardRight(vel_cmd);
                            else if((linearSpeed < 0)&&(angularSpeed > 0))
                                robot.goBackwardRight(vel_cmd);
                            else
                                robot.goBackwardLeft(vel_cmd);
                            break;
                        case 5:
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

        speedSub.addMessageListener(new MessageListener<Int8>() {
            @Override
            public void onNewMessage(Int8 int8) {
                if (robot != null) {
                    robot.setMovementSpeed(int8.getData());
                }
            }
        });

        robotSub.addMessageListener(new MessageListener<String>() {
            @Override
            public void onNewMessage(String string) {
                if (robot != null) {
                    RobotManager.getInstance(MainActivity.mainApp).disconnectRobot();
                }
                serialNumber = string.getData();
                RobotManager.getInstance(MainActivity.mainApp).connectRobotByBluetooth(serialNumber);
                Log.d(TAG, "Connecting to " + serialNumber);
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
        Log.d(TAG, "Connecting failed, try another robot");
        TryConnectRobot();
    }

    @Override
    public void onRobotDisconnected(java.lang.String s, RobotDisconnectType robotDisconnectType) {
        this.robot = null;
        robotIndex = 0;
        Log.d(TAG, "Robot disconnected");
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
    public void onReceivedRobotSerialNumber(java.lang.String s) {

    }

    @Override
    public void onReceivedCustomData(java.lang.String s) {

    }

    @Override
    public void onReceivedSoundSourceAngle(int i) {

    }
}
