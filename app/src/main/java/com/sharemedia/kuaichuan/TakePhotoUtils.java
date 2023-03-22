package com.sharemedia.kuaichuan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TakePhotoUtils {
    public  static  Uri takePhoto2(Activity activity, String name) throws  IOException {
        Context context = activity.getApplicationContext();
        // 打开相机Intent
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
// 给拍摄的照片指定存储位置
        String f = "Camera/" + name +".jpg"; // 指定名字
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), f); // 指定文件
        Log.d("TakePhoto2", file.getAbsolutePath());
        //Uri fileUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", file); // 路径转换
        Uri fileUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file); // 路径转换

        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); //指定图片存放位置，指定后，在onActivityResult里得到的Data将为null
        cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
        cameraIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 1024*1024*5);

        //cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, file.getAbsolutePath());
        activity.startActivityForResult(cameraIntent, 0);
        Log.d("TakePhoto2.fileUri.getPath", fileUri.getPath());
        Log.d("TakePhoto2.fileUri", fileUri.toString());
        activity.setResult(0, cameraIntent);

        /*ContentResolver resolver = context.getContentResolver();
        ContentValues newValues = new ContentValues(5);
        newValues.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
        newValues.put(MediaStore.Images.Media.DATA, file.getPath());
        newValues.put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000);
        newValues.put(MediaStore.Images.Media.SIZE, file.length());
        newValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newValues);*/
        //MediaScannerConnection.scanFile(context, new String[]{file.getAbsoluteFile().getPath()},null, null);//刷新相册

        /*MediaScanner scanner=new MediaScanner(context);
        scanner.scanFile( new File(String.valueOf(fileUri)) , null);
        scanner.scanFile(file, null);*/




/*        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                try {
                    MediaStore.Images.Media.insertImage(activity.getContentResolver(),
                            fileUri.getPath(), file.getName(), null);
                    Log.d("TakePhoto2", "其次把文件插入到系统图库");
                } catch (FileNotFoundException e) {
                    Log.d("TakePhoto2.ERROR", e.getMessage());
                    e.printStackTrace();
                }
                // 通知图库更新
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    MediaScannerConnection.scanFile(context, new String[]{ fileUri.toString() }, null,
                            (path, uri) -> {
                                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                mediaScanIntent.setData(uri);
                                activity.sendBroadcast(mediaScanIntent);
                            });
                } else {
                    String relationDir = file.getParent();
                    File file1 = new File(relationDir);
                    activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(file1.getAbsoluteFile())));
                }
            }
        });
        thread.start();*/



        return fileUri;
    }



    /**
     * 拍照
     */
    public static Uri takePhoto(Activity mActivity, int flag) throws IOException {
        //指定拍照intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri imageUri = null;
        if (takePictureIntent.resolveActivity(mActivity.getPackageManager()) != null) { }
            String sdcardState = Environment.getExternalStorageState();
            File outputImage = null;
            if (Environment.MEDIA_MOUNTED.equals(sdcardState)) {
                outputImage = createImageFile(mActivity);
            } else {
                Toast.makeText(mActivity.getApplicationContext(), "内存异常", Toast.LENGTH_SHORT).show();
            }
            /*try {
                if (outputImage.exists()) {
                    outputImage.delete();
                }
                outputImage.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
            if (outputImage != null) {
                imageUri = Uri.fromFile(outputImage);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                mActivity.startActivityForResult(takePictureIntent, flag);
            }


        return imageUri;
    }





    public static  File createImageFile(Activity mActivity) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;//创建以时间命名的文件名称
        File storageDir = getOwnCacheDirectory(mActivity, "takephoto");//创建保存的路径
        File image = new File(storageDir.getPath(), imageFileName + ".jpg");
        if (!image.exists()) {
            try {
                //在指定的文件夹中创建文件
                image.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }

        return image;
    }


    /**
     * 根据目录创建文件夹
     * @param context
     * @param cacheDir
     * @return
     */
    public static File getOwnCacheDirectory(Activity context, String cacheDir) {
        File appCacheDir = null;
        //判断sd卡正常挂载并且拥有权限的时候创建文件
        if ( Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) && hasExternalStoragePermission(context)) {
            appCacheDir = new File(Environment.getExternalStorageDirectory(), cacheDir);
        }
        if (appCacheDir == null || !appCacheDir.exists() && !appCacheDir.mkdirs()) {
            appCacheDir = context.getCacheDir();
        }
        return appCacheDir;
    }


    /**
     * 检查是否有权限
     * @param context
     * @return
     */
    private static boolean hasExternalStoragePermission(Activity context) {
        int perm = context.checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE");
        return perm == 0;
    }


}
