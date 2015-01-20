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

package cz.yetanotherview.webcamviewer.app;

import android.app.DialogFragment;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;
import com.nispok.snackbar.Snackbar;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cz.yetanotherview.webcamviewer.app.actions.AddDialog;
import cz.yetanotherview.webcamviewer.app.actions.EditDialog;
import cz.yetanotherview.webcamviewer.app.actions.JsonFetcherDialog;
import cz.yetanotherview.webcamviewer.app.actions.WelcomeDialog;
import cz.yetanotherview.webcamviewer.app.fullscreen.FullScreenImage;
import cz.yetanotherview.webcamviewer.app.adapter.WebCamAdapter;
import cz.yetanotherview.webcamviewer.app.helper.DatabaseHelper;
import cz.yetanotherview.webcamviewer.app.helper.WebCamListener;
import cz.yetanotherview.webcamviewer.app.model.Category;
import cz.yetanotherview.webcamviewer.app.model.WebCam;

public class MainActivity extends ActionBarActivity implements WebCamListener, JsonFetcherDialog.ReloadInterface, SwipeRefreshLayout.OnRefreshListener {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();

    private DatabaseHelper db;
    private WebCam webCam;
    private List<WebCam> allWebCams;
    private List<Category> allCategories;
    private String[] drawerItems;
    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private WebCamAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private SwipeRefreshLayout swipeLayout;
    private Toolbar mToolbar;

    private boolean firstRun;
    private String sortOrder = "id ASC";
    private String allWebCamsString;
    private String selectedCategoryName;
    private int selectedCategory;
    private float zoom;
    private boolean fullScreen;
    private boolean autoRefresh;
    private int autoRefreshInterval;
    private boolean autoRefreshFullScreenOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // loading saved preferences
        loadPref();
        allWebCamsString = getString(R.string.all_webcams);

        setContentView(R.layout.activity_main);
        mEmptyView = findViewById(R.id.empty);

        // Auto Refreshing
        if (autoRefresh && !autoRefreshFullScreenOnly) {
            autoRefreshTimer(autoRefreshInterval);
        }

        // Go FullScreen only on KitKat and up
        if (Build.VERSION.SDK_INT >= 19 && fullScreen) {
            goFullScreen();
        }

        // First run
        if (firstRun){
            showWelcomeDialog();
            // Save the state
            firstRun = false;
            saveToPref();
        }

        db = new DatabaseHelper(getApplicationContext());

        initToolbar();
        initDrawer();
        initRecyclerView();
        initFab();
        initPullToRefresh();

    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(allWebCamsString);
        setSupportActionBar(mToolbar);
    }

    private void initDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerList = (ListView) findViewById(R.id.drawer);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        allCategories = db.getAllCategories();
        drawerItems = new String[allCategories.size() + 1];
        drawerItems[0] = allWebCamsString;
        if (allCategories.size() != 0) {
            int count = 1;
            for (Category category : allCategories) {
                drawerItems[count] = category.getcategoryName();
                count++;
            }
        }

        if (mDrawerList != null) {
            ArrayAdapter<String> mArrayAdapter = new ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_list_item_1,
                    drawerItems);
            mDrawerList.setAdapter(mArrayAdapter);
            mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        }

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolbar, R.drawable.ic_action_content_new, R.drawable.ic_action_sort_by_size_white) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void initRecyclerView() {
        mLayoutManager = new LinearLayoutManager(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.cardList);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        allWebCams = db.getAllWebCams(sortOrder);
        db.closeDB();

        mAdapter = new WebCamAdapter(allWebCams);
        mRecyclerView.setAdapter(mAdapter);

        checkAdapterIsEmpty();

        mAdapter.setClickListener(new WebCamAdapter.ClickListener() {

            @Override
            public void onClick(View v, int position, boolean isEditClick) {
                if (isEditClick) {
                    showEditDialog(position);
                } else {
                    showImageFullscreen(position);
                }
            }
        });
    }

    private void initFab() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(mRecyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddDialog();
            }
        });
    }

    private void initPullToRefresh() {
        // Pull To Refresh 1/2
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeResources(R.color.primary_dark);
    }

    private void checkAdapterIsEmpty () {
        if (mAdapter.getItemCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            reInitializeAdapter(position);
            selectedCategory = position;

            getSupportActionBar().setTitle(selectedCategoryName);
            mDrawerLayout.closeDrawers();
        }
    }

    private void reInitializeAdapter(int position) {
        allWebCams.clear();
        if (position == 0) {
            allWebCams = db.getAllWebCams(sortOrder);
            db.closeDB();
            mAdapter.swapData(allWebCams);
            selectedCategoryName = allWebCamsString;
        }
        else {
            Category category = allCategories.get(position - 1);
            allWebCams = db.getAllWebCamsByCategory(category.getId(),sortOrder);
            db.closeDB();
            mAdapter.swapData(allWebCams);
            selectedCategoryName = category.getcategoryName();
        }
        saveToPref();
        checkAdapterIsEmpty();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        setItemChecked(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {

            //Refresh
            case R.id.action_refresh:
                refresh();
                refreshDone();
                break;

            //Sort view
            case R.id.sort_def:
                sortOrder = "id ASC";
                reInitializeAdapter(selectedCategory);
                item.setChecked(true);
                break;
            case R.id.sort_asc:
                sortOrder = "webcam_name COLLATE UNICODE";
                reInitializeAdapter(selectedCategory);
                item.setChecked(true);
                break;
            case R.id.sort_desc:
                sortOrder = "webcam_name COLLATE UNICODE DESC";
                reInitializeAdapter(selectedCategory);
                item.setChecked(true);
                break;

            //Settings
            case R.id.action_settings:
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, 0);
                break;

            //About
            case R.id.menu_about:
                showAbout();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setItemChecked(Menu menu) {
        MenuItem def = menu.findItem(R.id.sort_def);
        def.setChecked(true);
    }

    private void showWelcomeDialog() {
        DialogFragment newFragment = new WelcomeDialog();
        newFragment.show(getFragmentManager(), "WelcomeDialog");
    }

    private void showAbout() {
        final String VERSION_UNAVAILABLE = "N/A";

        // Get app version
        PackageManager pm = getPackageManager();
        String packageName = getPackageName();
        String versionName;
        try {
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = VERSION_UNAVAILABLE;
        }

        new MaterialDialog.Builder(this)
                .title(getString(R.string.app_name) + " " + versionName)
                .content(Html.fromHtml(getString(R.string.about_body)))
                .contentLineSpacing(1)
                .positiveText(android.R.string.ok)
                .iconRes(R.drawable.ic_launcher)
                .show();
    }

    private void showImageFullscreen(int position) {
        webCam = (WebCam) mAdapter.getItem(position);
        Intent fullScreenIntent = new Intent(getApplicationContext(), FullScreenImage.class);
        fullScreenIntent.putExtra("url", webCam.getUrl());
        fullScreenIntent.putExtra("zoom", zoom);
        fullScreenIntent.putExtra("autoRefresh", autoRefresh);
        fullScreenIntent.putExtra("interval", autoRefreshInterval);
        startActivity(fullScreenIntent);
    }

    private void showAddDialog() {
        DialogFragment newFragment = AddDialog.newInstance(this);
        newFragment.show(getFragmentManager(), "AddDialog");
    }

    private void showEditDialog(int position) {
        DialogFragment newFragment = EditDialog.newInstance(this);

        webCam = (WebCam) mAdapter.getItem(position);
        Bundle bundle = new Bundle();
        bundle.putLong("id", webCam.getId());
        bundle.putInt("position", position);
        newFragment.setArguments(bundle);

        newFragment.show(getFragmentManager(), "EditDialog");
    }

    @Override
    public void webCamAdded(WebCam wc, long[] category_ids, boolean share) {
        synchronized (sDataLock) {
            if (category_ids != null) {
                wc.setId(db.createWebCam(wc, category_ids));
            }
            else {
                wc.setId(db.createWebCam(wc, null));
            }
            db.closeDB();
        }
        BackupManager backupManager = new BackupManager(this);
        backupManager.dataChanged();

        mAdapter.addItem(mAdapter.getItemCount(), wc);
        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);

        checkAdapterIsEmpty();

        if (share) {
            sendEmail(wc);
        }
        else saveDone();
    }

    @Override
    public void webCamEdited(int position, WebCam wc, long[] category_ids, boolean share) {
        synchronized (sDataLock) {
            if (category_ids != null) {
                db.updateWebCam(wc, category_ids);
            }
            else {
                db.updateWebCam(wc, null);
            }
            db.closeDB();
        }
        BackupManager backupManager = new BackupManager(this);
        backupManager.dataChanged();

        mAdapter.modifyItem(position,wc);

        if (share) {
            sendEmail(wc);
        }
        else saveDone();
    }

    @Override
    public void webCamDeleted(long id, int position) {
        synchronized (sDataLock) {
            db.deleteWebCam(id);
            db.closeDB();
        }
        BackupManager backupManager = new BackupManager(this);
        backupManager.dataChanged();

        if (mAdapter != null && mAdapter.getItemCount() > 0) {
            mAdapter.removeItem(mAdapter.getItemAt(position));
        }

        checkAdapterIsEmpty();

        delDone();
    }

    @Override
    public void invokeReload() {
        reInitializeAdapter(0);
        initDrawer();
    }

    private void saveDone() {
        Snackbar.with(getApplicationContext())
                .text(R.string.dialog_positive_toast_message)
                .actionLabel(R.string.dismiss)
                .actionColor(getResources().getColor(R.color.yellow))
                .show(this);
    }

    private void delDone() {
        Snackbar.with(getApplicationContext())
                .text(R.string.action_deleted)
                .actionLabel(R.string.dismiss)
                .actionColor(getResources().getColor(R.color.yellow))
                .show(this);
    }

    private void refreshDone() {
        Snackbar.with(getApplicationContext())
                .text(R.string.refresh_done)
                .actionLabel(R.string.dismiss)
                .actionColor(getResources().getColor(R.color.yellow))
                .duration(Snackbar.SnackbarDuration.LENGTH_SHORT)
                .show(this);
    }

    // Pull To Refresh 2/2
    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeLayout.setRefreshing(false);
                refresh();
                refreshDone();
            }
        }, 600);
    }

    private void refresh() {
        Utils.deletePicassoCache(getApplicationContext().getCacheDir());
        mAdapter.notifyDataSetChanged();
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

    private void sendEmail(WebCam webCam) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto","cz840311@gmail.com", null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "New WebCam for approval");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "WebCam Name: " + webCam.getName()
                + "\n" + "WebCam URL: " + webCam.getUrl());

        List<ResolveInfo> list = getPackageManager().queryIntentActivities(emailIntent, 0);
        if(list.isEmpty()) {
            noEmailClientsFound();
        }
        else {
            try {
                startActivity(Intent.createChooser(emailIntent, getString(R.string.send_via_email)));
            } catch (android.content.ActivityNotFoundException ex) {
                noEmailClientsFound();
            }
        }
    }

    private void noEmailClientsFound() {
        new MaterialDialog.Builder(this)
                .title(R.string.oops)
                .content(getString(R.string.no_email_clients_installed))
                .positiveText(android.R.string.ok)
                .show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && fullScreen) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void goFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void loadPref(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        firstRun = preferences.getBoolean("pref_first_run", true);
        fullScreen = preferences.getBoolean("pref_full_screen", false);
        autoRefresh = preferences.getBoolean("pref_auto_refresh", false);
        autoRefreshInterval = preferences.getInt("pref_auto_refresh_interval", 30000);
        autoRefreshFullScreenOnly = preferences.getBoolean("pref_auto_refresh_fullscreen", false);
        zoom = preferences.getFloat("pref_zoom", 2);
        selectedCategory = preferences.getInt("pref_selected_category", 0);
        selectedCategoryName = preferences.getString("pref_selectedCategoryName", allWebCamsString);
    }

    private void saveToPref(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("pref_first_run", firstRun);
        editor.putInt("pref_selected_category",selectedCategory);
        editor.putString("pref_selectedCategoryName",selectedCategoryName);
        editor.apply();
    }
}
