package org.ros.android.android_ros_padbot;

import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import geometry_msgs.Twist;
import std_msgs.Bool;
import std_msgs.Int8;
import std_msgs.String;

public class PadbotNode extends AbstractNodeMain {

    // Node variables
    private static final java.lang.String TAG = PadbotNode.class.getSimpleName();
    private java.lang.String nodeName;
    static public boolean nodeState = true;

    // Padbot variables
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

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void loop() throws InterruptedException {
                // Publish battery and obstacle data
                std_msgs.Int8 batteryData = batteryPub.newMessage();
                std_msgs.String obstacleData = obstaclePub.newMessage();
                batteryData.setData((byte) MainActivity.battery_data);
                obstacleData.setData(MainActivity.obstacle_data);
                batteryPub.publish(batteryData);
                obstaclePub.publish(obstacleData);
            }
        });

        stateSub.addMessageListener(new MessageListener<Bool>() {
            @Override
            public void onNewMessage(Bool bool) {
                nodeState = bool.getData();
            }
        });

        cmdSub.addMessageListener(new MessageListener<String>() {
            @Override
            public void onNewMessage(String string) {
                if (MainActivity.robot != null) {
                    switch(string.getData()) {
                        case "up":
                            MainActivity.robot.headRise();
                            break;
                        case "down":
                            MainActivity.robot.headDown();
                            break;
                        case "stop":
                            MainActivity.robot.stop();
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

                if (MainActivity.robot != null) {
                    switch (vel_cmd){
                        case 1:
                            if(linearSpeed > 0)
                                MainActivity.robot.goForward();
                            else
                                MainActivity.robot.goBackward();
                            break;
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                            if((linearSpeed > 0)&&(angularSpeed > 0))
                                MainActivity.robot.goForwardLeft(vel_cmd-1);
                            else if((linearSpeed > 0)&&(angularSpeed < 0))
                                MainActivity.robot.goForwardRight(vel_cmd-1);
                            else if((linearSpeed < 0)&&(angularSpeed > 0))
                                MainActivity.robot.goBackwardRight(vel_cmd-1);
                            else
                                MainActivity.robot.goBackwardLeft(vel_cmd-1);
                            break;
                        case 6:
                            if(angularSpeed > 0)
                                MainActivity.robot.turnLeft();
                            else
                                MainActivity.robot.turnRight();
                            break;
                        default:
                            MainActivity.robot.stop();
                            break;
                    }
                }
            }
        });
    }
}
