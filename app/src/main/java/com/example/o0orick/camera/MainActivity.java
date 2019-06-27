package com.example.o0orick.camera;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.widget.CameraViewInterface;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import io.socket.client.Socket;

public final class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent {
    private static final boolean DEBUG = true;	// TODO set false on release
    private static final String TAG = "MainActivity";

    /**
     * 操作锁
     */
	private final Object mSync = new Object();

    /**
     * set true if you want to record movie using MediaSurfaceEncoder
     * (writing frame data into Surface camera from MediaCodec
     *  by almost same way as USBCameratest2)
     * set false if you want to record movie using MediaVideoEncoder
     */
    private static final boolean USE_SURFACE_ENCODER = false;

    /**
     * preview resolution(width)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_WIDTH = 640; // 640
    /**
     * preview resolution(height)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_HEIGHT = 480; //480
    /**
     * preview mode
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     * 0:YUYV, other:MJPEG
     */
    private static final int PREVIEW_MODE = 0; // YUV

    protected static final int SETTINGS_HIDE_DELAY_MS = 2500;

    /**
     * for accessing USB
     */
    private USBMonitor mUSBMonitor;
    /**
     * Handler to execute camera related methods sequentially on private thread
     */
    private UVCCameraHandler mCameraHandler;
    /**
     * for camera preview display
     */
    private CameraViewInterface mUVCCameraView;
    /**
     * for open&start / stop&close camera preview
     */
    private ImageButton mCameraButton;
    private ImageView mUsbImageView;
    private boolean isScaling = false;
    private boolean isInCapturing = false;

    private int[][] capture_solution = {{640,480}, {800,600},{1024,768}, {1280,1024}};
    private int mCaptureWidth = capture_solution[0][0];
    private int mCaptureHeight = capture_solution[0][1];

    private Socket mSocket;
    private String ChatUser;
    private SocketIOManager app;

    @Override
    protected void onResume() {
        super.onResume();

    }


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate:");

        onResume();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        mCameraButton = (ImageButton) findViewById(R.id.imageButton);
        mCameraButton.setOnClickListener(mOnClickListener);

        mCaptureWidth = capture_solution[0][0];
        mCaptureHeight = capture_solution[0][1];
        bitmap = Bitmap.createBitmap(mCaptureWidth, mCaptureHeight, Bitmap.Config.RGB_565);

        final View view = findViewById(R.id.camera_view);
        mUVCCameraView = (CameraViewInterface)view;
        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);

        mUsbImageView = (ImageView) findViewById(R.id.usb_imageView);

        app = (SocketIOManager) getApplication();
        mSocket = app.getSocket();

		synchronized (mSync) {
	        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
	        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
	                USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);
		}

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onStart() {
        super.onStart();

		synchronized (mSync) {
        	mUSBMonitor.register();
		}
		if (mUVCCameraView != null) {
  			mUVCCameraView.onResume();
		}
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop:");
        synchronized (mSync) {
    		mCameraHandler.close();	// #close include #stopRecording and #stopPreview
			mUSBMonitor.unregister();
        }
		 if (mUVCCameraView != null)
			mUVCCameraView.onPause();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy:");
        synchronized (mSync) {
            if (mCameraHandler != null) {
                mCameraHandler.setPreviewCallback(null); //zhf
                mCameraHandler.release();
                mCameraHandler = null;
            }
            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }
        }
        app.setChatUser("");
        super.onDestroy();
    }

    /**
     * event handler when click camera / capture button
     */
    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            synchronized (mSync) {
                if ((mCameraHandler != null) && !mCameraHandler.isOpened()) {
                    CameraDialog.showDialog(MainActivity.this);
                } else {
                    mCameraHandler.close();
                }
            }
        }
    };

    private void startPreview() {
		synchronized (mSync) {
			if (mCameraHandler != null) {
                final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
                /**
                 * 由于surfaceview由另一个线程处理，这里使用消息处理机制
                 * 对Frame进行回调处理
                 */
				mCameraHandler.setPreviewCallback(mIFrameCallback);
                mCameraHandler.startPreview(new Surface(st));
			}
		}
        updateItems();
    }

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            if (DEBUG) Log.v(TAG, "onConnect:");
            synchronized (mSync) {
                if (mCameraHandler != null) {
	                mCameraHandler.open(ctrlBlock);
	                startPreview();
	                updateItems();
				}
            }
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect:");
            synchronized (mSync) {
                if (mCameraHandler != null) {
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                // maybe throw java.lang.IllegalStateException: already released
                                mCameraHandler.setPreviewCallback(null); //zhf
                            }
                            catch(Exception e){
                                e.printStackTrace();
                            }
                            mCameraHandler.close();
                        }
                    }, 0);
				}
            }
        }
        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

    /**
     * to access from CameraDialog
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
		synchronized (mSync) {
			return mUSBMonitor;
		}
	}

    @Override
    public void onDialogResult(boolean canceled) {
        if (DEBUG) Log.v(TAG, "onDialogResult:canceled=" + canceled);
    }

    //================================================================================
    private boolean isActive() {
        return mCameraHandler != null && mCameraHandler.isOpened();
    }

    private boolean checkSupportFlag(final int flag) {
        return mCameraHandler != null && mCameraHandler.checkSupportFlag(flag);
    }

    private int getValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.getValue(flag) : 0;
    }

    private int setValue(final int flag, final int value) {
        return mCameraHandler != null ? mCameraHandler.setValue(flag, value) : 0;
    }

    private int resetValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.resetValue(flag) : 0;
    }

    /**
     * 利用Activity.runOnUiThread(Runnable)把更新ui的代码创建在Runnable中，
     * 然后在需要更新ui时，把这个Runnable对象传给Activity.runOnUiThread(Runnable)
     */
    private void updateItems() {
        runOnUiThread(mUpdateItemsOnUITask, 30);
    }

    private final Runnable mUpdateItemsOnUITask = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) return;
            final int visible_active = isActive() ? View.VISIBLE : View.INVISIBLE;
        }
    };

    // if you need frame data as byte array on Java side, you can use this callback method with UVCCamera#setFrameCallback
    // if you need to create Bitmap in IFrameCallback, please refer following snippet.
    private Bitmap bitmap = null;//Bitmap.createBitmap(640, 480, Bitmap.Config.RGB_565);
    private final Bitmap srcBitmap = Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.RGB_565);
    private String WarnText;

    public static Bitmap getTransformBitmap(Bitmap src) {
        int width = src.getWidth();         // 宽
        int height = src.getHeight();       // 高
        int count = src.getByteCount();     // 获取图片的RGB 565颜色数组总数
        ByteBuffer buffer = ByteBuffer.allocate(count);
        src.copyPixelsToBuffer(buffer);
        byte[] data = buffer.array();       //获取数组
        int sum = width * height;
        int[] pixels = new int[sum];

        for (int i = 0; i < sum; i++) {
            int tmpint = data[i * 2 + 0] + data[i * 2 + 1] * 256;
            int a = 0xff;                           //透明度
            int r = (tmpint & 0xf800) >> 11;        //红
            int g = (tmpint & 0x07e0) >> 5;         //绿
            int b = (tmpint & 0x001f);              //蓝

            r = r << 3;
            g = g << 2;
            b = b << 3;
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | (b);
        }
        Bitmap bmp = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        bmp.setPixels(pixels, 0, width, 0, 0, width, height);
        return bmp;    //旋转屏幕
    }

    private void sendBitmap(Bitmap it){

        ByteArrayOutputStream buf;
        buf = new ByteArrayOutputStream();
        it.compress(Bitmap.CompressFormat.JPEG, 20, buf);

        mSocket.emit("image", buf.toByteArray());

    }

    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            frame.clear();
            if(!isActive() || isInCapturing){
                return;
            }
            if(bitmap == null){
                Toast.makeText(MainActivity.this, "错误：Bitmap为空", Toast.LENGTH_SHORT).show();
                return;
            }
            /**
             * 这里进行opencv操作
             * srcBitmap:源
             * bitmap:处理后
             * 转发bitmap数据
             */
            synchronized (bitmap) {
                srcBitmap.copyPixelsFromBuffer(frame);
                WarnText = "";

                if(bitmap.getWidth() != mCaptureWidth || bitmap.getHeight() != mCaptureHeight){
                    bitmap = Bitmap.createBitmap(mCaptureWidth, mCaptureHeight, Bitmap.Config.RGB_565);
                }

                sendBitmap(srcBitmap);
            }
            //mImageView.post(mUpdateImageTask);
        }
    };

    private final Runnable mUpdateImageTask = new Runnable() {
        @Override
        public void run() {
            synchronized (bitmap) {
                //mImageView.setImageBitmap(bitmap);

            }
        }
    };


}