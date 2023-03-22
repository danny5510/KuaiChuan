package com.sharemedia.kuaichuan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.ui.AppBarConfiguration;

import com.sharemedia.kuaichuan.databinding.ActivityCamera2Binding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraActivity2 extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityCamera2Binding binding;
    private static final String TAG = "预览";
    private static final SparseIntArray ORIENTATION = new SparseIntArray();
    static {
        ORIENTATION.append(Surface.ROTATION_0,90);
        ORIENTATION.append(Surface.ROTATION_90,0);
        ORIENTATION.append(Surface.ROTATION_180,270);
        ORIENTATION.append(Surface.ROTATION_270,180);
    }
    private String mCameraId;         // 摄像头Id
    private Size mPreviewSize;      //获取分辨率
    private Size mCameraSize;//镜头最大分辨率
    private ImageReader mImageReader;  //图片阅读器
    private CameraDevice mCameraDevice;   //摄像头设备
    private CameraCaptureSession mCaptureSession;   //获取会话
    private CaptureRequest mPreviewRequest;      //获取请求
    private CaptureRequest.Builder mPreviewRequestBuilder;   //创建获取请求
    private TextureView textureView;      //预览视图
    private Surface mPreviewSurface;      //
    private String[] permissions = {Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };
    private List<String> permissionList = new ArrayList();
    // 第一步：获取权限
    /**
     * 获取拍照和读写权限
     */
    private void getPermission() {
        //Log.i(TAG, " getPermission");
        if (permissionList != null) {
            permissionList.clear();
        }
        //版本判断 当手机系统大于23时,才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //权限是否已经 授权 GRANTED-授权  DINIED-拒绝
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(permission);
                }
            }
            if (!permissionList.isEmpty()) {
                ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), 1000);
            } else {
                //表示全都授权了
                textureView.setSurfaceTextureListener(textureListener);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] mPermissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            //权限请求失败
            if (grantResults.length > 0) {
                //存放没授权的权限
                List<String> deniedPermissions = new ArrayList<>();
                for (int i = 0; i < grantResults.length; i++) {
                    int grantResult = grantResults[i];
                    String permission = permissions[i];
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        deniedPermissions.add(permission);
                    }
                }
                if (deniedPermissions.isEmpty()) {
                    //说明都授权了
                    openCamera();
                } else {
                    getPermission();
                }
            }
        }
    }

    // 1.  SurfaceView状态回调
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            setupCamera(width,height);
            configureTransform(width,height);   //使配置改变
            openCamera();             //打开相机
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
            configureTransform(width,height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
        }
    };

    // 2.  摄像头状态回调
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        //打开成功获取到camera设备
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            //开启预览
            startPreview();
        }
        //打开失败
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Toast.makeText(CameraActivity2.this, "摄像头设备连接失败", Toast.LENGTH_SHORT).show();
        }
        //打开错误
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Toast.makeText(CameraActivity2.this, "摄像头设备连接出错", Toast.LENGTH_SHORT).show();
        }
    };

    //设置摄像机
    private void setupCamera(int width,int height){
        //获取摄像头的管理者CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //遍历所有摄像头
            for(String cameraId : manager.getCameraIdList()){
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId); //获取摄像机的特征
                //默认打开后置  - 忽略前置 LENS（镜头）
                if(characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)  //如果是前置跳过
                {
                    continue;
                }
                //获取StreamConfigurationMap,他是管理摄像头支持的所有输出格式
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class),width,height); //获取最佳的预览大小
                int orientation = getResources().getConfiguration().orientation;
                if(orientation== Configuration.ORIENTATION_LANDSCAPE){
                    textureView.setSurfaceTextureListener(textureListener);
                }else {
                    textureView.setSurfaceTextureListener(textureListener);
                }
                mCameraId = cameraId;
                mCameraSize = map.getOutputSizes(ImageFormat.JPEG)[0];

                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(){
        //获取摄像头的管理者 CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //检查权限
        try{
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
                return;
            }else{
                manager.openCamera(mCameraId,stateCallback,null);
            }
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }
    //  关闭相机
    private void closeCamera(){
        if(mCaptureSession != null){
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if(mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice=null;
        }
    }

    private void startPreview(){
        setupImageReader();
        SurfaceTexture mSurfaceTexture = textureView.getSurfaceTexture();
        //设置TextureView的缓冲区大小
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
        //获取Surface显示预览数据
        mPreviewSurface = new Surface(mSurfaceTexture);
        try {
            getPreviewRequestBuilder();
            //创建相机捕捉会话,第一个参数是捕获数据的输出Surface列表,第二个参数是CameraCaptureSession的状态回调接口,当他创建好后会回调onCconfigured方法
            //第三个参数用来确定Callback在那个线程执行,null表示在当前线程执行
            mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCaptureSession = cameraCaptureSession;
                    repeatPreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                }
            }, null);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        // 一旦捕获完成
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };
    private void repeatPreview(){
        mPreviewRequestBuilder.setTag(TAG);
        mPreviewRequest = mPreviewRequestBuilder.build();
        //设置反复捕获数据的请求,这样预览界面就会一直有数据显示
        try {
            mCaptureSession.setRepeatingRequest(mPreviewRequest,mPreviewCaptureCallback,null);
        }catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void setupImageReader(){
        //前三个参数分别是需要的尺寸和格式,最后一个参数代表每次最多获取几帧数据
        //mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(),mPreviewSize.getHeight(), ImageFormat.JPEG,1);

        mImageReader = ImageReader.newInstance(mCameraSize.getWidth(),mCameraSize.getHeight(), ImageFormat.JPEG,1);
        //监听ImageReader的事件,当有图像流数据可用时会回调onImageAvailable
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Toast.makeText(CameraActivity2.this,"图片已保存",Toast.LENGTH_SHORT).show();
                Image image = imageReader.acquireNextImage();
                //开启线程一部保存图片
                new Thread(new ImageSaver(image)).start();
            }
        },null);
    }
    //选择sizeMap中大于并且接近width和height的size
    private Size getOptimalSize(Size[] sizeMap, int width, int height){
        List<Size> sizeList = new ArrayList<>();
        for(Size option:sizeMap){
            if(width>height){
                if(option.getWidth()>width && option.getHeight()>height){
                    sizeList.add(option);
                }
            }else {
                if(option.getWidth()>height&&option.getHeight()>width){
                    sizeList.add(option);
                }
            }
        }
        if(sizeList.size()>0){
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size size, Size t1) {
                    return Long.signum(size.getWidth()*size.getHeight()- t1.getWidth()*t1.getHeight());
                }
            });
        }
        return sizeMap[0];
    }
    //使配置转换
    private void configureTransform(int viewWidth,int viewHeight){
        if(textureView == null || mPreviewSize == null){
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0,0,viewWidth,viewHeight);
        RectF bufferRect = new RectF(0,0,mPreviewSize.getHeight(),mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_90){
            bufferRect.offset(centerX - bufferRect.centerX(),centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect,bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) viewHeight/mPreviewSize.getHeight(),
                    (float) viewWidth/mPreviewSize.getWidth());
            matrix.postScale(scale,scale,centerX,centerY);
            matrix.postRotate(90*(rotation-2),centerX,centerY);
        }else if(rotation == Surface.ROTATION_180){
            matrix.postRotate(180,centerX,centerY);
        }
        textureView.setTransform(matrix);
    }

    //创建预览请求的Builder （TEMPLATE_PREVIEW表示预览请求）
    private void getPreviewRequestBuilder(){
        try {
            mPreviewRequestBuilder =mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
        //设置预览的显示图
        mPreviewRequestBuilder.addTarget(mPreviewSurface);
        MeteringRectangle[] meteringRectangles = mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AE_REGIONS);
        if(meteringRectangles != null && meteringRectangles.length > 0){
            //Toast.makeText(CameraActivity2.this,"PreviewRequestBuilder: AF_REGIONS=",Toast.LENGTH_SHORT).show();
        }
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
    }

    //  拍照
    private void takePhoto(){
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId); //获取摄像机的特征
            Range<Long> range = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            Range<Integer> range2 = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);

            //Long exposureTime
            Log.e("开始拍照","");
            Log.e("exp_time_range", range.getUpper()+"/"+ range.getLower());
            Log.e("sen_rang", range2.getUpper() + "/" + range2.getLower());
            Integer sensitivity = 10000;
            //首先创建拍照的请求 CaptureRequest
            final CaptureRequest.Builder mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //获取屏幕方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            mCaptureBuilder.addTarget(mPreviewSurface);
            mCaptureBuilder.addTarget(mImageReader.getSurface());
            //设置拍照方向
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATION.get(rotation));
            //设置质量
            mCaptureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 95);
            // 设置自动对焦模式
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 设置自动曝光模式
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            //控制当前白平衡模式的选择(色温)
            mCaptureBuilder.set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_AUTO);//CONTROL_AWB_MODE_AUTO);
            //ISO
            //mCaptureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, sensitivity);

            // 设置为自动模式
            mCaptureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            /*//尝试手动模式
            mCaptureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            //mCaptureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime);

            //mCaptureBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, frameDuration);*/



            //停止预览
            mCaptureSession.stopRepeating();
            //开始拍照,然后回调上面的接口重启预览,因为mCaptureBuilder设置ImageReader作为target,所以会自动回调ImageReader
            // 的onImageAvailable()方法保存图片
            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Log.e("onCaptureCompleted","onCaptureCompleted");
                    super.onCaptureCompleted(session, request, result);
                    repeatPreview();
                }
            };
            mCaptureSession.capture(mCaptureBuilder.build(),captureCallback,null);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void takePhoto1(){
        try {
            Log.e("开始拍照","");
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId); //获取摄像机的特征
            Range<Integer> range = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);

            int max1 = range.getUpper();
            //10000
            int min1 = range.getLower();
            //100
            int iso = 3000;
            Log.e("最大最小iso", max1 +" " + min1);


            //首先创建拍照的请求 CaptureRequest
            final CaptureRequest.Builder mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
            //获取屏幕方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            mCaptureBuilder.addTarget(mPreviewSurface);
            mCaptureBuilder.addTarget(mImageReader.getSurface());
            //设置拍照方向
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATION.get(rotation));
            // 设置自动曝光模式
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF);
            mCaptureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);


            //停止预览
            mCaptureSession.stopRepeating();
            //开始拍照,然后回调上面的接口重启预览,因为mCaptureBuilder设置ImageReader作为target,所以会自动回调ImageReader
            // 的onImageAvailable()方法保存图片
            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Log.e("onCaptureCompleted","onCaptureCompleted");
                    super.onCaptureCompleted(session, request, result);
                    repeatPreview();
                }
            };
            mCaptureSession.capture(mCaptureBuilder.build(),captureCallback,null);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    // 第九步 创建子线程保存图片
    public  static  class ImageSaver implements  Runnable{
        private Image mImage;

        public ImageSaver(Image image) {
            mImage = image;
        }
        @Override
        public void run(){
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data =new  byte[buffer.remaining()];
            buffer.get(data);
            File imageFile = new File(Environment.getExternalStorageDirectory()+"/DCIM/Camera/" + System.currentTimeMillis() + "myPicture.jpg");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(imageFile);
                fos.write(data,0, data.length);
                Utils.sendBroadcastMediaScanner(MainActivity2.getInstance(), imageFile);
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                if(fos !=null){
                    try {
                        fos.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
                mImage.close(); // 必须关闭 不然拍第二章会报错
            }
        }
    }

    //活动的创建
    @Override
    protected void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_camera2);
        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(textureListener);
        //getPermission();
        findViewById(R.id.takePicture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });
    }
    //onPause
    @Override
    protected void onPause(){
        //closeCamera();
        super.onPause();
    }
    protected void onResume(){
        super.onResume();
        // textureView.setSurfaceTextureListener(textureListener);
        //openCamera();
    }
    protected void onDestroy(){
        super.onDestroy();
        closeCamera();
    }

    @Override
    public boolean onSupportNavigateUp() {
        /*NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_camera2);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();*/
        return true;
    }
}