package com.griffith.adpautoconnect;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

    private EditText mInputUserName;
    private EditText mInputPassword;
    private Button mClockOutButton;
    private Button mClockInButton;
    public boolean mJobDone = false;
    public int intAction = 0; // 1 for clock in, 2 for clock out

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mInputUserName = (EditText)findViewById(R.id.txtUsername);
        mInputPassword = (EditText) findViewById(R.id.txtPassword);
        Button mSaveButton = (Button) findViewById(R.id.save_button);
        Button mClearButton = (Button) findViewById(R.id.clear_button);
        mClockInButton = (Button) findViewById(R.id.in_button);
        mClockOutButton = (Button) findViewById(R.id.out_button);
        mSaveButton.setOnClickListener(this);
        mClearButton.setOnClickListener(this);
        mClockInButton.setOnClickListener(this);
        mClockOutButton.setOnClickListener(this);

        loadPreferences();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save_button:
                SavePreferences("ADPUser", mInputUserName.getText().toString());
                SavePreferences("ADPPass", mInputPassword.getText().toString());
                Toast.makeText(this, "Credentials Saved", Toast.LENGTH_LONG).show();
                break;
            case R.id.clear_button:
                clearAllPreferences();
                Toast.makeText(this, "Removed Credentials", Toast.LENGTH_LONG).show();
                break;
            case R.id.in_button:
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    private ProgressDialog pd;

                    @Override
                    protected void onPreExecute() {
                        pd = new ProgressDialog(MainActivity.this);
                        pd.setTitle("Connecting...");
                        pd.setMessage("Please wait.");
                        pd.setCancelable(false);
                        pd.setIndeterminate(true);
                        pd.show();
                    }
                    @Override
                    protected Void doInBackground(Void... arg0) {
                        try {
                            mJobDone = false;
                            ClockIn();
                            while(!mJobDone)
                            {
                            Thread.sleep(500);
                            }
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        pd.dismiss();
                        mJobDone = true;
                    }
                };
                task.execute((Void[])null);
                break;
            case R.id.out_button:
                ClockOut(true);
                break;
        }
    }

    private void SavePreferences(String key, String value) {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private void clearAllPreferences() {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
        mInputUserName.setText("");
        mInputPassword.setText("");
    }

    private void loadPreferences() {
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String savedPref = sharedPreferences.getString("ADPUser", "");
        mInputUserName.setText(savedPref);
        savedPref = sharedPreferences.getString("ADPPass", "");
        mInputPassword.setText(savedPref);
    }

    private void ClockIn() {
        final WebView engine = (WebView) findViewById(R.id.web_engine);
        WebSettings webSettings = engine.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        engine.setVisibility(View.INVISIBLE);
        engine.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                ADPLogin(engine);
            }
        });
         engine.loadUrl("https://portal.adp.com");
    }
    private void ClockOut(boolean buttonclicked) {
        final WebView engine = (WebView) findViewById(R.id.web_engine);
        engine.setVisibility(View.INVISIBLE);
        engine.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                engine.clearCache(true);
            }
        });
        engine.loadUrl("https://portal.adp.com/wps/myportal/sitemap/Employee/Home/Welcome/!ut/p/c5/04_SB8K8xLLM9MSSzPy8xBz9CP0os3ivQHcjDy9vA3d_M1dLA8-wsMDgYHN3Q2cjc30_j_zcVP2CbEdFACV36N8!/dl3/d3/L3dDb1ZJQSEhL3dPb0JKTnNBLzREMGo5ZWtBU0VFIS9IY0JGMjU1MTUwMDAyLzEzNzUyL2xv/");

//        engine.destroy();
        if(buttonclicked)
        {
            if(getApplicationContext() == null)
            {
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            }
            mClockInButton.setEnabled(true);
            mClockOutButton.setEnabled(false);
        }
        //finish();
    }

    private void ADPLogin(final WebView engine) {
        engine.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {

                handler.proceed(mInputUserName.getText().toString(), mInputPassword.getText().toString());
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                ADPLogin2(engine);
            }
        });
        engine.loadUrl("https://portal.adp.com/wps/employee/employee.jsp");
    }

    private void ADPLogin2 (final WebView engine) {
        final int[] intpass= {0};
        engine.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                new CountDownTimer(2000, 1000) {
                    @Override
                    public void onTick(long arg0) {}
                    @Override
                    public void onFinish() {
                        intpass[0]++;
                        if(intpass[0] > 1)
                        {
                            engine.setVisibility(View.VISIBLE);
                            mClockOutButton.setEnabled(true);
                            mClockInButton.setEnabled(false);
                            mJobDone = true;
                        }
                    }
                }.start();
            }
        });
        engine.loadUrl("https://ezlmisiappdc1f.adp.com/ezLaborManagerNet/IFrameRedir.aspx?pg=ECW3FE52&isiclientid=veeam");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        ClockOut(false);
    }
}

