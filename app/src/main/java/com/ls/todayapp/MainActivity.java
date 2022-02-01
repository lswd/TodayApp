package com.ls.todayapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.example.refresh.GodRefreshLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GodRefreshLayout godRefreshLayout = findViewById(R.id.god_refresh);
        godRefreshLayout.setRefreshManager();
    }
}