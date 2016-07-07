package com.jay.smartvehicle.comm;

/**
 * Created by H151136 on 7/7/2016.
 */
public class LoginMsg {
    public String deviceid;
    public int msg_type;

    public LoginMsg(String id, int type) {
        this.deviceid = id;
        this.msg_type = type;
    }
}
