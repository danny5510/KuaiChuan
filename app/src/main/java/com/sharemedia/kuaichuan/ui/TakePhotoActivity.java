package com.sharemedia.kuaichuan.ui;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.sharemedia.kuaichuan.NfcUtils;
import com.sharemedia.kuaichuan.R;
import com.sharemedia.kuaichuan.Utils;

import java.io.IOException;

public class TakePhotoActivity extends AppCompatActivity {
    private String TAG = "TakePhoto";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);
        processIntent(getIntent());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, data.getStringExtra("cardno"));
    }

    //在onResume中开启前台调度001
    @Override
    protected void onResume() {
        super.onResume();
        //设定intent filter和tech-list。如果两个都为null就代表优先接收任何形式的TAG action。也就是说系统会主动发TAG intent。
        if (NfcUtils.mNfcAdapter != null) {
            NfcUtils.mNfcAdapter.enableForegroundDispatch(this, NfcUtils.mPendingIntent, NfcUtils.mIntentFilter, NfcUtils.mTechList);
        }
    }


    //在onNewIntent中处理由NFC设备传递过来的intent
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.e(TAG, "--------------NFC-------------");
        processIntent(intent);
    }

    //  这块的processIntent() 就是处理卡中数据的方法
    public void
    processIntent(Intent intent) {
        Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (messages != null) {
            NdefMessage msg = (NdefMessage) messages[0];
            NdefRecord[] records = msg.getRecords();
            String resultStr = new String(records[0].getPayload());
            // 返回的是NFC检查到卡中的数据
            Log.e(TAG, "processIntent: " + resultStr);
            try {
                // 检测卡的id
                String id = NfcUtils.readNFCId(intent);
                Log.e(TAG, "processIntent--id: " + id);
                // NfcUtils中获取卡中数据的方法
                String result = NfcUtils.readNFCFromTag(intent);
                result = Utils.getJustNumber(result);
                Log.e(TAG, "processIntent--result: " + result);
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
            /*try{
                //往卡中写数据
                String data = "000017";
                NfcUtils.writeNFCToTag(data,intent);
            }catch (FormatException | IOException e) {
                e.printStackTrace();;
            }*/
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (NfcUtils.mNfcAdapter != null) {
            NfcUtils.mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NfcUtils.mNfcAdapter = null;
    }
}