package com.jay.smartvehicle.comm;

/**
 * Created by H151136 on 7/7/2016.
 */
public class HeartbeatMsg {
    public int msg_type;
    public String deviceid;
    public int heartbeat_count;

    public HeartbeatMsg(String deviceid, int msg_type, int count) {
        this.deviceid = deviceid;
        this.msg_type = msg_type;
        this.heartbeat_count = count;
    }
}
