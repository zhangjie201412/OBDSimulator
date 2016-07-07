package com.jay.smartvehicle.comm;

import android.os.Handler;
import android.util.Log;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.ConnectCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.net.InetSocketAddress;

/**
 * Created by H151136 on 7/7/2016.
 */
public class VehicleClient {
    private final static String TAG = "SmartVehicle-Client";

    public static final int COMM_TYPE_CHEBANG = 0;
    public static final int COMM_TYPE_CHERUI = 1;


    public final static int EVENT_CONNECTED = 0x00;
    public final static int EVENT_DISCONNECTED = 0x01;
    public final static int EVENT_MSG_UPDATE = 0x02;

    private String host;
    private int port;
    private AsyncSocket mSocket;
    private boolean isConnected;
    private final int INTERVAL = 10000;
    private Moshi mMoshi;
    private int mHearbeat;
    private String mDeviceid;
    private Handler mHandler;

    private int mCommType = 0;

    public VehicleClient(String host, int port) {
        this.host = host;
        this.port = port;
        isConnected = false;
        mMoshi = new Moshi.Builder().build();
        mHearbeat = 0;
        //default device id
        mDeviceid = "0000000000000002";
    }

    public void setType(int type) {
        mCommType = type;
    }

    public int getType() {
        return mCommType;
    }

    public void setParam(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getDeviceId() {
        return mDeviceid;
    }

    public void setDeviceId(String id) {
        mDeviceid = id;
    }

    public void setup(Handler handler) {
        mHandler = handler;
        AsyncServer.getDefault().connectSocket(new InetSocketAddress(host, port),
                new ConnectCallback() {
                    @Override
                    public void onConnectCompleted(Exception ex, AsyncSocket socket) {
                        Log.d(TAG, "onConnectCompleted!");
                        isConnected = true;
                        mHandler.obtainMessage(EVENT_CONNECTED).sendToTarget();
                        handleConnectCompleted(ex, socket);
                    }
                });
    }

    private void handleConnectCompleted(Exception ex, final AsyncSocket socket) {
        if (ex != null) throw new RuntimeException(ex);

        mSocket = socket;
        mSocket.setDataCallback(new SocketDataCallback());
        mSocket.setClosedCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                Log.d(TAG, "onClosedCompleted!");
                isConnected = false;
                mHandler.obtainMessage(EVENT_DISCONNECTED).sendToTarget();
            }
        });

        if (mCommType == COMM_TYPE_CHERUI) {
            JsonAdapter<LoginMsg> jsonAdapter = mMoshi.adapter(LoginMsg.class);
            String toMsg = jsonAdapter.toJson(new LoginMsg(mDeviceid, 8));
            sendMsg(toMsg);
        }

        new LooperThread().start();
    }

    public void sendMsg(String msg) {
        if (isConnected) {
            Util.writeAll(mSocket, msg.getBytes(), new CompletedCallback() {
                @Override
                public void onCompleted(Exception ex) {
                    Log.d(TAG, "write done!");
                }
            });
        }
    }

    private void heartbeat() {
        JsonAdapter<HeartbeatMsg> jsonAdapter = mMoshi.adapter(HeartbeatMsg.class);
        int type = 0;
        if(mCommType == COMM_TYPE_CHEBANG) {
            type = 2;
        } else if(mCommType == COMM_TYPE_CHERUI) {
            type = 0;
        }
        String toMsg = jsonAdapter.toJson(new HeartbeatMsg(mDeviceid, type, mHearbeat++));
        Log.d(TAG, toMsg);
        if (isConnected) {
            Util.writeAll(mSocket, toMsg.getBytes(), new CompletedCallback() {
                @Override
                public void onCompleted(Exception ex) {
                    Log.d(TAG, "write done!");
                }
            });
        }
    }

    public void shutdown() {
        if (isConnected) {
            mSocket.close();
        }
    }

    private class SocketDataCallback implements DataCallback {

        @Override
        public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
            String msg = new String(bb.getAllByteArray());
            Log.d(TAG, "Received Message " + msg);
            mHandler.obtainMessage(EVENT_MSG_UPDATE, msg).sendToTarget();
        }
    }

    private class LooperThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (isConnected) {
                try {
                    Thread.sleep(INTERVAL);
                    heartbeat();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
