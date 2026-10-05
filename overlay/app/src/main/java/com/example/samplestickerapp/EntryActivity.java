package com.example.samplestickerapp;

import android.content.Intent;
import android.os.Bundle;

public class EntryActivity extends BaseActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, AshleyWebActivity.class));
        finish();
        overridePendingTransition(0,0);
    }
}