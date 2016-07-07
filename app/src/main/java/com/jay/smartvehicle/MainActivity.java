package com.jay.smartvehicle;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jay.smartvehicle.comm.DataStreamMsg;
import com.jay.smartvehicle.comm.ErrorCodeMsg;
import com.jay.smartvehicle.comm.HeartbeatMsg;
import com.jay.smartvehicle.comm.VehicleClient;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final static int CONNECTING_TIMEOUT = 10000;

    private final static String TAG = "SmartVehicle";
    private Toolbar mToolbar;
    private boolean mStart = false;
    private Button mStartButton;
    private Button mErrorCodeButton;
    private Button mDataStreamButton;

    private VehicleClient mClient;
    private DeviceInputDialog mDeviceDialog;
    private DeviceInputDialog mErrorCodeInputDialog;
    private DeviceInputDialog mDataStreeamInputDialog;

    private ProgressDialog mWaitDialog;
    private String mRecvMsg = "";
    private TextView mMsgTextView;
    private Moshi mMoshi;

    private Toast mToast;

    private Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case VehicleClient.EVENT_CONNECTED:
                    dismissDialog();
                    toastShow(getString(R.string.connected));
                    mErrorCodeButton.setEnabled(true);
                    mDataStreamButton.setEnabled(true);
                    break;
                case VehicleClient.EVENT_DISCONNECTED:
                    toastShow(getString(R.string.disconnected));
                    mErrorCodeButton.setEnabled(false);
                    mDataStreamButton.setEnabled(false);
                    break;
                case VehicleClient.EVENT_MSG_UPDATE:
                    String recv = (String) msg.obj;
                    mRecvMsg += recv + "\n";
                    updateMsg();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initToolbar();
        initUi();
        mClient = new VehicleClient("139.224.17.163", 8880);
        mDeviceDialog = new DeviceInputDialog();
        mErrorCodeInputDialog = new DeviceInputDialog();
        mDataStreeamInputDialog = new DeviceInputDialog();
        mWaitDialog = new ProgressDialog(this);
        mWaitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mWaitDialog.setCancelable(false);
        mMoshi = new Moshi.Builder().build();

        String[] items = {getString(R.string.chebang), getString(R.string.cherui)};
        showSelectDialog(getString(R.string.select), items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int which) {
                if (which == 0) {
                    mClient.setType(VehicleClient.COMM_TYPE_CHEBANG);
                    mClient.setParam("139.196.46.68", 9999);
                } else if (which == 1) {
                    mClient.setType(VehicleClient.COMM_TYPE_CHERUI);
                    mClient.setParam("139.224.17.163", 8880);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mClient != null) {
            mClient.shutdown();
        }
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.tb_main);
        setSupportActionBar(mToolbar);
        mToolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        mToolbar.setTitleTextColor(getResources().getColor(R.color.colorTitle));
        mToolbar.setTitle(getResources().getString(R.string.obd_simulator));
//        mToolbar.setNavigationIcon(R.mipmap.bluetooth_disabled);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "on Navigation Click!");

            }
        });
    }

    private void initUi() {
        mStartButton = (Button) findViewById(R.id.bt_start);
        mStartButton.setOnClickListener(this);
        mErrorCodeButton = (Button) findViewById(R.id.bt_error_code);
        mDataStreamButton = (Button) findViewById(R.id.bt_data_stream);
        mErrorCodeButton.setOnClickListener(this);
        mDataStreamButton.setOnClickListener(this);

        mMsgTextView = (TextView) findViewById(R.id.tv_recv);
        mMsgTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mToast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT);

        mErrorCodeButton.setEnabled(false);
        mDataStreamButton.setEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_setting:
//                Log.d(TAG, "setting!");
                mDeviceDialog.init(getString(R.string.action_setting), getString(R.string.deviceid), new DeviceInputDialog.DeviceInputListern() {
                    @Override
                    public void onSettingInputComplete(String id) {
                        mClient.setDeviceId(id);
                    }
                });
                mDeviceDialog.show(getFragmentManager(), "setting");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_start:
                if (mStart) {
                    mStart = false;
                    mStartButton.setText(getString(R.string.start));
                    if (mClient != null) {
                        mClient.shutdown();
                    }
                } else {
                    mStart = true;
                    mStartButton.setText(getString(R.string.stop));
                    mClient.setup(mUiHandler);
                    loadConnectingDialog();
                }
                break;
            case R.id.bt_error_code:
                final String[] items = {"ENG", "AT", "ABS", "SRS", "BCM",
                        "IPC", "EPS", "AC", "TPMS"};
                showSelectDialog(getString(R.string.error_code), items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        mErrorCodeInputDialog.init(items[which], getString(R.string.error_code), new DeviceInputDialog.DeviceInputListern() {
                            @Override
                            public void onSettingInputComplete(String id) {
                                toastShow(id);
                                ErrorCodeMsg msg = new ErrorCodeMsg();
                                JsonAdapter<ErrorCodeMsg> jsonAdapter = mMoshi.adapter(ErrorCodeMsg.class);
                                String toMsg = "";

                                switch (which) {
                                    //eng
                                    case 0:
                                        msg.deviceid = mClient.getDeviceId();
                                        msg.msg_type = 5;
                                        msg.eng_code = id;
                                        toMsg = jsonAdapter.toJson(msg);
                                        Log.d(TAG, "SEND: " + toMsg);
                                        mClient.sendMsg(toMsg);
                                        break;
                                    //at
                                    case 1:
                                        msg.deviceid = mClient.getDeviceId();
                                        msg.msg_type = 5;
                                        msg.at_code = id;
                                        toMsg = jsonAdapter.toJson(msg);
                                        Log.d(TAG, "SEND: " + toMsg);
                                        mClient.sendMsg(toMsg);
                                        break;
                                    //abs
                                    case 2:
                                        msg.deviceid = mClient.getDeviceId();
                                        msg.msg_type = 5;
                                        msg.abs_code = id;
                                        toMsg = jsonAdapter.toJson(msg);
                                        Log.d(TAG, "SEND: " + toMsg);
                                        mClient.sendMsg(toMsg);
                                        break;
                                    //srs
                                    case 3:
                                        msg.deviceid = mClient.getDeviceId();
                                        msg.msg_type = 5;
                                        msg.srs_code = id;
                                        toMsg = jsonAdapter.toJson(msg);
                                        Log.d(TAG, "SEND: " + toMsg);
                                        mClient.sendMsg(toMsg);
                                        break;
                                    //bcm
                                    case 4:
                                        msg.deviceid = mClient.getDeviceId();
                                        msg.msg_type = 5;
                                        msg.bcm_code = id;
                                        toMsg = jsonAdapter.toJson(msg);
                                        Log.d(TAG, "SEND: " + toMsg);
                                        mClient.sendMsg(toMsg);
                                        break;
                                    //ipc
                                    case 5:
                                        msg.deviceid = mClient.getDeviceId();
                                        msg.msg_type = 5;
                                        msg.ipc_code = id;
                                        toMsg = jsonAdapter.toJson(msg);
                                        Log.d(TAG, "SEND: " + toMsg);
                                        mClient.sendMsg(toMsg);
                                        break;
                                    //eps
                                    case 6:
                                        msg.deviceid = mClient.getDeviceId();
                                        msg.msg_type = 5;
                                        msg.eps_code = id;
                                        toMsg = jsonAdapter.toJson(msg);
                                        Log.d(TAG, "SEND: " + toMsg);
                                        mClient.sendMsg(toMsg);
                                        break;
                                    //ac
                                    case 7:
                                        msg.deviceid = mClient.getDeviceId();
                                        msg.msg_type = 5;
                                        msg.ac_code = id;
                                        toMsg = jsonAdapter.toJson(msg);
                                        Log.d(TAG, "SEND: " + toMsg);
                                        mClient.sendMsg(toMsg);
                                        break;
                                    //tpms
                                    case 8:
                                        msg.deviceid = mClient.getDeviceId();
                                        msg.msg_type = 5;
                                        msg.tpms_code = id;
                                        toMsg = jsonAdapter.toJson(msg);
                                        Log.d(TAG, "SEND: " + toMsg);
                                        mClient.sendMsg(toMsg);
                                        break;
                                    default:
                                        break;
                                }
                            }
                        });
                        mErrorCodeInputDialog.show(getFragmentManager(), "error_code");
                    }
                });

                break;
            case R.id.bt_data_stream:

                final String[] stream_types = {"eng_data_rpm", "eng_data_vs"};
                showSelectDialog(getString(R.string.data_stream), stream_types, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        mDataStreeamInputDialog.init(stream_types[which], getString(R.string.data_stream), new DeviceInputDialog.DeviceInputListern() {
                            @Override
                            public void onSettingInputComplete(String id) {
                                toastShow(id);
                                DataStreamMsg msg = new DataStreamMsg();
                                JsonAdapter<DataStreamMsg> jsonAdapter = mMoshi.adapter(DataStreamMsg.class);
                                String toMsg = "";

                                switch (which) {
                                    //rpm
                                    case 0:
                                        msg.deviceid = mClient.getDeviceId();
                                        if (mClient.getType() == VehicleClient.COMM_TYPE_CHEBANG) {
                                            msg.msg_type = 0;
                                        } else if (mClient.getType() == VehicleClient.COMM_TYPE_CHERUI) {
                                            msg.msg_type = 4;
                                        }
                                        msg.eng_data_rpm = id;
                                        toMsg = jsonAdapter.toJson(msg);
                                        Log.d(TAG, "SEND: " + toMsg);
                                        mClient.sendMsg(toMsg);
                                        break;
                                    //vs
                                    case 1:
                                        msg.deviceid = mClient.getDeviceId();
                                        if (mClient.getType() == VehicleClient.COMM_TYPE_CHEBANG) {
                                            msg.msg_type = 0;
                                        } else if (mClient.getType() == VehicleClient.COMM_TYPE_CHERUI) {
                                            msg.msg_type = 4;
                                        }
                                        msg.eng_data_vs = id;
                                        toMsg = jsonAdapter.toJson(msg);
                                        Log.d(TAG, "SEND: " + toMsg);
                                        mClient.sendMsg(toMsg);
                                        break;

                                    default:
                                        break;
                                }
                            }
                        });
                        mDataStreeamInputDialog.show(getFragmentManager(), "error_code");
                    }
                });

                break;
        }
    }

    private void showSelectDialog(String title, final String[] items, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setItems(items, listener);
        builder.create().show();
    }

    private Handler mHandler = new Handler();

    private void loadConnectingDialog() {
        final Runnable callback = new Runnable() {
            @Override
            public void run() {
                mWaitDialog.dismiss();
                toastShow(getString(R.string.connect_timeout));
            }
        };
        if (!mWaitDialog.isShowing()) {
            mWaitDialog.setMessage(getString(R.string.connecting));
            mWaitDialog.show();
            mHandler.postDelayed(callback, CONNECTING_TIMEOUT);
            mWaitDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mHandler.removeCallbacks(callback);
                }
            });
        }
    }

    private void dismissDialog() {
        if (mWaitDialog.isShowing()) {
            mWaitDialog.dismiss();
        }
    }

    private void updateMsg() {
        mMsgTextView.setText(mRecvMsg);
    }

    private void toastShow(String msg) {
        if (mToast != null) {
            mToast.setText(msg);
            mToast.show();
        }
    }
}
