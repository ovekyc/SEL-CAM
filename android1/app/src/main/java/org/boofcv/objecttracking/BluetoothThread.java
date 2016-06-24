package org.boofcv.objecttracking;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by chan on 16. 5. 18..
 */
public class BluetoothThread extends Thread {
    private BluetoothSocket mSocket;
    private DataOutputStream out;
    private static String TAG = "BluetoothThread";

    public BluetoothThread(Context context, BluetoothSocket s){
        mSocket = s;
        OutputStream temp = null;
        try {
            temp = mSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "BluetoothThread : " + e.toString());
        }
        if(temp != null)
            out = new DataOutputStream(temp);
    }

    public void stopThread(){
        try {
            out.close();
            mSocket.close();
        }catch(Exception e){
            Log.e(TAG, "stopThread : " + e.toString());
        }
    }

    public void run(int px, int py) {
        super.run();
        try {
            out.writeInt(px);
            out.writeInt(py);
            Log.e("TrackPoint", "(" + px + "," + py + ")");
        } catch(Exception e) {
            Log.e(TAG, "run : " + e.toString());
        }
    }
}
