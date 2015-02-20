/*
* ******************************************************************************
* Copyright (c) 2013-2015 Tomas Valenta.
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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.PicassoTools;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.Utils;
import cz.yetanotherview.webcamviewer.app.actions.FolderSelectorDialog;

public class FullScreenImage extends Activity implements FolderSelectorDialog.FolderSelectCallback {

    private static final String TAG = "ImmersiveMode";

    private RelativeLayout mButtonsLayout;
    private TouchImageView image;
    private ProgressBar progressBar;
    private Animation fadeOut;
    private String name;
    private String strippedName;
    private String url;
    private float zoom;
    private boolean fullScreen;
    private String path;

    private boolean autoRefresh;
    private int autoRefreshInterval;
    private boolean screenAlwaysOn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.full_screen_layout);
        mButtonsLayout = (RelativeLayout) findViewById(R.id.buttons_layout);

        Intent intent = getIntent();
        name = intent.getExtras().getString("name");
        url = intent.getExtras().getString("url");
        zoom = intent.getExtras().getFloat("zoom");
        fullScreen = intent.getExtras().getBoolean("fullScreen");
        autoRefresh = intent.getExtras().getBoolean("autoRefresh");
        autoRefreshInterval = intent.getExtras().getInt("interval");
        screenAlwaysOn = intent.getExtras().getBoolean("screenAlwaysOn");

        // Go FullScreen only on KitKat and up
        if (Build.VERSION.SDK_INT >= 19 && fullScreen) {
            goFullScreen();
        }

        // Screen Always on
        if (screenAlwaysOn){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Auto Refresh timer
        if (autoRefresh) {
            autoRefreshTimer(autoRefreshInterval);
        }

        initViews();
        loadImage();
        setAnimation();
    }

    private void initViews() {
        image = (TouchImageView) findViewById(R.id.touch_image);
        image.setMaxZoom(zoom);
        image.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mButtonsLayout.setVisibility(View.VISIBLE);
                mButtonsLayout.startAnimation(fadeOut);
            }
        });

        ImageButton refreshButton = (ImageButton) findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });

        ImageButton saveButton = (ImageButton) findViewById(R.id.save_button);
        saveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new FolderSelectorDialog().show(FullScreenImage.this);
            }
        });

        ImageButton backButton = (ImageButton) findViewById(R.id.back_button);
        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);
    }

    private void setAnimation() {
        fadeOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mButtonsLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void loadImage() {
        //Picasso.with(image.getContext()).setIndicatorsEnabled(true);
        Picasso.with(image.getContext())
                .load(url)
                .fit()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder_error)
                .into(image, new Callback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        mButtonsLayout.startAnimation(fadeOut);
                        mButtonsLayout.setBackgroundResource(R.drawable.selector);
                    }

                    @Override
                    public void onError() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
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
        PicassoTools.clearCache(Picasso.with(image.getContext()));
        progressBar.setVisibility(View.VISIBLE);
        loadImage();
    }

    @Override
    public void onFolderSelection(File folder) {
        path = folder.getAbsolutePath();
        strippedName = Utils.getNameStrippedAccents(name);

        Target saveFileTarget = new Target() {
            @Override
            public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        File file = new File(path + "/" + strippedName + " " + Utils.getDateString() + ".jpg");
                        try
                        {
                            file.createNewFile();
                            FileOutputStream ostream = new FileOutputStream(file);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 89, ostream);
                            ostream.close();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                    }
                }).start();
            }
            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        };

        Picasso.with(image.getContext())
                .load(url)
                .into(saveFileTarget);

        Toast.makeText(this, R.string.dialog_positive_toast_message, Toast.LENGTH_SHORT).show();
    }
}
