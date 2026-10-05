package com.example.samplestickerapp;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
public class EntryActivity extends BaseActivity {
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, CreativeStudioActivity.class));
        finish();
    }
}