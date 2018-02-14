package com.boxmeup.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import com.boxmeup.app.scans.QrScanResult;
import com.boxmeup.app.scans.ScanResult;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends Activity {

    WebView mWebView;
    ImageView mImageView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mWebView = (WebView) findViewById(R.id.webview);
        mImageView = (ImageView) findViewById(R.id.imageview);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.loadUrl(getString(R.string.url_source));
        mWebView.setWebViewClient(new BoxmeupWebViewClient());

        // Initialize the javascript interface
        mWebView.addJavascriptInterface(new BoxmeupJavascriptInterface(this), getString(R.string.javascript_interface));
//      mWebView.setWebContentsDebuggingEnabled(true);
        mWebView.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                Uri requestedUrl = Uri.parse(url);
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                DownloadManager.Request request = new DownloadManager.Request(requestedUrl);
                request.setMimeType(mimeType);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,fileName);
                String cookie = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("Cookie", cookie);

                //Persist download notification in the status bar after the download completes (Android 3.0+)
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                dm.enqueue(request);
            }

        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        String contents = null, scanFormat = null;
        ScanResult result = null;

        if (scanResult != null) {
            result = new QrScanResult(this);
            result.processResult(scanResult);
        }
    }

    public WebView getWebView() {
        return this.mWebView;
    }

    public String getValidUrlHost() {
        return getString(R.string.valid_host);
    }

    /**
     * Nested class that controls webview specific events
     */
    private class BoxmeupWebViewClient extends WebViewClient {
        /**
         * Keeps clicked links and submitted forms from opening the default browser app.
         *
         * @param view
         * @param url
         * @return true
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        /**
         * Responsible for hiding the splash screen and making the main browser view visible.
         *
         * @param view
         * @param url
         */
        @Override
        public void onPageFinished(WebView view, String url) {
            mImageView.setVisibility(View.GONE);
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Javascript interface that allows the web application to talk to the
     * android app.
     */
    private class BoxmeupJavascriptInterface {
        Context mContext;

        BoxmeupJavascriptInterface(Context c) {
            mContext = c;
        }

        /** Show a toast from the web page */
        @android.webkit.JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        }

        @android.webkit.JavascriptInterface
        public void qrScan() {
            IntentIntegrator integrator = new IntentIntegrator((Activity)mContext);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
            integrator.setPrompt(getString(R.string.qr_scan_prompt));
            integrator.setBeepEnabled(false);
            integrator.initiateScan();
        }
    }
}
