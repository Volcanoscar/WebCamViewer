/*
* ******************************************************************************
* Copyright (c) 2013-2014 Tomas Valenta.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* *****************************************************************************
*/

package cz.yetanotherview.webcamviewer.app.fullscreen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.squareup.picasso.Picasso;

import java.util.Timer;
import java.util.TimerTask;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.Utils;

public class FullScreenImage extends Activity {

    public static final String TAG = "ImmersiveMode";
    private static Context context;

    private TouchImageView image;
    private String url;
    private float zoom;

    private boolean autoRefresh;
    private int autoRefreshInterval;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FullScreenImage.context = getApplicationContext();

        setContentView(R.layout.full_screen_layout);
        goFullScreen();

        Intent intent = getIntent();
        url = intent.getExtras().getString("url");
        zoom = intent.getExtras().getFloat("zoom");
        autoRefresh = intent.getExtras().getBoolean("autoRefresh");
        autoRefreshInterval = intent.getExtras().getInt("interval");

        image = (TouchImageView) findViewById(R.id.touch_image);
        image.setMaxZoom(zoom);

        ImageButton backButton = (ImageButton) findViewById(R.id.back_button);
        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        loadImage();

        if (autoRefresh) {
            autoRefreshTimer(autoRefreshInterval);
        }
    }

    public static Context getAppContext() {
        return FullScreenImage.context;
    }

    private void loadImage() {
        //Picasso.with(FullScreenImage.getAppContext()).setIndicatorsEnabled(true);
        Picasso.with(FullScreenImage.getAppContext())
                .load(url)
                .fit()
                .skipMemoryCache()
                //.centerInside()
                .placeholder(R.drawable.animation)
                .error(R.drawable.placeholder_error)
                .into(image);
    }

    private void goFullScreen() {

        // The UI options currently enabled are represented by a bitfield.
        // getSystemUiVisibility() gives us that bitfield.
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        boolean isImmersiveModeEnabled =
                ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            Log.i(TAG, "Turning immersive mode mode off. ");
        } else {
            Log.i(TAG, "Turning immersive mode mode on.");
        }

        // Navigation bar hiding:  Backwards compatible to ICS.
        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        // Status bar hiding: Backwards compatible to Jellybean
        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        // Immersive mode: Backward compatible to KitKat.
        // Note that this flag doesn't do anything by itself, it only augments the behavior
        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
        // all three flags are being toggled together.
        // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
        // Sticky immersive mode differs in that it makes the navigation and status bars
        // semi-transparent, and the UI flag does not get cleared when the user interacts with
        // the screen.
        if (Build.VERSION.SDK_INT >= 18) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    private void autoRefreshTimer(int interval) {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            refresh();
                        } catch (Exception e) {
                            // Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, interval);
    }

    private void refresh() {
        Utils.deletePicassoCache(getApplicationContext().getCacheDir());
        loadImage();
    }
}
