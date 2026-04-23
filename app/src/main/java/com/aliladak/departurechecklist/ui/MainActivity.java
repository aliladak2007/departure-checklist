package com.aliladak.departurechecklist.ui;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aliladak.departurechecklist.databinding.ActivityMainBinding;

/**
 * Single activity host for the application's navigation graph.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;

    /**
     * Creates the activity and inflates the root layout.
     *
     * @param savedInstanceState saved instance state
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        } catch (RuntimeException exception) {
            Log.e(TAG, "Failed to inflate activity layout", exception);
            throw exception;
        }
    }
}

