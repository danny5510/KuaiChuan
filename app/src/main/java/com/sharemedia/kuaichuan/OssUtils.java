package com.sharemedia.kuaichuan;

import android.content.Context;
import android.util.Log;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.ObjectMetadata;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;

import org.json.JSONException;
import org.json.JSONObject;

public class OssUtils {
    private static Context applicationContext = null;
    private final static String  OSS_ENDPOINT = "https://oss-cn-shenzhen.aliyuncs.com";
    private final static String OSS_KEYFILENAME_PRE = "Projects/XVault";
    private final static String OSS_STSURL= "https://mina.sharemedia.biz:8001/sts/aliyunsts.ashx";
    private final static String OSS_BUCKETNAME = "kuaichuan-files";
    private final static String OSS_URL_PRE = "https://sharemedia-files.oss-cn-shenzhen.aliyuncs.com";
    private static OSSClient client = null;
    private static String AccessKeyId, SecurityToken,AccessKeySecret,Expiration;
    private static void getSts(){
        String res = Utils.post(OSS_STSURL,"");
        try {
            JSONObject jsonObject = new JSONObject(res);
            AccessKeyId = jsonObject.getString("AccessKeyId");
            AccessKeySecret = jsonObject.getString("AccessKeySecret");
            SecurityToken = jsonObject.getString("SecurityToken");
            Expiration = jsonObject.getString("Expiration");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    public static void initClient(Context context){
        applicationContext = context;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    getSts();
                    // yourEndpoint填写Bucket所在地域对应的Endpoint。以华东1（杭州）为例，Endpoint填写为https://oss-cn-hangzhou.aliyuncs.com。
                    String endpoint = OSS_ENDPOINT;
                    // 从STS服务获取的临时访问密钥（AccessKey ID和AccessKey Secret）。
                    // 从STS服务获取的安全令牌（SecurityToken）。
                    OSSCredentialProvider credentialProvider = new OSSStsTokenCredentialProvider(AccessKeyId, AccessKeySecret, SecurityToken);
                    // 创建OSSClient实例。
                    client = new OSSClient(applicationContext, endpoint, credentialProvider);
                } catch (Exception e) {
                    Log.e("OSS",e.getLocalizedMessage());
                }
            }
        });
        thread.start();
    }

    public void put(String objectKey, String uploadFilePath){
        // 构造上传请求。
        // 依次填写Bucket名称（例如examplebucket）、Object完整路径（例如exampledir/exampleobject.txt）和本地文件完整路径（例如/storage/emulated/0/oss/examplefile.txt）。
        // Object完整路径中不能包含Bucket名称。
        PutObjectRequest put = new PutObjectRequest(OSS_BUCKETNAME, objectKey, uploadFilePath);

        // 设置文件元信息为可选操作。
        ObjectMetadata metadata = new ObjectMetadata();
        // metadata.setContentType("application/octet-stream"); // 设置content-type。
        // metadata.setContentMD5(BinaryUtil.calculateBase64Md5(uploadFilePath)); // 校验MD5。
        // 设置object的归档类型为标准存储
        metadata.setHeader("x-oss-storage-class", "Standard");
        // 设置覆盖同名目标Object
         metadata.setHeader("x-oss-forbid-overwrite", "true");


        put.setMetadata(metadata);

        try {
            PutObjectResult putResult = client.putObject(put);

            Log.d("PutObject", "UploadSuccess");
            Log.d("ETag", putResult.getETag());
            Log.d("RequestId", putResult.getRequestId());
        } catch (ClientException e) {
            // 客户端异常，例如网络异常等。
            e.printStackTrace();
        } catch (ServiceException e) {
            // 服务端异常。
            Log.e("RequestId", e.getRequestId());
            Log.e("ErrorCode", e.getErrorCode());
            Log.e("HostId", e.getHostId());
            Log.e("RawMessage", e.getRawMessage());
        }
    }

    public static void putAsync(String objectKey, String file){
        // 构造上传请求。
        // 依次填写Bucket名称（例如examplebucket）、Object完整路径（例如exampledir/exampleobject.txt）和本地文件完整路径（例如/storage/emulated/0/oss/examplefile.txt）。
        // Object完整路径中不能包含Bucket名称。
        PutObjectRequest put = new PutObjectRequest(OSS_BUCKETNAME, objectKey, file);

        // 异步上传时可以设置进度回调。
        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                Log.d("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
            }
        });

        OSSAsyncTask task = client.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.d("PutObject", "UploadSuccess");
                Log.d("ETag", result.getETag());
                Log.d("RequestId", result.getRequestId());
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常。
                if (clientExcepion != null) {
                    // 客户端异常，例如网络异常等。
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务端异常。
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
            }
        });
        // 取消上传任务。
        // task.cancel();
        // 等待上传任务完成。
        task.waitUntilFinished();
    }

}
