package org.ros.android.android_ros_padbot;

import com.google.ar.core.Frame;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import geometry_msgs.Point;
import geometry_msgs.Pose;
import geometry_msgs.PoseWithCovariance;
import geometry_msgs.Quaternion;
import nav_msgs.Odometry;
import std_msgs.Bool;
import std_msgs.Header;

public class ARCore implements NodeMain {

    private static final String TAG = ARCore.class.getSimpleName();
    private String nodeName;
    private Boolean nodeState = Boolean.TRUE;

    // ARCore
    private com.google.ar.core.Pose cameraPose;
    private Frame frame;

    public ARCore() {
        this.nodeName = "ARCore";
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of(nodeName);
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {

        // Set an update listener on the Scene that will hide the loading message once a Plane is
        MainActivity.arSceneView
                .getScene()
                .setOnUpdateListener(
                        frameTime -> {
                            if (nodeState) {
                                frame = MainActivity.arSceneView.getArFrame();
                                if (frame == null) {
                                    return;
                                }
                                cameraPose = frame.getCamera().getPose();
                            }
                        });

        // Publishers and subscribers
        final Publisher<Odometry> odomPub = connectedNode.newPublisher("arcore/odom", Odometry._TYPE);
        final Subscriber<Bool> stateSub = connectedNode.newSubscriber("arcore/state",Bool._TYPE);

        connectedNode.executeCancellableLoop(new CancellableLoop() {

            protected void loop() throws InterruptedException {
                if (nodeState) {
                    try {
                        Odometry robot_odom = odomPub.newMessage();
                        Header h = robot_odom.getHeader();
                        h.setFrameId("odom");
                        robot_odom.setChildFrameId("base_footprint");
                        PoseWithCovariance poseWithC = robot_odom.getPose();
                        Pose robot_pose = poseWithC.getPose();
                        float[] trans = cameraPose.getTranslation();
                        Point p = robot_pose.getPosition();
                        p.setX(trans[0]);
                        p.setY(trans[1]);
                        p.setZ(trans[2]);
                        float[] quat = cameraPose.getRotationQuaternion();
                        Quaternion q = robot_pose.getOrientation();
                        q.setX(quat[0]);
                        q.setY(quat[1]);
                        q.setZ(quat[2]);
                        q.setW(quat[3]);
                        odomPub.publish(robot_odom);
                    } catch (Exception e) {

                    }
                }
            }
        });

        stateSub.addMessageListener(new MessageListener<Bool>() {
            @Override
            public void onNewMessage(Bool bool) {
                nodeState = bool.getData();
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
