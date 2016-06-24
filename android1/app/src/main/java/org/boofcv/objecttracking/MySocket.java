package org.boofcv.objecttracking;

import android.app.Application;
import android.bluetooth.BluetoothSocket;

/**
 * Created by chan on 16. 5. 18..
 */
public class MySocket extends Application{
    public BluetoothSocket mSocket;
    public BluetoothSocket getSocket(){
        return mSocket;
    }
    public void setSocket(BluetoothSocket s){
        mSocket = s;
    }
}
