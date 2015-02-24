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

package cz.yetanotherview.webcamviewer.app;

import android.app.DialogFragment;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;
import com.nispok.snackbar.Snackbar;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.PicassoTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cz.yetanotherview.webcamviewer.app.actions.AboutDialog;
import cz.yetanotherview.webcamviewer.app.actions.AddDialog;
import cz.yetanotherview.webcamviewer.app.actions.EditDialog;
import cz.yetanotherview.webcamviewer.app.actions.JsonFetcherDialog;
import cz.yetanotherview.webcamviewer.app.actions.SaveDialog;
import cz.yetanotherview.webcamviewer.app.actions.SelectionDialog;
import cz.yetanotherview.webcamviewer.app.actions.WelcomeDialog;
import cz.yetanotherview.webcamviewer.app.adapter.CategoryAdapter;
import cz.yetanotherview.webcamviewer.app.fullscreen.FullScreenActivity;
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
    private Category allWebCamsCategory;
    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private WebCamAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private int numberOfColumns;
    private FloatingActionButton fab;
    private int mOrientation;

    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private CategoryAdapter mArrayAdapter;
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
    private boolean screenAlwaysOn;

    private MaterialDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // loading saved preferences
        loadPref();
        allWebCamsString = getString(R.string.all_webcams);

        // Inflating main layout
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

        // Screen Always on
        if (screenAlwaysOn){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Init DatabaseHelper for late use
        db = new DatabaseHelper(getApplicationContext());

        // Get current orientation
        mOrientation = getResources().getConfiguration().orientation;

        // Other core init
        initToolbar();
        loadCategories();
        initDrawer();
        initRecyclerView();
        initFab();
        initPullToRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(allWebCamsString);
        setSupportActionBar(mToolbar);
    }

    private void loadCategories() {
        allCategories = db.getAllCategories();
        for (Category category : allCategories) {
            category.setCount(db.getCategoryItemsCount(category.getId()));
        }

        allWebCamsCategory = new Category();
        allWebCamsCategory.setcategoryName(allWebCamsString);
        allWebCamsCategory.setCount(db.getWebCamCount());
        db.closeDB();
    }

    private void initDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerList = (ListView) findViewById(R.id.drawer);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);


        if (mDrawerList != null) {
            ArrayList<Category> arrayOfCategories = new ArrayList<>();
            mArrayAdapter = new CategoryAdapter(this, arrayOfCategories);

            mArrayAdapter.add(allWebCamsCategory);
            mArrayAdapter.addAll(allCategories);

            mDrawerList.setAdapter(mArrayAdapter);
            mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        }
        db.closeDB();

        if (mDrawerList.getCheckedItemPosition() == -1) {
            mDrawerList.setItemChecked(0, true);
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
        int mLayoutId = 1;
        if (numberOfColumns == 1 && mOrientation == 1) {
            mLayoutId = 1;
        }
        else if(numberOfColumns == 1 && mOrientation == 2) {
            mLayoutId = 2;
        }
        else if(numberOfColumns == 2 && mOrientation == 1) {
            mLayoutId = 2;
        }
        else if(numberOfColumns == 2 && mOrientation == 2) {
            mLayoutId = 3;
        }
        mLayoutManager = new GridLayoutManager(this, mLayoutId);

        mRecyclerView = (RecyclerView) findViewById(R.id.cardList);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        allWebCams = db.getAllWebCams(sortOrder);
        db.closeDB();

        mAdapter = new WebCamAdapter(this, allWebCams, mOrientation, mLayoutId);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setClickListener(new WebCamAdapter.ClickListener() {

            @Override
            public void onClick(View v, int position, boolean isEditClick) {
                if (isEditClick) {
                    showOptionsDialog(position);
                } else {
                    showImageFullscreen(position, false);
                }
            }
        });

        checkAdapterIsEmpty();
    }

    private void initFab() {
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(mRecyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChooseDialog();
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
            reInitializeRecyclerViewAdapter(position);
            selectedCategory = position;

            getSupportActionBar().setTitle(selectedCategoryName);
            mDrawerLayout.closeDrawers();
        }
    }

    private void reInitializeRecyclerViewAdapter(int position) {
        if (db.getWebCamCount() != 0) {
            allWebCams.clear();
            if (position == 0) {
                allWebCams = db.getAllWebCams(sortOrder);
                mAdapter.swapData(allWebCams);
                selectedCategoryName = allWebCamsString;
            }
            else {
                Category category = allCategories.get(position - 1);
                allWebCams = db.getAllWebCamsByCategory(category.getId(),sortOrder);
                mAdapter.swapData(allWebCams);
                selectedCategoryName = category.getcategoryName();
            }
            db.closeDB();
            saveToPref();
            checkAdapterIsEmpty();
        }
    }

    private void reInitializeDrawerListAdapter() {
        int checked = mDrawerList.getCheckedItemPosition();
        loadCategories();
        mArrayAdapter.clear();
        mArrayAdapter.add(allWebCamsCategory);
        mArrayAdapter.addAll(allCategories);
        mDrawerList.setAdapter(mArrayAdapter);
        mDrawerList.setItemChecked(checked, true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
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

        MenuItem dashboard = menu.findItem(R.id.action_dashboard);
        if (numberOfColumns == 1) {
            dashboard.setIcon(R.drawable.ic_action_action_dashboard);
        }
        else dashboard.setIcon(R.drawable.ic_action_action_view_day);

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
                refresh(false);
                break;

            //Sort view
            case R.id.action_sort:
                    showSortDialog();
                break;

            //View
            case R.id.action_dashboard:
                if (numberOfColumns == 1) {
                    numberOfColumns = 2;
                    item.setIcon(R.drawable.ic_action_action_view_day);
                }
                else if (numberOfColumns == 2) {
                    numberOfColumns = 1;
                    item.setIcon(R.drawable.ic_action_action_dashboard);
                }
                initRecyclerView();
                item.setChecked(true);
                saveToPref();
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

            //Help
            case R.id.menu_help:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://youtu.be/Xcp0j2vwbxI"));
                startActivity(browserIntent);
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSortDialog() {

        int whatMarkToCheck = 0;
        switch (sortOrder) {
            case "created_at ASC":
                whatMarkToCheck = 0;
                break;
            case "created_at DESC":
                whatMarkToCheck = 1;
                break;
            case "webcam_name COLLATE UNICODE":
                whatMarkToCheck = 2;
                break;
            case "webcam_name COLLATE UNICODE DESC":
                whatMarkToCheck = 3;
                break;
            default:
                break;
        }

        dialog = new MaterialDialog.Builder(this)
                .title(R.string.action_sort)
                .items(R.array.sort_values)
                .itemsCallbackSingleChoice(whatMarkToCheck, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        switch (which) {
                            case 0:
                                sortOrder = "created_at ASC";
                                break;
                            case 1:
                                sortOrder = "created_at DESC";
                                break;
                            case 2:
                                sortOrder = "webcam_name COLLATE UNICODE";
                                break;
                            case 3:
                                sortOrder = "webcam_name COLLATE UNICODE DESC";
                                break;
                            default:
                                break;
                        }
                        reInitializeRecyclerViewAdapter(selectedCategory);
                    }
                })
                .negativeText(R.string.close)
                .show();
    }

    private void showWelcomeDialog() {
        DialogFragment newFragment = new WelcomeDialog();
        newFragment.show(getFragmentManager(), "WelcomeDialog");
    }

    private void showAbout() {
        DialogFragment newFragment = new AboutDialog();
        newFragment.show(getFragmentManager(), "AboutDialog");
    }

    private void showImageFullscreen(int position, boolean map) {
        webCam = (WebCam) mAdapter.getItem(position);

        Intent intent = new Intent(this, FullScreenActivity.class);
        intent.putExtra("map", map);
        intent.putExtra("name", webCam.getName());
        intent.putExtra("url", webCam.getUrl());
        intent.putExtra("coordinates", webCam.getLatitude() + "," + webCam.getLongitude());
        intent.putExtra("zoom", zoom);
        intent.putExtra("fullScreen", fullScreen);
        intent.putExtra("autoRefresh", autoRefresh);
        intent.putExtra("interval", autoRefreshInterval);
        intent.putExtra("screenAlwaysOn", screenAlwaysOn);

        startActivity(intent);
    }

    private void showOptionsDialog(final int position) {
        webCam = (WebCam) mAdapter.getItem(position);

        String[] options_values = getResources().getStringArray(R.array.options_values);
        if (webCam.getUniId() != 0) {
            options_values[6] = getString(R.string.report_problem);
        }
        else {
            options_values[6] = getString(R.string.submit_to_appr);
        }

        dialog = new MaterialDialog.Builder(this)
                .items(options_values)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        switch (which) {
                            case 0:
                                showEditDialog(position);
                                break;
                            case 1:
                                webCamDeleted(webCam.getId(), position);
                                break;
                            case 2:
                                showImageFullscreen(position, false);
                                break;
                            case 3:
                                DialogFragment newFragment = new SaveDialog();
                                Bundle bundle = new Bundle();
                                bundle.putString("name", webCam.getName());
                                bundle.putString("url", webCam.getUrl());
                                newFragment.setArguments(bundle);
                                newFragment.show(getFragmentManager(), "SaveDialog");
                                break;
                            case 4:

                                break;
                            case 5:
                                showImageFullscreen(position, true);
                                break;
                            case 6:
                                if (webCam.getUniId() != 0) {
                                    sendEmail(webCam, true);
                                }
                                else sendEmail(webCam, false);
                                break;
                            default:
                                break;
                        }

                    }
                })
                .show();
    }

    private void showChooseDialog() {
        dialog = new MaterialDialog.Builder(this)
                .title(R.string.add_options)
                .items(R.array.choose_values)
                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        if (which == 0) {
                            DialogFragment selection = new SelectionDialog();
                            selection.show(getFragmentManager(), "SelectionDialog");
                        }
                        else if (which == 1){
                            showAddDialog();
                        }
                    }
                })
                .positiveText(R.string.choose)
                .show();
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
        reInitializeDrawerListAdapter();

        if (share) {
            sendEmail(wc, false);
        }
        else saveDone();
    }

    @Override
    public void webCamEdited(int position, WebCam wc, long[] category_ids) {
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
        reInitializeDrawerListAdapter();

        saveDone();
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
        reInitializeDrawerListAdapter();

        fab.show();
        delDone();
    }

    @Override
    public void invokeReload() {
        reInitializeRecyclerViewAdapter(0);
        reInitializeDrawerListAdapter();
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

    private void refreshIsRunning() {
        Snackbar.with(getApplicationContext())
                .text(R.string.refresh_is_running)
                .actionLabel(R.string.dismiss)
                .actionColor(getResources().getColor(R.color.yellow))
                .duration(Snackbar.SnackbarDuration.LENGTH_SHORT)
                .show(this);
    }

    private void nothingToRefresh() {
        Snackbar.with(getApplicationContext())
                .text(R.string.nothing_to_refresh)
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
                refresh(false);
            }
        }, 600);
    }

    private void refresh(boolean fromAutoRefresh) {
        PicassoTools.clearCache(Picasso.with(getApplicationContext()));
        mAdapter.notifyDataSetChanged();
        if (!fromAutoRefresh) {
            if (mAdapter.getItemCount() != 0) {
                refreshIsRunning();
            }
            else nothingToRefresh();
        }
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
                            refresh(true);
                        } catch (Exception e) {
                            // Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, interval);
    }

    private void sendEmail(WebCam webCam, boolean fromCommunityList) {

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "cz840311@gmail.com", null));

        if (fromCommunityList) {
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Something is wrong");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "WebCam UniID: " + webCam.getUniId()
                    + "\n" + "WebCam Name: " + webCam.getName());
        }
        else {
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "New WebCam for approval");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "WebCam Name: " + webCam.getName()
                    + "\n" + "WebCam URL: " + webCam.getUrl() + "\n" + "WebCam Coordinates: "
                    + webCam.getLatitude() + " - " + webCam.getLongitude());
        }

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
        dialog = new MaterialDialog.Builder(this)
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
        numberOfColumns = preferences.getInt("number_of_columns", 1);
        fullScreen = preferences.getBoolean("pref_full_screen", false);
        autoRefresh = preferences.getBoolean("pref_auto_refresh", false);
        autoRefreshInterval = preferences.getInt("pref_auto_refresh_interval", 30000);
        autoRefreshFullScreenOnly = preferences.getBoolean("pref_auto_refresh_fullscreen", false);
        zoom = preferences.getFloat("pref_zoom", 2);
        selectedCategory = preferences.getInt("pref_selected_category", 0);
        selectedCategoryName = preferences.getString("pref_selectedCategoryName", allWebCamsString);
        screenAlwaysOn = preferences.getBoolean("pref_screen_always_on", false);
    }

    private void saveToPref(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("pref_first_run", firstRun);
        editor.putInt("number_of_columns", numberOfColumns);
        editor.putInt("pref_selected_category",selectedCategory);
        editor.putString("pref_selectedCategoryName",selectedCategoryName);
        editor.apply();
    }
}
