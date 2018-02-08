package org.ros.android.android_ros_padbot;

import android.hardware.Camera;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import geometry_msgs.Twist;
import geometry_msgs.Vector3;
import std_msgs.Bool;
import std_msgs.Int8;
import std_msgs.String;

public class PadbotNode extends AbstractNodeMain {

    private java.lang.String nodeName;

    private double linearSpeed = 0.0;
    private double angularSpeed = 0.0;
    private int vel_cmd = 0;
    private double[] vel_linear = {0, 0.0779, 0.0763, 0.0762, 0.0753, 0.0752, 0};
    private double[] vel_angular = {0, 0, 0.0312, 0.0585, 0.0896, 0.1212, 0.865};
    private double deadzone = 0;  // a speed smaller than this value will not work

    private java.lang.String headMoveCmd = "null";
    private boolean obstacle_enable = true;

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
        final Publisher<geometry_msgs.Twist> velPub = connectedNode.newPublisher("padbot/cmd_vel", Twist._TYPE);
        final Subscriber<geometry_msgs.Twist> velSub = connectedNode.newSubscriber("cmd_vel", Twist._TYPE);
        final Subscriber<std_msgs.String> headSub = connectedNode.newSubscriber("padbot/headmove",String._TYPE);
        final Subscriber<std_msgs.Bool> enObstacleSub = connectedNode.newSubscriber("padbot/obstacle_enable", Bool._TYPE);
        final Subscriber<std_msgs.Int8> cameraIDSub = connectedNode.newSubscriber("padbot/cameraID", Int8._TYPE);

        // ROS parameters
        ParameterTree params = connectedNode.getParameterTree();
        params.set("/robot_description", ControlActivity.urdf);

        // The loop breaks when the node shuts down
        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void loop() throws InterruptedException {

                // Publish battery and obstacle data
                std_msgs.Int8 batteryData = batteryPub.newMessage();
                std_msgs.String obstacleData = obstaclePub.newMessage();
                batteryData.setData( (byte) ControlActivity.batteryData);
                obstacleData.setData(ControlActivity.obstacleData);
                batteryPub.publish(batteryData);
                obstaclePub.publish(obstacleData);

                if (ControlActivity.robot != null) {

                    ControlActivity.robot.setMovementSpeed(1);

                    switch (vel_cmd){
                        case 1:
                            if(linearSpeed > 0)
                                ControlActivity.robot.goForward();
                            else
                                ControlActivity.robot.goBackward();
                            break;
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                            if((linearSpeed > 0)&&(angularSpeed > 0))
                                ControlActivity.robot.goForwardLeft(vel_cmd-1);
                            else if((linearSpeed > 0)&&(angularSpeed < 0))
                                ControlActivity.robot.goForwardRight(vel_cmd-1);
                            else if((linearSpeed < 0)&&(angularSpeed > 0))
                                ControlActivity.robot.goBackwardRight(vel_cmd-1);
                            else
                                ControlActivity.robot.goBackwardLeft(vel_cmd-1);
                            break;
                        case 6:
                            if(angularSpeed > 0)
                                ControlActivity.robot.turnLeft();
                            else
                                ControlActivity.robot.turnRight();
                            break;
                        default:
                            ControlActivity.robot.stop();
                            break;
                    }

                    // Set robot movement
                    switch (headMoveCmd){
                        case "up":
                            ControlActivity.robot.headRise();
                            break;
                        case "down":
                            ControlActivity.robot.headDown();
                            break;
                        case "stop":
                            ControlActivity.robot.stop();
                            headMoveCmd = "null";
                            break;
                        default:
                            break;
                    }

                    if (ControlActivity.stopBtnClicked)
                    {
                        linearSpeed = 0.0;
                        angularSpeed = 0.0;
                        headMoveCmd = "null";
                        ControlActivity.robot.stop();
                        ControlActivity.stopBtnClicked = false;
                    }

                    // Enable/Disable obstacle detection
                    if (obstacle_enable) {
                        ControlActivity.robot.turnOnObstacleDetection();
                        ControlActivity.robot.queryObstacleDistanceData();
                    }
                    else {
                        ControlActivity.robot.turnOffObstacleDetection();
                    }

                    // Query the robot information
                    ControlActivity.robot.queryBatteryPercentage();

                }

                Thread.sleep(100);
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
                geometry_msgs.Twist padbot_vel = velPub.newMessage();
                Vector3 linear = padbot_vel.getLinear();
                if (linearSpeed != 0)
                    linear.setX(linearSpeed*vel_linear[vel_cmd]/v);
                else
                    linear.setX(0);
                linear.setY(0);
                linear.setZ(0);

                Vector3 angular = padbot_vel.getAngular();
                angular.setX(0);
                angular.setY(0);
                if (angularSpeed != 0)
                    angular.setZ(angularSpeed*vel_angular[vel_cmd]/w);
                else
                    angular.setZ(0);

                velPub.publish(padbot_vel);
            }
        });

        headSub.addMessageListener(new MessageListener<String>() {
            @Override
            public void onNewMessage(String string) {
                headMoveCmd = string.getData();
            }
        });

        enObstacleSub.addMessageListener(new MessageListener<Bool>() {
            @Override
            public void onNewMessage(Bool bool) {
                obstacle_enable = bool.getData();
            }
        });

        cameraIDSub.addMessageListener(new MessageListener<Int8>() {
            @Override
            public void onNewMessage(Int8 int8) {
                int numberOfCameras = Camera.getNumberOfCameras();
                if (numberOfCameras > 1) {
                    ControlActivity.cameraId = int8.getData() % numberOfCameras;
                }
                else {
                    ControlActivity.cameraId = 0;
                }
                ControlActivity.rosCameraPreviewView.releaseCamera();
                ControlActivity.rosCameraPreviewView.setCamera(ControlActivity.getCamera());
            }
        });
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
}
