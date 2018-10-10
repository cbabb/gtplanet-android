package net.gtplanet.android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    private WebView webview;
    private static final String TAG = MainActivity.class.getSimpleName();
    private String mCM;
    private ValueCallback mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR=1;
    private boolean multiple_files = false;

    //back button navigate back
    public void onBackPressed()
    {
        if (this.webview.canGoBack())
        {
            this.webview.goBack();
            return;
        }
        super.onBackPressed();
    }

    //begin core


    // orig
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if(Build.VERSION.SDK_INT >= 21){
            Uri[] results = null;
            //Checking
            if(resultCode== Activity.RESULT_OK){
                if(requestCode == FCR){
                    if(null == mUMA){
                        return;
                    }
                    if(intent == null || intent.getData() == null){
                        //take
                        if(mCM != null){
                            results = new Uri[]{Uri.parse(mCM)};
                        }
                    }else{
                        String dataString = intent.getDataString();
                        if(dataString != null){
                            results = new Uri[]{Uri.parse(dataString)};
                        } else {
                            if(multiple_files) {
                                if (intent.getClipData() != null) {
                                    final int numSelectedFiles = intent.getClipData().getItemCount();
                                    results = new Uri[numSelectedFiles];
                                    for (int i = 0; i < numSelectedFiles; i++) {
                                        results[i] = intent.getClipData().getItemAt(i).getUri();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        }else{
            if(requestCode == FCR){
                if(null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >=23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED))
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);

        webview = findViewById(R.id.webView);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setDomStorageEnabled(true);
        assert webview != null;
        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);

        webview = findViewById(R.id.webView);
        webview.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {

                // modify css
                injectCSS();
                super.onPageFinished(view, url);
            }
        });


        webview.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        webview.setWebViewClient(new Callback());
        webview.loadUrl("http://www.gtplanet.net/");




    }
    public void injectCSS() {
        try {
            InputStream inputStream = getAssets().open ("src/main/assets/style.css");
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            webview.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style)" +
                    "})()");
        } catch (Exception e) {
            e.printStackTrace();
        }

        webview.setWebChromeClient(new WebChromeClient(){
            public boolean onShowFileChooser(
                    WebView webview, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams){
                if(mUMA != null) mUMA.onReceiveValue(null);
                mUMA = filePathCallback;
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null){
                    File photoFile = null;
                    try{
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCM);
                    }catch(IOException ex){
                        Log.e(TAG, getString(R.string.creation), ex);
                    }
                    if(photoFile != null){
                        mCM = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    }else{
                        takePictureIntent = null;
                    }
                }
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("*/*");
                if(multiple_files) {
                    contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                Intent[] intentArray;
                if(takePictureIntent != null){
                    intentArray = new Intent[]{takePictureIntent};
                }else{
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, R.string.pickimg);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                if(multiple_files) {
                    chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                startActivityForResult(chooserIntent, FCR);
                return true;
            }
        });
    }

    class Callback extends WebViewClient{
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl){
            Toast.makeText(getApplicationContext(), getString(R.string.err), Toast.LENGTH_SHORT).show();
        }
    }
    private File createImageFile() throws IOException{
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_"+timeStamp+"_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName,".jpg",storageDir);
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }
}
