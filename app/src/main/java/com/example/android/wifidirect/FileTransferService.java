// Copyright 2011 Google Inc. All Rights Reserved.

package com.example.android.wifidirect;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import android.graphics.Bitmap;
import android.widget.Toast;
public class FileTransferService extends IntentService {
    private static final int SOCKET_TIMEOUT = 15000;
    private Bitmap bmp;
    byte[] combined;
    byte[] single;
    private Socket socket;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_SCREENSHOT = "screenshot";
    public Boolean IS_CHUNCK = false;
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
    public static final String EXTRAS_CHUNCKS_COUNT = "chuncks_count";
    public static final String[] CHUNCK_ARRAY = {"first_chunck", "second_chunck", "third_chunck", "fourth_chunck", "fifth_chunck", "sixth_chunck", "seventh_chunck", "eightth_chunck"};
    public FileTransferService(String name) {
        super(name);
    }
    public FileTransferService() {
        super("FileTransferService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            Toast.makeText(context, "service called", Toast.LENGTH_SHORT).show();
            IS_CHUNCK = intent.getBooleanExtra("is_chunck", false);
            if (!IS_CHUNCK) {
//                Toast.makeText(context,"filetransfer-file size less than 1 mb",Toast.LENGTH_LONG);
                String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
                int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
                single = intent.getByteArrayExtra(EXTRAS_SCREENSHOT);
            } else {
                Toast.makeText(context, "filetransfer-file size greater than 1 mb", Toast.LENGTH_LONG);
                int chunck_size = intent.getIntExtra(EXTRAS_CHUNCKS_COUNT, 0);
                byte[][] output1 = new byte[chunck_size][];

                for (int i = 0; i < chunck_size; i++) {
                    output1[i] = intent.getByteArrayExtra(CHUNCK_ARRAY[i]);
                }
                for (int i = 0; i < output1.length; i++) {
                    combined = new byte[combined.length + output1[i].length];
                }
                for (int i = 0; i < output1.length; i++) {
                    if (i == 0) {
                        System.arraycopy(output1[i], 0, combined, 0, output1[i].length);
                    } else {
                        System.arraycopy(output1[i], 0, combined, output1[i - 1].length, output1[i].length);
                    }
                }
            }
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
            socket = new Socket();
            try {
                Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                if (!IS_CHUNCK) {
                    DeviceDetailFragment.copyFile(single, stream);
                } else {
                    DeviceDetailFragment.copyFile(single, stream);
                }
                Log.d(WiFiDirectActivity.TAG, "Client: Data written");
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, "filetransfer service" + e.getMessage());
                Toast.makeText(context, "filetransfer service" + e.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }
}














//public class FileTransferService extends IntentService {
//
//    private static final int SOCKET_TIMEOUT = 5000;
//    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
//    public static final String EXTRAS_FILE_PATH = "file_url";
//    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
//    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
//    public FileTransferService(String name) {
//        super(name);
//    }
//    public FileTransferService() {
//        super("FileTransferService");
//    }
//    @Override
//    protected void onHandleIntent(Intent intent) {
//        Context context = getApplicationContext();
//        if (intent.getAction().equals(ACTION_SEND_FILE)) {
//            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
//            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
//            Socket socket = new Socket();
//            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
//            try {
//                Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
//                socket.bind(null);
//                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
//                Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
//                OutputStream stream = socket.getOutputStream();
//                ContentResolver cr = context.getContentResolver();
//                InputStream is = null;
//                try {
//                    is = cr.openInputStream(Uri.parse(fileUri));
//                } catch (FileNotFoundException e) {
//                    Log.d(WiFiDirectActivity.TAG, e.toString());
//                }
//                DeviceDetailFragment.copyFile(is, stream);
//                Log.d(WiFiDirectActivity.TAG, "Client: Data written");
//            } catch (IOException e) {
//                Log.e(WiFiDirectActivity.TAG, e.getMessage());
//            } finally {
//                if (socket != null) {
//                    if (socket.isConnected()) {
//                        try {
//                            socket.close();
//                        } catch (IOException e) {
//                            // Give up
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//
//        }
//    }
//}
