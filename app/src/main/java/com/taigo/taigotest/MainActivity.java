package com.taigo.taigotest;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.data.ScanResult;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;
import com.taigo.taigotest.permissionLibrary.PermissionsManager;
import com.taigo.taigotest.permissionLibrary.PermissionsResultAction;
import com.taigo.taigotest.service.BluetoothService;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BluetoothService mBluetoothService;
    private ScanResult device;
    public Button btnConnect,btnGenerator;
    private BluetoothGattCharacteristic writecharacteristic;
    private TextView txtShow;
    private boolean isConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        if (mBluetoothService == null) {
            bindService();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService();
    }

    public void init() {
        btnConnect= (Button) findViewById(R.id.btnConnect);
        btnGenerator= (Button) findViewById(R.id.btnGenerator);
        txtShow = (TextView) findViewById(R.id.txtShow);
        String[] Permissions=new String[]{Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //权限申请
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this,
                Permissions,
                new PermissionsResultAction() {
                    @Override
                    public void onGranted() {

                    }

                    @Override
                    public void onDenied(String permission) {
                        Toast.makeText(MainActivity.this, "获取权限失败，请点击后允许获取", Toast.LENGTH_SHORT).show();
                    }
                }, true);
    }


    public void bindService() {
        Intent bindIntent = new Intent(this, BluetoothService.class);
        bindIntent.putExtra("HomeFragment","从HomeFragment");
        bindService(bindIntent, mFhrSCon, Context.BIND_AUTO_CREATE);
    }

    public void unbindService() {
        if(mFhrSCon!=null) {
            unbindService(mFhrSCon);
            mFhrSCon=null;
        }
    }

    private ServiceConnection mFhrSCon = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothService = ((BluetoothService.BluetoothBinder) service).getService();
            mBluetoothService.setScanCallback(callback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothService = null;
        }
    };



    private BluetoothService.Callback callback = new BluetoothService.Callback() {

        @Override
        public void onStartScan() {
            btnConnect.setText("连接中...");

        }

        @Override
        public void onScanning(ScanResult result) {
            if(result.getDevice().getName() !=null && (result.getDevice().getName().contains("Taigo"))) {
                device=result;
            }
        }

        @Override
        public void onScanComplete() {
            if(device!=null)
            mBluetoothService.scanAndConnect5(device.getDevice().getAddress());

        }

        @Override
        public void onConnecting() {
        }

        @Override
        public void onConnectFail() {
           // NToast.shortToast(MainActivity.this, "连接失败。");
        }

        @Override
        public void onDisConnected() {
            NToast.shortToast(MainActivity.this, "连接断开。");
            btnConnect.setText("连接Taigo枪");
        }

        @Override
        public void onServicesDiscovered() {
                isConnect=true;
                btnConnect.setText("断开连接");
                btnGenerator.setEnabled(true);
                showData();

        }
    };

    public void ConnectHandler(View view) {
        if(isConnect) {
            mBluetoothService.closeConnect();
            isConnect=false;
        }
        else
            mBluetoothService.scanDevice();
    }

    public void GeneratorHandler(View view) {
        String serviceUuid=mBluetoothService.getService().getUuid().toString();
        String writeUuid=writecharacteristic.getUuid().toString();
        mBluetoothService.write(serviceUuid, writeUuid, "s1", new BleCharacterCallback() {
            @Override
            public void onSuccess(BluetoothGattCharacteristic characteristic) {

            }

            @Override
            public void onFailure(BleException exception) {

            }

            @Override
            public void onInitiatedResult(boolean result) {

            }
        });
    }

    public void showData() {
        BluetoothGatt gatt = mBluetoothService.getGatt();
        for (BluetoothGattService servicess : gatt.getServices()) {
            Log.w(TAG, "================== find service: " + servicess.getUuid().toString());
            if (servicess.getUuid().toString().startsWith("0000ae00")) {//枪发给手机
                mBluetoothService.setService(servicess);
                List<BluetoothGattCharacteristic> characteristics = servicess.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {

                    if (characteristic.getUuid().toString().startsWith("0000ae01"))
                        writecharacteristic=characteristic;

                    Log.w(TAG, "================== find characteristics count: " + characteristics.size());
//                            BluetoothGattCharacteristic characteristic = characteristics.get(0);
                    Log.w(TAG, "================== find characteristic: " + characteristic.getUuid().toString());
                    //Log.w(TAG, "================== characteristic value: " + byte2HexStr(characteristic.getValue()));
                    //gatt.setCharacteristicNotification(characteristic, true);
                    Log.w(TAG, "================== Thread : " + Thread.currentThread().getId());
                    if (characteristic.getUuid().toString().startsWith("0000ae02")) {
                        mBluetoothService.setCharacteristic(characteristic);

                    }
                }
            }
        }

        final BluetoothGattCharacteristic characteristic = mBluetoothService.getCharacteristic();
        mBluetoothService.notify(  characteristic.getService().getUuid().toString(), characteristic.getUuid().toString(), new BleCharacterCallback() {

            @Override
            public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String s = String.valueOf(HexUtil.encodeHex(characteristic.getValue()));
                        //String s10=NumberUtils.print10(s);
                        //NLog.d("BLEBLE",s+"-----");
                        //UnityPlayer.UnitySendMessage("Main Camera","eee",s10);

                        // For all other profiles, writes the data formatted in HEX.对于所有的文件，写入十六进制格式的文件
                        //这里读取到数据
                        final byte[] data = characteristic.getValue();
                        for (int i = 0; i < data.length; i++) {
                            //System.out.println("BLEBLE---原始byte" + (int)data[i]);
                        }
                        if (data != null && data.length > 0) {
                            final StringBuilder stringBuilder = new StringBuilder(data.length);
                            for (byte byteChar : data)
                                //以十六进制的形式输出
                                stringBuilder.append(String.format("%02x ", byteChar));
                            // intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
                            //intent.putExtra(EXTRA_DATA, new String(data));
                            NLog.d("BLEBLE---转换成16进制", stringBuilder.toString());
                        }
                        //Integer.toHexString(10)
                        byte command = data[6];
//                                int aaaaa=command & 0x01;
//                                NLog.d("BLEBLE",aaaaa);


                        NLog.d("BLEBLE---未知", s);
                        String x = s.substring(14, 18);
                        String y = s.substring(18, 22);

                        //NLog.d("BLEBLE>>>>",x+"-"+y);

                        int xx = Integer.parseInt(x, 16);
                        int yy = Integer.parseInt(y, 16);


                        if ((xx > 460 && xx < 530) && (yy > 460 && yy < 530)) {
                            txtShow.setText("重置摇杆");
                        } else {
                            txtShow.setText("摇杆"); return;
                        }


                        if ((int) command == 0){
                            txtShow.setText("松开");}

                        if ((int) command == 1) {
                            txtShow.setText("射击");
                        }

                        if ((int) command == 2)
                            txtShow.setText("上弹");
                        else if ((int) command == 3) {

                        } else if ((int) command == 4) {
                            txtShow.setText("换枪");
                        } else if ((int) command == 8) {
                            txtShow.setText("补血");
                        } else if ((int) command == 10) {
                            txtShow.setText("换枪");
                        } else if ((int) command == 20) {
                            txtShow.setText("无用");
                        }

                    }
                });
            }

            @Override
            public void onFailure(final BleException exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }

            @Override
            public void onInitiatedResult(boolean result) {

            }

        });

    }
}
