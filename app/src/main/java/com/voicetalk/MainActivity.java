package com.voicetalk;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.voicetalk.manager.ClientManager;

import utils.IPUtil;

public class MainActivity extends AppCompatActivity {
    public static final int STOPPED = 0;
    public static final int RECORDING = 1;

    ClientManager clientManager ;

    int status = STOPPED;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    private EditText mEtLocalRtpPort,mEtLocalRtcpPort,mEtDestRtpPort,mEtDestRtcpPort,mEtNetworkAddress;
    private Button mRegister , mReceiveTalk;
    private int mLocalRtpPort,mLocalRtcpPort,mDestRtpPort,mDestRtcpPort;
    private String mNetworkAddress;
    private TextView currentIp;
    private RadioGroup rg;
    private RadioButton rbClient , rbServer;

    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        verifyStoragePermissions(this);

    }

    private void initView() {
        mEtLocalRtpPort = findViewById(R.id.et_local_rtp_port);
        mEtLocalRtcpPort = findViewById(R.id.et_local_rtcp_port);
        mEtDestRtpPort = findViewById(R.id.et_dest_rtp_port);
        mEtDestRtcpPort = findViewById(R.id.et_dest_rtcp_port);
        mEtNetworkAddress = findViewById(R.id.et_network_address);
        mEtLocalRtpPort.setText("8002");
        mEtLocalRtcpPort.setText("8003");
        mEtDestRtpPort.setText("8004");
        mEtDestRtcpPort.setText("8005");
        mEtNetworkAddress.setText("192.168.1.105");
        mEtLocalRtpPort.requestFocus();
        mEtLocalRtcpPort.requestFocus();
        mEtDestRtpPort.requestFocus();
        mEtDestRtcpPort.requestFocus();
        mEtNetworkAddress.requestFocus();
        mRegister = findViewById(R.id.bt_register);
        currentIp =findViewById(R.id.tv_current_ip);
        currentIp.setText(IPUtil.getLocalIPAddress());

        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setClientManager();
            }
        });
    }

    public void startTalk(View view) {

        if(status == STOPPED){
            clientManager.setRecording(true);
            status = RECORDING;
        }
        else if(status == RECORDING){
            clientManager.setRecording(false);
            status = STOPPED;
        }
    }

    private void setClientManager() {
        if("".equals(mEtLocalRtpPort.getText().toString().trim()))

        {
            Toast.makeText(MainActivity.this, "请输入正确的rtp端口号",Toast.LENGTH_LONG).show();
            return;
        }
        if("".equals(mEtLocalRtcpPort.getText().toString().trim()))

        {
            Toast.makeText(MainActivity.this, "请输入正确的rtcp端口号",Toast.LENGTH_LONG).show();
            return;
        }
        if("".equals(mEtDestRtpPort.getText().toString().trim()))

        {
            Toast.makeText(MainActivity.this, "请输入正确的rtp端口号",Toast.LENGTH_LONG).show();
            return;
        }
        if("".equals(mEtDestRtcpPort.getText().toString().trim()))

        {
            Toast.makeText(MainActivity.this, "请输入正确的rtcp端口号",Toast.LENGTH_LONG).show();
            return;
        }
        if("".equals(mEtNetworkAddress.getText().toString().trim()))

        {
            Toast.makeText(MainActivity.this, "请输入正确的ip地址",Toast.LENGTH_LONG).show();
            return;
        }
        mLocalRtpPort = Integer.parseInt(mEtLocalRtpPort.getText().toString());
        mLocalRtcpPort = Integer.parseInt(mEtLocalRtcpPort.getText().toString());
        mDestRtpPort = Integer.parseInt(mEtDestRtpPort.getText().toString());
        mDestRtcpPort = Integer.parseInt(mEtDestRtcpPort.getText().toString());
        mNetworkAddress = mEtNetworkAddress.getText().toString();
        clientManager = new ClientManager(mLocalRtpPort,mLocalRtcpPort,mDestRtpPort,mDestRtcpPort,mNetworkAddress);
//        if(mEtNetworkAddress.getText().toString().trim().equals(IPUtil.getLocalIPAddress())){
//            clientManager.setMode(ClientManager.SERVER);
//        }else {
//            clientManager.setMode(ClientManager.CLIENT);
//        }

        rg = (RadioGroup) findViewById(R.id.rg_mode);
        rbClient = (RadioButton) findViewById(R.id.rb_client);
        rbServer = (RadioButton) findViewById(R.id.rb_server);
        rg.setOnCheckedChangeListener(new MyRadioButtonListener() );//注意是给RadioGroup绑定监视器

        clientManager.setRunning(true);
        Thread sessionThread = new Thread(clientManager);
        sessionThread.start();

    }

    class MyRadioButtonListener implements RadioGroup.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            // 选中状态改变时被触发
            switch (checkedId) {
                case R.id.rb_client:
                    clientManager.setMode(ClientManager.CLIENT);
                    Toast.makeText(MainActivity.this , "CLIENT",Toast.LENGTH_SHORT).show();
                    break;
                case R.id.rb_server:
                    // 当用户选择server端时
                    clientManager.setMode(ClientManager.SERVER);
                    Toast.makeText(MainActivity.this , "SERVER",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    }
