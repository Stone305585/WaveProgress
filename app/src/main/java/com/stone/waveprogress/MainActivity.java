package com.stone.waveprogress;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.stone.waveprogress.lib.WaveProgress;

public class MainActivity extends AppCompatActivity {

    private WaveProgress wave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        wave = (WaveProgress) findViewById(R.id.wave_progress);

        wave.setCurrent("20%");

    }
}
