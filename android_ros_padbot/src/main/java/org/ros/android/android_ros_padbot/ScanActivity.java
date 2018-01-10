package org.ros.android.android_ros_padbot;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.widget.PullRefreshLayout;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.util.ArrayList;
import java.util.List;

import cn.inbot.padbotsdk.RobotManager;
import cn.inbot.padbotsdk.listener.RobotScanListener;
import rx.functions.Action1;

public class ScanActivity extends Activity implements RobotScanListener {

    private PullRefreshLayout mRefreshView;
    private ListView mListView;
    private RobotAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        setTitle("Pull to scan robot");

        mRefreshView = (PullRefreshLayout) findViewById(R.id.refresh_view);
        mRefreshView.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                RxPermissions rxPermissions = new RxPermissions(ScanActivity.this);
                rxPermissions
                        .request(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN
                                , Manifest.permission.ACCESS_COARSE_LOCATION)
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean aBoolean) {
                                if (aBoolean) {

                                    mAdapter.getRobotList().clear();
                                    mAdapter.notifyDataSetChanged();

                                    // Set the scan listener
                                    RobotManager.getInstance(getApplication()).setRobotScanListener(ScanActivity.this);
                                    // The argument is scan time, the unit is second
                                    RobotManager.getInstance(getApplication()).scanForRobots(8);
                                } else {
                                    mRefreshView.setRefreshing(false);
                                    Toast.makeText(ScanActivity.this, "Please agree with the app permissions", Toast.LENGTH_LONG).show();
                                }
                            }
                        });

            }
        });

        mAdapter = new RobotAdapter(this);
        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                List<String> robotList = mAdapter.getRobotList();
                if (null != robotList && position < robotList.size()) {
                    String serialNumber = robotList.get(position);
                    Intent intent = new Intent();
                    intent.setClass(ScanActivity.this, ControlActivity.class);
                    intent.putExtra("serialNumber", serialNumber);
                    intent.putExtra("model", 1);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStop() {
        super.onStop();

        RobotManager.getInstance(getApplication()).stopScan();
    }

    @Override
    public void onRobotDiscovered(String s, int i) {
        List<String> robotList = mAdapter.getRobotList();

        boolean isExist = false;
        if (null != robotList && !robotList.isEmpty()) {
            for (String serialNumber : robotList) {
                if (s.equals(serialNumber)) {
                    isExist = true;
                    break;
                }
            }
        }

        if (!isExist) {
            mAdapter.addRobot(s);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRobotScanCompleted() {
        mRefreshView.setRefreshing(false);
    }

    private class RobotAdapter extends BaseAdapter {

        private LayoutInflater mInflater;
        private List<String> robotList;

        public RobotAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
            robotList = new ArrayList<>();
        }

        public void setRobotList(List<String> robotList) {
            this.robotList = robotList;
        }

        public List<String> getRobotList() {
            return this.robotList;
        }

        public void addRobot(String robotName) {
            this.robotList.add(robotName);
        }

        @Override
        public int getCount() {
            return robotList.size();
        }

        @Override
        public Object getItem(int position) {
            return robotList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            String robotName = robotList.get(position);

            RobotViewHolder viewHolder = null;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item_robot, null);
                TextView nameTextView = (TextView) convertView.findViewById(R.id.item_robot_name);

                viewHolder = new RobotViewHolder();
                viewHolder.nameTextView = nameTextView;
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (RobotViewHolder) convertView.getTag();
            }

            viewHolder.nameTextView.setText("Robot Name: " + robotName);

            return convertView;
        }
    }

    private class RobotViewHolder {
        public TextView nameTextView;
    }

    /**
     * Created by ros on 1/10/18.
     */

    public static class VirtualEncoder {
    }
}
