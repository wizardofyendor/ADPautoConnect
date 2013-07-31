package com.wizard.adpautoconnect;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
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

    // Define variables
    private EditText mInputUserName;
    private EditText mInputPassword;
    private Button mLogoutButton;
    private Button mConnectButton;
    private boolean mJobDone = false;
    private WebView engine;
//    private ProgressDialog pd;
    private String resultTitle;

    // Map variables to fields, and load defaults
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initiate GUI
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Map variables to GUI objects
        mInputUserName = (EditText)findViewById(R.id.txtUsername);
        mInputPassword = (EditText) findViewById(R.id.txtPassword);
        Button mSaveButton = (Button) findViewById(R.id.save_button);
        Button mClearButton = (Button) findViewById(R.id.clear_button);
        mConnectButton = (Button) findViewById(R.id.connect_button);
        mLogoutButton = (Button) findViewById(R.id.logout_button);
        engine = (WebView) findViewById(R.id.web_engine);
//        pd = new ProgressDialog(this);

        // Map variables to OnClick events
        mSaveButton.setOnClickListener(this);
        mClearButton.setOnClickListener(this);
        mConnectButton.setOnClickListener(this);
        mLogoutButton.setOnClickListener(this);

        // Load saved preferences
        funcLoadPresences();
    }

    // Specify onClick actions
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save_button:
                // 'Save Creds' button pressed
                // Save current username and password.
                // first check for nulls
                if(mInputUserName.getText().toString().trim().length() > 0 && mInputPassword.getText().toString().trim().length() > 0)
                {
                    if(mInputUserName.getText().toString().contains("@"))
                    {
                        // Save the creds
                        funcSavePreferences("ADPUser", mInputUserName.getText().toString());
                        funcSavePreferences("ADPPass", mInputPassword.getText().toString());
                        Toast.makeText(this, "Credentials Saved", Toast.LENGTH_LONG).show();
                    } else {
                        // Reject the creds
                        Toast.makeText(this, "Username must contain an '@' separating name and company name", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    // Reject the creds
                    Toast.makeText(this, "Please enter credentials first", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.clear_button:
                // 'Clear Creds' button pressed
                // Blank all credentials
                funcClearAllPreferences();
                Toast.makeText(this, "Removed Credentials", Toast.LENGTH_LONG).show();
                break;
            case R.id.connect_button:
                // 'Connect to ADP' button pressed

                // Verify connectivity
                if(!isOnline()){
                    Toast.makeText(this, "No network connection is available", Toast.LENGTH_LONG).show();
                    break;
                }

                // Create a new thread to handle the progress dialog.
                AsyncTask<Void, Void, Void> taskConnect = new AsyncTask<Void, Void, Void>() {
                    private ProgressDialog pd;
                    // Define progress dialog
                    @Override
                    protected void onPreExecute() {
                        pd = new ProgressDialog(MainActivity.this);
                        pd.setTitle("Connecting...");
                        pd.setMessage("Please wait.");
                        pd.setCancelable(false);
                        pd.setIndeterminate(true);
                        pd.show();
                        funcWebConnectADP();
                    }

                    @Override
                    protected Void doInBackground(Void... arg0) {
                        // run the funcWebConnectADP function and check every 0.5 seconds to see if
                        // it has completed
                        mJobDone = false;
                        try {
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
                    // After funcWebConnectADP has completed, dismiss the progress dialog
                    @Override
                    protected void onPostExecute(Void result) {
                        pd.dismiss();
                        //pd.hide();
                        mJobDone = true;
                        engine.setVisibility(View.VISIBLE);
                        //if(resultTitle == "ADP ezLaborManager - Home")
                        //{
                            mConnectButton.setEnabled(false);
                            mLogoutButton.setEnabled(true);
                        //}

                        //Toast.makeText(MainActivity.this, resultTitle, Toast.LENGTH_LONG).show();
                    }
                };
                taskConnect.execute((Void[]) null);

                break;
            case R.id.logout_button:
                // Logout button pressed
                // Run funcCleanUp, and pass 'true' so it knows it was from a button press
                //funcCleanUp(true);
                // Create a new thread to handle the progress dialog.
                AsyncTask<Void, Void, Void> taskLogout = new AsyncTask<Void, Void, Void>() {
                    private ProgressDialog pd;
                    // Define progress dialog
                    @Override
                    protected void onPreExecute() {
                        pd = new ProgressDialog(MainActivity.this);
                        pd.setTitle("Logging out...");
                        pd.setMessage("Please wait.");
                        pd.setCancelable(false);
                        pd.setIndeterminate(true);
                        pd.show();
                        engine.setVisibility(View.INVISIBLE);
                        funcCleanUp();
                    }

                    @Override
                    protected Void doInBackground(Void... arg0) {
                        // run the funcWebConnectADP function and check every 0.5 seconds to see if
                        // it has completed
                        mJobDone = false;
                        try {
                            //funcCleanUp(true);
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
                    // After funcWebConnectADP has completed, dismiss the progress dialog
                    @Override
                    protected void onPostExecute(Void result) {
                        pd.dismiss();
                        //pd.hide();
                        mJobDone = true;
                        mConnectButton.setEnabled(true);
                        mLogoutButton.setEnabled(false);

                        //Toast.makeText(MainActivity.this, resultTitle, Toast.LENGTH_LONG).show();
                        Toast.makeText(MainActivity.this, "Logout successful", Toast.LENGTH_LONG).show();
                    }
                };
                taskLogout.execute((Void[]) null);
                break;
        }
    }

    // Save credentials
    private void funcSavePreferences(String key, String value) {
        // Save the preferences
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    // Erase saved credentials
    private void funcClearAllPreferences() {
        // Erase the saved preferences and null the text boxes
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
        mInputUserName.setText("");
        mInputPassword.setText("");
    }

    // Load saved preferences
    private void funcLoadPresences() {
        // Load the saved preferences, and set the text boxes to their values
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        String savedPref = sharedPreferences.getString("ADPUser", "");
        mInputUserName.setText(savedPref);
        savedPref = sharedPreferences.getString("ADPPass", "");
        mInputPassword.setText(savedPref);
    }

    // Initiate the WebView object and and initiate the connection to ADP
    private void funcWebConnectADP() {
        // Split username and company name
        String[] separated = mInputUserName.getText().toString().split("@");
        // Map 'engine' to the WebView object, and enable java script
        //final WebView engine = (WebView) findViewById(R.id.web_engine);
        WebSettings webSettings = engine.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        engine.setVisibility(View.INVISIBLE);
        // Map web calls from this application to the WebView object as a web client
        engine.setWebViewClient(new WebViewClient() {
            // Pass credentials from text boxes to page when requested.
            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {

                handler.proceed(mInputUserName.getText().toString(), mInputPassword.getText().toString());
            }
            // Once the page has finished loading call the login function
            @Override
            public void onPageFinished(final WebView view, String url) {
                super.onPageFinished(view, url);
                // Verify you are at the right page
                if(view.getTitle().contains("ADP ezLaborManager - Home"))
                {
                    // Enable the WebView and logout buttons, and set job to done.
                    mJobDone = true;
                } else {
                    Toast.makeText(MainActivity.this, "Something went wrong :(", Toast.LENGTH_LONG).show();
                    mJobDone = true;
                }
                resultTitle = view.getTitle();
            }
        });
        engine.loadUrl("https://ezlmisiappdc1f.adp.com/ezLaborManagerNet/IFrameRedir.aspx?pg=ECW3FE52&isiclientid=" + separated[1]);
    }

    // Logout of ADP and clean up after WebView
    private void funcCleanUp() {
        //final WebView engine = (WebView) findViewById(R.id.web_engine);
        engine.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if(view.getTitle().contains("Portal Logout")){
                    mJobDone = true;
                    engine.clearCache(true);
                }
                resultTitle = view.getTitle();
            }
        });
        engine.loadUrl("https://portal.adp.com/wps/myportal/sitemap/Employee/Home/Welcome/!ut/p/c5/04_SB8K8xLLM9MSSzPy8xBz9CP0os3ivQHcjDy9vA3d_M1dLA8-wsMDgYHN3Q2cjc30_j_zcVP2CbEdFACV36N8!/dl3/d3/L3dDb1ZJQSEhL3dPb0JKTnNBLzREMGo5ZWtBU0VFIS9IY0JGMjU1MTUwMDAyLzEzNzUyL2xv/");
    }

    public boolean isOnline(){
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if(netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    // If app is killed perform cleanup
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        //funcCleanUp(false);
    }
}

