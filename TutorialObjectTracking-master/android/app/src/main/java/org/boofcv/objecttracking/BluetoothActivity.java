package org.boofcv.objecttracking;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends Activity {
    private static final int REQUEST_ENABLE_BT = 2;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> devices;
    //private BluetoothSocket mSocket;
    private BroadcastReceiver bluetoothReceiver;
    private ProgressDialog progressDialog;
    private Button setbtn;
    private boolean isConnect;
    private MySocket mSocket;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        mSocket = ((MySocket) getApplicationContext());
        isConnect = false;
        setbtn = (Button) findViewById(R.id.button);
        setbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBlueTooth();
            }
        });

        bluetoothReceiver = new BroadcastReceiver() {
            int deviceCount;

            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    //discovery starts, we can show progress dialog or perform other tasks
                    deviceCount = 0;
                    progressDialog = new ProgressDialog(BluetoothActivity.this);
                    progressDialog.setMessage("Scanning..");
                    progressDialog.show();
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    //discovery finishes, dismis progress dialog
                    progressDialog.dismiss();
                    if(deviceCount == 0)
                        Toast.makeText(BluetoothActivity.this, "No device", Toast.LENGTH_SHORT).show();
                    else {

                    }
/*
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Select Bluetooth Device");

                    builder.setTitle("Select Bluetooth Device");


                    // 페어링 된 블루투스 장치의 이름 목록 작성
                    List<String> listItems = new ArrayList<String>();
                    for(BluetoothDevice device : devices)
                        listItems.add(device.getName());
                    listItems.add("Cancel");    // 취소 항목 추가

                    final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(i == deviceCount) {
                                // 연결할 장치를 선택하지 않고 '취소'를 누른 경우
                                finish();
                            }
                            else {
                                // 연결할 장치를 선택한 경우
                                // 선택한 장치와 연결을 시도
                                connectToSelectedDevice(items[i].toString());
                            }
                        }
                    });

                    builder.setCancelable(false);    // 뒤로 가기 버튼 사용 금지
                    AlertDialog alert = builder.create();
                    alert.show();
*/

                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    //bluetooth device found
                    deviceCount++;
                    BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Toast.makeText(BluetoothActivity.this, "Found device : "+device.getName(), Toast.LENGTH_SHORT).show();
                }
            }

        };
    }


    void checkBlueTooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
            Toast.makeText(BluetoothActivity.this, "This device don't support a Bluetooth", Toast.LENGTH_SHORT).show();
            return;   // 어플리케이션 종료
        }

        else {
            // 장치가 블루투스 지원하는 경우
            if(!bluetoothAdapter.isEnabled()) {
                // 블루투스를 지원하지만 비활성 상태인 경우
                // 블루투스를 활성 상태로 바꾸기 위해 사용자 동의 요첨
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            else {
                // 블루투스를 지원하며 활성 상태인 경우
                // 페어링된 기기 목록을 보여주고 연결할 장치를 선택.
                selectDevice();

            }
        }
    }

    void selectDevice() {
        devices = bluetoothAdapter.getBondedDevices();
        final int deviceCount = devices.size();

        if(deviceCount == 0 ) {
//            IntentFilter filter = new IntentFilter();
//            filter.addAction(BluetoothDevice.ACTION_FOUND);
//            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//
//            registerReceiver(bluetoothReceiver, filter);
//            bluetoothAdapter.startDiscovery();
//리시버등록해제

            //  페어링 된 장치가 없는 경우
            Toast.makeText(BluetoothActivity.this, "There is no device to connect", Toast.LENGTH_SHORT).show();
            return;


        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Bluetooth Device");


        // 페어링 된 블루투스 장치의 이름 목록 작성
        List<String> listItems = new ArrayList<String>();
        for(BluetoothDevice device : devices)
            listItems.add(device.getName());
        listItems.add("Cancel");    // 취소 항목 추가

        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(i == deviceCount) {
                    // 연결할 장치를 선택하지 않고 '취소'를 누른 경우
                    return;
                }
                else {
                    // 연결할 장치를 선택한 경우
                    // 선택한 장치와 연결을 시도
                    connectToSelectedDevice(items[i].toString());

                    if(isConnect == true) {
                        Intent intent = new Intent(BluetoothActivity.this, ObjectTrackerActivity.class);
                        startActivity(intent);
                    }
                    return;
                }
            }
        });

        builder.setCancelable(false);    // 뒤로 가기 버튼 사용 금지
        AlertDialog alert = builder.create();
        alert.show();


    }


    BluetoothDevice getDeviceFromBondedList(String name) {
        BluetoothDevice selectedDevice = null;

        for(BluetoothDevice device : devices) {
            if(name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }

    void connectToSelectedDevice(String selectedDeviceName) {
        BluetoothDevice target = getDeviceFromBondedList(selectedDeviceName);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        try {
            // 소켓 생성
            mSocket.setSocket(target.createRfcommSocketToServiceRecord(uuid));
            // RFCOMM 채널을 통한 연결
            mSocket.getSocket().connect();

            isConnect = true;

        }catch(Exception e) {
            // 블루투스 연결 중 오류 발생
            Toast.makeText(BluetoothActivity.this, "Connect Fail", Toast.LENGTH_SHORT).show();
            Log.e("BluetoothActivity", "connectToSelectDevice : " + e.toString());
            return;   // 어플 종료
        }
    }



    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK) {
                    // 블루투스가 활성 상태로 변경됨
                    selectDevice();
                }

                else if(resultCode == RESULT_CANCELED) {
                    Toast.makeText(BluetoothActivity.this, "Please turn on Bluetooth", Toast.LENGTH_SHORT).show();
                    finish();  //  어플리케이션 종료
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
         super.onResume();
         mSocket = ((MySocket) getApplicationContext());
    }

}
