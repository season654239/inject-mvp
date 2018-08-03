package com.example.yueshaojun.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.lib.Presenter;
import com.example.yueshaojun.myapplication.BaseActivity;
import com.example.yueshaojun.myapplication.P.LoginPresenter;
import com.example.yueshaojun.myapplication.R;
import com.example.yueshaojun.myapplication.V.ILoginView;
import com.example.yueshaojun.myapplication.di.DaggerMyComponent;

import javax.inject.Inject;

/**
 * @author yueshaojun
 */
public class ActivityLogin extends BaseActivity implements ILoginView {

    private static final String TAG = "ActivityLogin_";
    @Inject
    @Presenter
    protected LoginPresenter loginPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        findViewById(R.id.click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginPresenter.login();
            }
        });
    }

    @Override
    public void injectMethod() {
        DaggerMyComponent.create().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MyApplication",TAG+"onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MyApplication",TAG+"onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("MyApplication",TAG+"onStop");
    }


    @Override
    public void onLoginSuccess() {
        Log.i("MyModule", TAG + "LoginSuccess_");
        Toast.makeText(this, "ActivityLogin onLoginSuccess", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this,ActivityPay.class));

    }

    @Override
    public void onLoginError(String errorMsg) {

    }
}
