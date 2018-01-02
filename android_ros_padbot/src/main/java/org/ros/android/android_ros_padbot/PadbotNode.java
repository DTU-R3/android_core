package org.ros.android.android_ros_padbot;

import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;

import std_msgs.Bool;
import std_msgs.Int8;
import std_msgs.String;
import geometry_msgs.Twist;

public class PadbotNode extends AbstractNodeMain {

    private java.lang.String nodeName;

    private double linearSpeed = 0.0;
    private double angularSpeed = 0.0;
    private double deadzone = 0.1;  // a speed smaller than this value will not work

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

        // The loop breaks when the node shuts down
        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void loop() throws InterruptedException {
                std_msgs.Int8 batteryData = batteryPub.newMessage();
                std_msgs.String obstacleData = obstaclePub.newMessage();
                batteryData.setData( (byte) ControlActivity.batteryData);
                obstacleData.setData(ControlActivity.obstacleData);
                batteryPub.publish(batteryData);
                obstaclePub.publish(obstacleData);

                if (ControlActivity.robot != null) {

                    // Set movement speed
                    double s = Math.abs(linearSpeed);
                    double w = Math.abs(angularSpeed);

                    ControlActivity.robot.setMovementSpeed( Math.min( (int) Math.ceil(s/0.3) ,6 ) );

                    if (s < deadzone && w < deadzone) {
                        ControlActivity.robot.stop();
                    }
                    else {

                        int offset = 1;

                        if (s/w >= 3)
                            offset = 1;
                        else if (s/w >= 1)
                            offset = 2;
                        else if (s/w >= 0.375)
                            offset = 3;
                        else
                            offset = 4;

                        if (linearSpeed > deadzone && w < deadzone) {
                            ControlActivity.robot.goForward();
                        }

                        if (linearSpeed < -deadzone && w < deadzone) {
                            ControlActivity.robot.goBackward();
                        }

                        if (s < deadzone && angularSpeed > deadzone) {
                            ControlActivity.robot.turnLeft();
                        }

                        if (s < deadzone && angularSpeed < -deadzone) {
                            ControlActivity.robot.turnRight();
                        }

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
                    if (obstacle_enable)
                        ControlActivity.robot.turnOnObstacleDetection();
                    else
                        ControlActivity.robot.turnOffObstacleDetection();

                    // Query the robot information
                    ControlActivity.robot.queryBatteryPercentage();
                    ControlActivity.robot.queryObstacleDistanceData();
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
