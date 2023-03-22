package com.sharemedia.kuaichuan;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        btnLogin = this.findViewById(R.id.login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String res = Utils.post(BuildConfig.SERVER_URL + "/login.ashx", "");
                        try {
                            JSONObject jsonObject = new JSONObject(res);
                            if(jsonObject.getInt("status")==200){
                                JSONObject jsData = jsonObject.getJSONObject("data");
                                Log.d("Login", jsData.getString("id"));
                            }
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                    }
                });

                thread.start();
            }
        });
    }


}