package com.sharemedia.kuaichuan;

import android.app.Activity;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    //返回字符中的数字部分
    public static String getJustNumber(String input){
        String regEx="[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(input);
        return m.replaceAll("");
    }

    @NonNull
    public static Size getResolution(@NonNull final CameraManager cameraManager, @NonNull final String cameraId) throws CameraAccessException
    {
        final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        final StreamConfigurationMap map             = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null)
        {
            throw new IllegalStateException("Failed to get configuration map.");

        }

        final Size[] choices = map.getOutputSizes(ImageFormat.JPEG);
        final Size size = getResolution(cameraManager, cameraId);
        final float megapixels = (((size.getWidth() * size.getHeight()) / 1000.0f) / 1000.0f);
        final String caption = String.format(Locale.getDefault(), "%.1f", megapixels);


        return size;
    }

    public  static void sendBroadcastMediaScanner(Activity activity, File file){
        // 通知图库更新
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            MediaScannerConnection.scanFile(activity.getApplicationContext(), new String[]{ file.getAbsolutePath()}, null,
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
    //POST
    public static String post(String url1, String data) {
        try {
            URL url = new URL(url1);
            HttpURLConnection Connection = (HttpURLConnection) url.openConnection();//创建连接
            Connection.setRequestMethod("POST");
            Connection.setConnectTimeout(3000);
            Connection.setReadTimeout(3000);
            Connection.setDoInput(true);
            Connection.setDoOutput(true);
            Connection.setUseCaches(false);
            Connection.connect();
            DataOutputStream dos = new DataOutputStream(Connection.getOutputStream());
            String title = data;//这里是POST请求需要的参数字符串类型，例如"id=1&data=2"
            dos.write(title.getBytes());
            dos.flush();
            dos.close();//写完记得关闭
            int responseCode = Connection.getResponseCode();
            if (responseCode == Connection.HTTP_OK) {//判断请求是否成功
                InputStream inputStream = Connection.getInputStream();
                ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                byte[] bytes = new byte[1024];
                int length = 0;
                while ((length = inputStream.read(bytes)) != -1) {
                    arrayOutputStream.write(bytes, 0, length);
                    arrayOutputStream.flush();
                }//读取响应体的内容
                String s = arrayOutputStream.toString();
                return s;//返回请求到的内容，字符串形式
            } else {
                return "-1";//如果请求失败返回-1
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "-1";//出现异常也返回-1
        }
    }

    public String get(String url1) {
        try {
            URL url = new URL(url1);
            HttpURLConnection Connection = (HttpURLConnection) url.openConnection();
            Connection.setRequestMethod("GET");
            Connection.setConnectTimeout(3000);
            Connection.setReadTimeout(3000);
            int responseCode = Connection.getResponseCode();
            if (responseCode == Connection.HTTP_OK) {
                InputStream inputStream = Connection.getInputStream();
                ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
                byte[] bytes = new byte[1024];
                int length = 0;
                while ((length = inputStream.read(bytes)) != -1) {
                    arrayOutputStream.write(bytes, 0, length);
                    arrayOutputStream.flush();//强制释放缓冲区
                }
                String s = arrayOutputStream.toString();
                return s;
            } else {
                return "-1";
            }
        } catch (Exception e) {
            return "-1";
        }
    }

}
