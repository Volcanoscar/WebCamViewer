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
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.maps.MapsFragment;

public class FullScreenActivity extends Activity {

    private static final String TAG = "ImmersiveMode";

    private FullScreenFragment fullScreenFragment;
    private MapsFragment mapsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.full_screen_container);

        Intent intent = getIntent();
        boolean map = intent.getExtras().getBoolean("map");
        boolean fullScreen = intent.getExtras().getBoolean("fullScreen");
        boolean screenAlwaysOn = intent.getExtras().getBoolean("screenAlwaysOn");

        // Orientation
        if (!map) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

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

        // Fragment Transaction
        if (findViewById(R.id.full_screen_container) != null) {
            if (savedInstanceState != null) {
                return;
            }
            if (!map) {
                fullScreenFragment = new FullScreenFragment();
                fullScreenFragment.setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction()
                        .add(R.id.full_screen_container, fullScreenFragment).commit();
            }
            else {
                mapsFragment = new MapsFragment();
                mapsFragment.setArguments(getIntent().getExtras());
                getFragmentManager().beginTransaction()
                        .add(R.id.full_screen_container, mapsFragment).commit();
            }
        }
    }

    public void replaceFragments(boolean fromImageView) {

        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        if (fromImageView) {
            if (mapsFragment == null) {
                mapsFragment = new MapsFragment();
            }
            transaction.replace(R.id.full_screen_container, mapsFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
        else {
            if (fullScreenFragment == null) {
                fullScreenFragment = new FullScreenFragment();
            }
            transaction.replace(R.id.full_screen_container, fullScreenFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
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
}
