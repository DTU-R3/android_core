package org.ros.android.android_ros_padbot;

import android.util.Log;

import com.google.ar.core.TrackingState;

import org.ros.concurrent.CancellableLoop;
import org.ros.internal.message.RawMessage;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import geometry_msgs.Point;
import geometry_msgs.Pose;
import geometry_msgs.Quaternion;

public class ARCore implements NodeMain {

    private static final String TAG = ARCore.class.getSimpleName();
    private String nodeName;

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
        final Publisher<Pose> posePub = connectedNode.newPublisher("arcore/pose", Pose._TYPE);

        connectedNode.executeCancellableLoop(new CancellableLoop() {

            protected void loop() throws InterruptedException {
                try {
                    Pose robot_pose = posePub.newMessage();
                    float[] trans = ControlActivity.cameraPose.getTranslation();
                    Point p = robot_pose.getPosition();
                    p.setX(trans[0]);
                    p.setY(trans[1]);
                    p.setZ(trans[2]);
                    float[] quat = ControlActivity.cameraPose.getRotationQuaternion();
                    Quaternion q = robot_pose.getOrientation();
                    q.setX(quat[0]);
                    q.setY(quat[1]);
                    q.setZ(quat[2]);
                    q.setW(quat[3]);
                    posePub.publish(robot_pose);
                } catch (Exception e) {

                }
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
