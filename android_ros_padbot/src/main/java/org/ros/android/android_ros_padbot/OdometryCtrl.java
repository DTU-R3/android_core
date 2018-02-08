package org.ros.android.android_ros_padbot;


import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import geometry_msgs.Pose2D;
import geometry_msgs.Twist;
import geometry_msgs.Vector3;
import std_msgs.String;

public class OdometryCtrl implements NodeMain {

    private java.lang.String nodeName;
    private java.lang.String robot_state = "idle";
    private double distance = 0.0;
    private double angle = 0.0;
    private double robot_x = 0.0;
    private double robot_y = 0.0;
    private double robot_th = 0.0;
    private double start_x = 0.0;
    private double start_y = 0.0;
    private double start_th = 0.0;
    private double linearSpeed = 0.0;
    private double angularSpeed = 0.0;

    public OdometryCtrl() {
        this.nodeName = "Odometry_control";
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(nodeName);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        // Publishers and subscribers
        final Publisher<Twist> velPub = connectedNode.newPublisher("cmd_vel",Twist._TYPE);
        final Publisher<String> statePub = connectedNode.newPublisher("odometry_control/robot_state", String._TYPE);
        final Subscriber<Pose2D> poseSub = connectedNode.newSubscriber("VirtualEncoder/robot_pose", Pose2D._TYPE);
        final Subscriber<String> cmdSub = connectedNode.newSubscriber("odometry_control/cmd", String._TYPE);

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void loop() throws InterruptedException {
                switch(robot_state) {
                    case "fwd":
                        if ( Math.abs(Math.sqrt( (robot_x-start_x)*(robot_x-start_x) + (robot_y-start_y)*(robot_y-start_y)) - Math.abs(distance)) < 0.1) {
                            linearSpeed = 0;
                            angularSpeed = 0;
                            robot_state = "done";
                        }
                        break;
                    case "turn":
                        if (Math.abs(robot_th - FitInRadians(start_th + angle) ) < 0.1) {
                            linearSpeed = 0;
                            angularSpeed = 0;
                            robot_state = "done";
                        }
                        break;
                    case "stop":
                        linearSpeed = 0;
                        angularSpeed = 0;
                        robot_state = "done";
                        break;
                    default:
                        linearSpeed = 0;
                        angularSpeed = 0;
                        robot_state = "idle";
                        break;
                }

                if (!robot_state.equals("idle")) {
                    // Publish the velocity
                    geometry_msgs.Twist padbot_vel = velPub.newMessage();
                    Vector3 linear = padbot_vel.getLinear();
                    if (linearSpeed != 0)
                        linear.setX(linearSpeed*Math.abs(linearSpeed));
                    else
                        linear.setX(0);
                    linear.setY(0);
                    linear.setZ(0);

                    Vector3 angular = padbot_vel.getAngular();
                    angular.setX(0);
                    angular.setY(0);
                    if (angularSpeed != 0)
                        angular.setZ(angularSpeed*Math.abs(angularSpeed));
                    else
                        angular.setZ(0);

                    velPub.publish(padbot_vel);
                }
                std_msgs.String state = statePub.newMessage();
                state.setData(robot_state);
                statePub.publish(state);
            }
        });

        poseSub.addMessageListener(new MessageListener<Pose2D>() {
            @Override
            public void onNewMessage(Pose2D pose2D) {
                robot_x = pose2D.getX();
                robot_y = pose2D.getY();
                robot_th = pose2D.getTheta();
            }
        });

        cmdSub.addMessageListener(new MessageListener<String>() {
            @Override
            public void onNewMessage(String string) {
                java.lang.String str = string.toString();
                java.lang.String[] cmds = str.split(",");
                if (cmds.length>2) {
                    robot_state = cmds[0];
                    switch(robot_state) {
                        case "fwd":
                            distance = Double.parseDouble(cmds[1]);
                            if(distance >= 0)
                                linearSpeed = 1;
                            else
                                linearSpeed = -1;
                            angularSpeed = 0;
                            start_x = robot_x;
                            start_y = robot_y;
                            break;
                        case "turn":
                            angle = Double.parseDouble(cmds[1]) * Math.PI / 180.0;
                            angle = FitInRadians(angle);
                            linearSpeed = 0;
                            if(angle >= 0)
                                angularSpeed = 1;
                            else
                                angularSpeed = -1;
                            start_th = robot_th;
                            break;
                        case "stop":
                            linearSpeed = 0;
                            angularSpeed = 0;
                            break;
                        default:
                            break;
                    }
                }
            }
        });
    }

    private double FitInRadians(double v) {
        double r = v;
        while(r > Math.PI) {
            r = r - 2.0 * Math.PI;
        }
        while(r < Math.PI) {
            r = r + 2.0 * Math.PI;
        }
        return r;
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
