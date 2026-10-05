package com.example.samplestickerapp;

import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class AshleyWebActivity extends BaseActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        WebView web = new WebView(this);
        web.setBackgroundColor(0xFF080A12);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/ashley/ashley.html");
    }

    private class Bridge {
        @JavascriptInterface public void action(String value) {
            runOnUiThread(() -> Toast.makeText(AshleyWebActivity.this,
                    value.equals("guardar") ? "Sticker listo para guardar" :
                    value.equals("compartir") ? "Compartir disponible" :
                    "Compartir en " + value,
                    Toast.LENGTH_SHORT).show());
        }
    }
}