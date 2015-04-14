package com.se1.Activity;


import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.se1.main.MainActivity;
import com.se1.main.R;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;



    public class splash extends Activity {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.splashscreen);

            int secondsDelayed = 1;
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    startActivity(new Intent(splash.this, MainActivity.class));
                    finish();
                }
            }, secondsDelayed * 10000);
        }
    }