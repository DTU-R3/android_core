package org.ros.android.android_ros_padbot;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.util.Timer;
import java.util.TimerTask;

import geometry_msgs.Pose2D;
import geometry_msgs.Twist;

public class ARCore implements NodeMain {

    private String nodeName;

    private double robot_x = 0.0;
    private double robot_y = 0.0;
    private double robot_th = 0.0;

    private double vel_linear = 0.0;
    private double vel_angular = 0.0;

    public ARCore() {
        this.nodeName = "ARCore";
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(nodeName);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        // Publishers and subscribers
        final Publisher<Pose2D> posePub = connectedNode.newPublisher("VirtualEncoder/robot_pose", Pose2D._TYPE);
        final Subscriber<Twist> velSub = connectedNode.newSubscriber("padbot/cmd_vel", Twist._TYPE);
        final Subscriber<Pose2D> poseSub = connectedNode.newSubscriber("padbot/robot_pose", Pose2D._TYPE);

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void loop() throws InterruptedException {
                Pose2D robot_pose = posePub.newMessage();
                robot_pose.setX(robot_x);
                robot_pose.setY(robot_y);
                robot_pose.setTheta(robot_th);
                posePub.publish(robot_pose);
            }
        });

        velSub.addMessageListener(new MessageListener<Twist>() {
            @Override
            public void onNewMessage(Twist twist) {
                vel_linear = twist.getLinear().getX();
                vel_angular = twist.getAngular().getZ();
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

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                double deltaDis = vel_linear * 0.001;
                double deltaTheta = vel_angular * 0.001;
                robot_x = robot_x + deltaDis * Math.cos(robot_th);
                robot_y = robot_y + deltaDis * Math.sin(robot_th);
                robot_th = robot_th + deltaTheta;
                if (robot_th > Math.PI) {
                    robot_th = robot_th - 2 * Math.PI;
                }
                if (robot_th < -Math.PI) {
                    robot_th = robot_th + 2 * Math.PI;
                }

            }
        }, 0,1);
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
