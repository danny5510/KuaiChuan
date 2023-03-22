package com.sharemedia.kuaichuan;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Choreographer;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

class CameraCoreManager {
    private static final String TAG = "CameraDemo";

    private Context mContext;
    private CameraManager mCameraManager;
    private String mCameraId;
    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private ImageReader mImageReader;
    private CameraDevice mCameraDevice;
    private CameraCharacteristics mCameraCharacteristics;
    private MediaRecorder mMediaRecorder;

    //Max preview width&height that is guaranteed by Camera2 API
    private static final int MAX_PREVIEW_WIDTH = 2268;
    private static final int MAX_PREVIEW_HEIGHT = 4032;

    //A Semaphore to prevent the app from exiting before closing the camera.
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private Size mPreviewSize = new Size(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT);
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mCaptureSession;
    private int mFacing = CameraCharacteristics.LENS_FACING_BACK;
    private Choreographer.FrameCallback mFrameCallback;
    private SurfaceTexture mSurfaceTexture;
    private File mCameraFile;

    private enum State{
        STATE_PREVIEW,
        STATE_CAPTURE,
        STATE_RECORD
    }
    State mState = State.STATE_PREVIEW;

    //camera capture process，step3 创建ImageReader并设置mImageAvailableListener，实现如下：
    private ImageReader.OnImageAvailableListener mImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            if(mState == State.STATE_PREVIEW){
                //Log.d(TAG, "##### onFrame: Preview");
                Image image = reader.acquireNextImage();
                image.close();
            }else if(mState == State.STATE_CAPTURE) {
                Log.d(TAG,"capture one picture to gallery");
                mCameraFile = new File("aa_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg");
                mCameraHandler.post(new ImageSaver(mContext, reader.acquireLatestImage(), mCameraFile));
                mState = State.STATE_PREVIEW;
            }else if(mState == State.STATE_RECORD) {
                Log.d(TAG,"record video");

            }else{
                Log.d(TAG, "##### onFrame: default/nothing");
            }
        }
    };

    //camera preview process，step2 mStateCallback 实例化
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //重写onOpened方法，最为关键
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            startCaptureSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenCloseLock.release();
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e("DEBUG", "onError: " + error);
            mCameraOpenCloseLock.release();
            camera.close();
            mCameraDevice = null;
            Log.e("DEBUG", "onError:  restart camera");
            stopPreview();
            startPreview();
        }
    };

    CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session,CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };

    public CameraCoreManager(Context context) {
        mContext = context;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mMediaRecorder = new MediaRecorder();
        mState = State.STATE_PREVIEW;
    }

    public void startPreview() {
        if (!chooseCameraIdByFacing()) {
            Log.e(TAG, "Choose camera failed.");
            return;
        }

        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        if (mImageReader == null) {
            mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 2);
            mImageReader.setOnImageAvailableListener(mImageAvailableListener, mCameraHandler);
        }else{
            mImageReader.close();
        }
        openCamera();
    }

    public void stopPreview() {
        closeCamera();
        if (mCameraThread != null) {
            mCameraThread.quitSafely();
            mCameraThread = null;
        }
        mCameraHandler = null;
    }

    private boolean chooseCameraIdByFacing() {
        try {
            String ids[] = mCameraManager.getCameraIdList();
            if (ids.length == 0) {
                Log.e(TAG, "No available camera.");
                return false;
            }

            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    continue;
                }

                Integer internal = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (internal == null) {
                    continue;
                }
                if (internal == mFacing) {
                    mCameraId = cameraId;
                    mCameraCharacteristics = characteristics;
                    return true;
                }
            }

            mCameraId = ids[1];
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            Integer level = mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                return false;
            }

            Integer internal = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (internal == null) {
                return false;
            }
            mFacing = CameraCharacteristics.LENS_FACING_BACK;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public void openCamera() {
        if (TextUtils.isEmpty(mCameraId)) {
            Log.e(TAG, "Open camera failed. No camera available");
            return;
        }

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            //camera preview process，step1 打开camera
            mCameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);
        } catch (InterruptedException | CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void startCaptureSession() {
        mState = State.STATE_PREVIEW;
        if (mCameraDevice == null) {
            return;
        }

        if ((mImageReader != null || mSurfaceTexture != null)) {
            try {
                //camera preview process，step3 创建一个 CaptureRequest.Builder，templateType来区分是拍照还是预览
                mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                //camera preview process，step4 将显示预览用的surface的实例传入，即将显示预览用的 surface 的实例，作为一个显示层添加到该 请求的目标列表中
                mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
                List<Surface> surfaceList = Arrays.asList(mImageReader.getSurface());
                if (mSurfaceTexture != null) {
                    mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                    Surface surface = new Surface(mSurfaceTexture);
                    mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
                    mPreviewRequestBuilder.addTarget(surface);
                    //camera preview process，step5 将显示预览用的surface的实例传入，即将显示预览用的surface的实例，作为一个显示层添加到该请求的目标列表中
                    surfaceList = Arrays.asList(surface, mImageReader.getSurface());
                }

                Range<Integer>[] fpsRanges = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                Log.d("DEBUG", "##### fpsRange: " + Arrays.toString(fpsRanges));
                //camera preview process，step6 & 7
                // 6 执行createCaptureSession方法
                // 7 参数中实例化 CameraCaptureSession.stateCallback，并重写 onConfigured 方法
                mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        if (mCameraDevice == null) return;
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                        mCaptureSession = session;
                        try {
                            if (mCaptureSession != null)
                                //camera preview process，step8 用 CameraCaptureSession.setRepeatingRequest()方法创建预览
                                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mCameraHandler);
                        } catch (CameraAccessException | IllegalArgumentException | IllegalStateException | NullPointerException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {
                        Log.e(TAG, "Failed to configure capture session");
                    }

                    @Override
                    public void onClosed(CameraCaptureSession session) {
                        if (mCaptureSession != null && mCaptureSession.equals(session)) {
                            mCaptureSession = null;
                        }
                    }
                }, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            } catch (IllegalStateException e) {
                stopPreview();
                startPreview();
            } catch (UnsupportedOperationException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public void captureStillPicture() {
        try {
            Log.d(TAG,"captureStillPicture");
            mState = State.STATE_CAPTURE;
            if (mCameraDevice == null) {
                return;
            }
            // camera capture process，step1 创建作为拍照的CaptureRequest.Builder
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // camera capture process，step2 将imageReader的surface作为CaptureRequest.Builder的目标
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            // 设置自动对焦模式
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 设置自动曝光模式
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 设置为自动模式
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // 设置摄像头旋转角度
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, Surface.ROTATION_0);
            // 停止连续取景
            mCaptureSession.stopRepeating();
            // camera capture process，step5 &6 捕获静态图像，结束后执行onCaptureCompleted
            mCaptureSession.capture(mCaptureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override// 拍照完成时激发该方法
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    Log.d(TAG,"onCaptureCompleted");
                    startCaptureSession();
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //camera capture progress，step4 保存图片相关操作
    public static class ImageSaver implements Runnable {
        private final Image mImage;
        private final File mFile;
        Context mContext;

        ImageSaver(Context context,Image image, File file) {
            mContext = context;
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            Log.d(TAG,"take picture Image Run");
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DESCRIPTION, "This is an qr image");
            values.put(MediaStore.Images.Media.DISPLAY_NAME, mFile.getName());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.TITLE, "Image.jpg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/");
            Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            ContentResolver resolver = mContext.getContentResolver();
            Uri insertUri = resolver.insert(external, values);
            OutputStream os = null;
            try {
                if (insertUri != null) {
                    os = resolver.openOutputStream(insertUri);
                }
                if (os != null) {
                    os.write(bytes);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                try {
                    if(os!=null) {
                        os.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public interface FrameCallback {
        void onFrame(Image data);
    }

    public Size getPreviewSize() {
        return mPreviewSize;
    }

    public void setPreviewSize(Size previewSize) {
        mPreviewSize = previewSize;
    }

    public FrameCallback getFrameCallback() {
        return (FrameCallback) mFrameCallback;
    }

    public void setFrameCallback(FrameCallback frameCallback) {
        mFrameCallback = (Choreographer.FrameCallback) frameCallback;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture;
    }
}