package com.wizard.adpautoconnect;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
    private Button mConnectButton;
    private boolean mJobDone = false;
    private boolean mFailed = false;
    private WebView engine;
    private boolean mConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initiate GUI
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Map variables to GUI objects
        mInputUserName = (EditText)findViewById(R.id.txtUsername);
        mInputPassword = (EditText) findViewById(R.id.txtPassword);
        mConnectButton = (Button) findViewById(R.id.connect_button);
        engine = (WebView) findViewById(R.id.web_engine);

        // Map variables to OnClick events
        mConnectButton.setOnClickListener(this);

        // Load saved preferences
        funcLoadPresences();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && engine.canGoBack()) {
            engine.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onPause(){
        if(mConnected){
            funcCleanUp();
            engine.clearHistory();
            engine.clearCache(true);
            engine.setVisibility(View.INVISIBLE);
            mConnectButton.setEnabled(true);
            mConnected = false;
        }
        super.onPause();
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public void onGroupItemClick(MenuItem item) {
        // One of the group items (using the onClick attribute) was clicked
        // The item parameter passed here indicates which item it is
        // All other menu item clicks are handled by onOptionsItemSelected()
        switch (item.getItemId()){
            case R.id.clear_creds:
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                funcClearAllPreferences();
                                Toast.makeText(MainActivity.this, "Credentials Cleared", Toast.LENGTH_SHORT).show();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Clear Credentials");
                builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
                break;
        }
    }

    // Specify onClick actions
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect_button:
                // 'Connect to ADP' button pressed
                // Save current username and password.
                // first check for nulls
                if(mInputUserName.getText().toString().trim().length() > 0 && mInputPassword.getText().toString().trim().length() > 0)
                {
                    if(mInputUserName.getText().toString().contains("@"))
                    {
                        // Save the creds
                        funcSavePreferences("ADPUser", mInputUserName.getText().toString());
                        funcSavePreferences("ADPPass", mInputPassword.getText().toString());
                        //Toast.makeText(this, "Credentials Saved", Toast.LENGTH_LONG).show();
                    } else {
                        // Reject the creds
                        Toast.makeText(this, "Username must contain an '@' separating user and company name", Toast.LENGTH_LONG).show();
                        break;
                    }
                }
                else {
                    // Reject the creds
                    Toast.makeText(this, "Please enter credentials first", Toast.LENGTH_LONG).show();
                    break;
                }

                // Verify connectivity
                if(!isOnline()){
                    Toast.makeText(this, "No network connection is available", Toast.LENGTH_LONG).show();
                    break;
                }

                // Create a new thread to handle the progress dialog.
                AsyncTask<Void, Void, Void> taskConnect = new AsyncTask<Void, Void, Void>() {
                    // Define progress dialog
                    private ProgressDialog pd;

                    @Override
                    protected void onPreExecute() {
                        pd = new ProgressDialog(MainActivity.this);
                        pd.setTitle("Connecting...");
                        pd.setMessage("Please wait.");
                        pd.setCancelable(false);
                        pd.setIndeterminate(true);
                        pd.show();
                        // Call connect to ADP function
                        mConnected = true;
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
                            System.err.println("Name of Your Application, Error: " + e.getLocalizedMessage());
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        // After funcWebConnectADP has completed, dismiss the progress dialog
                        pd.dismiss();

                        mJobDone = true;
                        if(mFailed){
                            Toast.makeText(MainActivity.this, "Login failed. Check username, password, and network connectivity", Toast.LENGTH_LONG).show();
                            mConnected = false;
                        } else {
                            engine.setVisibility(View.VISIBLE);
                            mConnectButton.setEnabled(false);
                            mConnected = true;
                        }
                        mFailed = false;
                    }
                };
                taskConnect.execute((Void[]) null);

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
                    mFailed = false;
                    return;
                } else {
                    //Toast.makeText(MainActivity.this, "Something went wrong :(", Toast.LENGTH_LONG).show();
                    mJobDone = true;
                    mFailed = true;
                }
            }
        });
        engine.loadUrl("https://ezlmisiappdc1f.adp.com/ezLaborManagerNet/IFrameRedir.aspx?pg=ECW3FE52&isiclientid=" + separated[1]);
    }

    // Logout of ADP and clean up after WebView
    private void funcCleanUp() {
        engine.setWebViewClient(new WebViewClient() {
            // Pass credentials from text boxes to page when requested.
            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {

                handler.proceed(mInputUserName.getText().toString(), mInputPassword.getText().toString());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if(view.getTitle().contains("Portal Logout") || view.getTitle().contains("Portal Login")){
                    mJobDone = true;
                    mFailed = false;
                    return;
                } else {
                    mJobDone = true;
                    mFailed = true;
                }
            }
        });
        engine.loadUrl("https://portal.adp.com/wps/myportal/sitemap/Employee/Home/Welcome/!ut/p/c5/04_SB8K8xLLM9MSSzPy8xBz9CP0os3ivQHcjDy9vA3d_M1dLA8-wsMDgYHN3Q2cjc30_j_zcVP2CbEdFACV36N8!/dl3/d3/L3dDb1ZJQSEhL3dPb0JKTnNBLzREMGo5ZWtBU0VFIS9IY0JGMjU1MTUwMDAyLzEzNzUyL2xv/");
    }

    // Check online connectivity
    public boolean isOnline(){
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}

