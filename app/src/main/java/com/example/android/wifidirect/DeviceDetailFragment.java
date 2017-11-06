package com.example.android.wifidirect;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Random;

public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener, SurfaceHolder.Callback {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private static final String TAG = "Device detail fragment";
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    private Bitmap bmp1;
    private Bitmap bmp;
    SurfaceView surfaceView;
    ImageView imageView;
    private Camera mCamera;
    private Bitmap bitmap;
    ProgressDialog progressDialog = null;
    private byte[] byteArray;
    private byte[][] outputBytes1;
    private boolean previewRunning;
    private SurfaceHolder surfaceHolder;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_detail, null);
        surfaceView = (SurfaceView) mContentView.findViewById(R.id.surfaceView);
        imageView = (ImageView) mContentView.findViewById(R.id.imageview1);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true);
                ((DeviceActionListener) getActivity()).connect(config);
            }
        });
        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.call).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        mContentView.findViewById(R.id.status_text).setVisibility(View.GONE);
                        mContentView.findViewById(R.id.group_owner).setVisibility(View.GONE);
                        mCamera.setPreviewCallback(previewCallback);
//                        mCamera.takePicture(null,null,mPicture);


                        // Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                        intent.setType("image/*");
//                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });

        return mContentView;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Toast.makeText(getActivity(), "surface created", Toast.LENGTH_LONG).show();
        try {
            mCamera = Camera.open(1);
            mCamera.setDisplayOrientation(90);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Error", Toast.LENGTH_LONG).show();
            getActivity().finish();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Toast.makeText(getActivity(), "surface chnaged", Toast.LENGTH_LONG).show();
        if (previewRunning) {
            mCamera.stopPreview();
        }
        Camera.Parameters camParams = mCamera.getParameters();
        Camera.Size size = camParams.getSupportedPreviewSizes().get(0);
        camParams.setPreviewSize(size.width, size.height);
        mCamera.setParameters(camParams);
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
//            mCamera.setPreviewCallback(previewCallback);
            previewRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Toast.makeText(getActivity(), "surface destroyed", Toast.LENGTH_SHORT).show();
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text) + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes) : getResources().getString(R.string.no)));
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());
        if (info.groupFormed && info.isGroupOwner) {
            mContentView.findViewById(R.id.call).setVisibility(View.VISIBLE);
            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text), imageView)
                    .execute();
        } else if (info.groupFormed) {
            mContentView.findViewById(R.id.call).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));
            mContentView.findViewById(R.id.device_address).setVisibility(View.GONE);
            mContentView.findViewById(R.id.group_ip).setVisibility(View.GONE);
            mContentView.findViewById(R.id.activity_videocalling).setVisibility(View.VISIBLE);
//            mContentView.findViewById(R.id.status_text).setVisibility(View.GONE);
            mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
            mContentView.findViewById(R.id.status_text).setVisibility(View.GONE);
            mContentView.findViewById(R.id.device_info).setVisibility(View.GONE);
            mContentView.findViewById(R.id.call).setVisibility(View.VISIBLE);
//            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text),imageView)
//                    .execute();
        }
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());
    }

    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.call).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {
        private Context context;
        private TextView statusText;
        private ImageView imageView;

        public FileServerAsyncTask(Context context, View statusText, ImageView imageView) {
            this.context = context;
            this.statusText = (TextView) statusText;
            this.imageView = imageView;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis() + ".jpg");
                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();
                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFileServer(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                statusText.setText("File copied - " + result);
                Bitmap bmp = loadBitmap(Uri.parse("file://" + result).toString());
                Toast.makeText(context, "async bitmap size" + bmp, Toast.LENGTH_SHORT).show();
                imageView.setImageBitmap(bmp);
//                Intent intent = new Intent();
//                intent.setAction(android.content.Intent.ACTION_VIEW);
//                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
//                context.startActivity(intent);
            }
        }

        public Bitmap loadBitmap(String url) {
            Bitmap bm = null;
            InputStream is = null;
            BufferedInputStream bis = null;
            try {
                URLConnection conn = new URL(url).openConnection();
                conn.connect();
                is = conn.getInputStream();
                bis = new BufferedInputStream(is, 8192);
                bm = BitmapFactory.decodeStream(bis);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return bm;
        }

        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
        }
    }

    public static boolean copyFile(byte[] byteArray, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        long startTime = System.currentTimeMillis();
        InputStream inputStream = new ByteArrayInputStream(byteArray);
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
            long endTime = System.currentTimeMillis() - startTime;
            Log.v("", "Time taken to transfer all bytes is : " + endTime);
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            if (MyPrefs.getInstance(getActivity()).isPreview()) {
                MyPrefs.getInstance(getActivity()).setPreCallBack(false);
                Camera.Size previewSize = cam.getParameters().getPreviewSize();
                YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 80, baos);
                byte[] jdata = baos.toByteArray();
                bitmap = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);
                try {
                    imageView.setVisibility(View.VISIBLE);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(270);
                    Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//                  imageview1.setImageBitmap(rotatedBitmap);
                    sendScreenshotToService(rotatedBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public static boolean copyFileServer(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        long startTime = System.currentTimeMillis();
//        InputStream inputStream = new ByteArrayInputStream(byteArray);
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
            long endTime = System.currentTimeMillis() - startTime;
            Log.v("", "Time taken to transfer all bytes is : " + endTime);
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

    public void sendScreenshotToService(Bitmap rotatedBitmap) throws Exception {
        Toast.makeText(getActivity(), "send screenshot to service called", Toast.LENGTH_SHORT).show();
        byte[] byteArray = compressImage(rotatedBitmap);
        System.out.println("byte array length" + byteArray.length);
        if (byteArray.length > 1000000) {
            int chuncksize = (byteArray.length) / 500;
            splitBytes(byteArray, chuncksize);
            Toast.makeText(getActivity(), "file size is greater than 1 mb", Toast.LENGTH_SHORT).show();
        } else {
            Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_SCREENSHOT, byteArray);
            serviceIntent.putExtra("is_chunck", false);
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, info.groupOwnerAddress.getHostAddress());
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
            getActivity().startService(serviceIntent);
        }
    }
    private byte[] compressImage(Bitmap rotatedBitmap) {

        System.out.println("bmp size" + rotatedBitmap.getByteCount());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byteArray = stream.toByteArray();
        System.out.println("byte size" + byteArray.length);
        return byteArray;
    }
    public void splitBytes(byte[] fileBytes, int chuncksize) throws Exception {
        int offset = 0;
        while (offset < fileBytes.length) {
            byte[] outputBytes;
            if (fileBytes.length - offset < 500) {
                outputBytes = new byte[fileBytes.length - offset];
                System.arraycopy(fileBytes, offset, outputBytes, 0, fileBytes.length - offset);
                saveFile(outputBytes, chuncksize);
                break;
            } else {
                outputBytes = new byte[500];
                System.arraycopy(fileBytes, offset, outputBytes, 0, 500);
                offset += 500;
                saveFile(outputBytes, chuncksize);
                Thread.sleep(250);
            }
        }
    }

    private void saveFile(byte[] outputBytes, int chuncksize) {
        int length = outputBytes1.length;
        outputBytes1 = new byte[chuncksize][];
        for (int i = 0; i < chuncksize; i++) {
            outputBytes1[i] = new byte[500];
            outputBytes1[i] = Arrays.copyOf(outputBytes, outputBytes.length);
        }
        sendBytestoService(chuncksize);
    }

    private void sendBytestoService(int chuncksize) {
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_CHUNCKS_COUNT, chuncksize);
        for (int i = 0; i < chuncksize; i++) {
            serviceIntent.putExtra(FileTransferService.CHUNCK_ARRAY[i], outputBytes1[i]);
        }
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        getActivity().startService(serviceIntent);
    }


}
