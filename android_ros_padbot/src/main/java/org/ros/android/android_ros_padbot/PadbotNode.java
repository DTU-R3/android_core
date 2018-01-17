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
import std_msgs.Bool;
import std_msgs.Int8;
import std_msgs.String;

public class PadbotNode extends AbstractNodeMain {

    private java.lang.String nodeName;

    private double linearSpeed = 0.0;
    private double angularSpeed = 0.0;
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
        final Subscriber<geometry_msgs.Twist> speedSub = connectedNode.newSubscriber("cmd_vel", Twist._TYPE);
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

                    // Set movement speed
                    double v = Math.abs(linearSpeed);
                    double w = Math.abs(angularSpeed);

                    ControlActivity.robot.setMovementSpeed( Math.min( (int) Math.ceil((v+w)/0.3) ,6 ) );

                    if (w <= deadzone) {
                        if (linearSpeed > deadzone)
                            ControlActivity.robot.goForward();
                        else if (linearSpeed < -deadzone)
                            ControlActivity.robot.goBackward();
                        else
                            ControlActivity.robot.stop();
                    }
                    else if (v <= deadzone) {
                        if (angularSpeed > deadzone)
                            ControlActivity.robot.turnLeft();
                        else if (angularSpeed < -deadzone)
                            ControlActivity.robot.turnRight();
                        else
                            ControlActivity.robot.stop();
                    }
                    else {
                        int offset = 1;

                        if (v/w >= 3)
                            offset = 1;
                        else if (v/w >= 1)
                            offset = 2;
                        else if (v/w >= 0.33)
                            offset = 3;
                        else
                            offset = 4;

                        if (linearSpeed > deadzone && angularSpeed > deadzone) {
                            ControlActivity.robot.goForwardLeft(offset);
                        }

                        if (linearSpeed > deadzone && angularSpeed < -deadzone) {
                            ControlActivity.robot.goForwardRight(offset);
                        }

                        if (linearSpeed < -deadzone && angularSpeed > deadzone) {
                            ControlActivity.robot.goBackwardRight(offset);
                        }

                        if (linearSpeed < -deadzone && angularSpeed < -deadzone) {
                            ControlActivity.robot.goBackwardLeft(offset);
                        }
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

        speedSub.addMessageListener(new MessageListener<Twist>() {
            @Override
            public void onNewMessage(Twist twist) {
                linearSpeed = twist.getLinear().getX();
                angularSpeed = twist.getAngular().getZ();
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
