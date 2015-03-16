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

package cz.yetanotherview.webcamviewer.app.actions;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import junit.framework.Assert;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.Utils;
import cz.yetanotherview.webcamviewer.app.adapter.CountryAdapter;
import cz.yetanotherview.webcamviewer.app.adapter.ManualSelectionAdapter;
import cz.yetanotherview.webcamviewer.app.helper.CountryNameComparator;
import cz.yetanotherview.webcamviewer.app.helper.DatabaseHelper;
import cz.yetanotherview.webcamviewer.app.helper.WebCamNameComparator;
import cz.yetanotherview.webcamviewer.app.model.Category;
import cz.yetanotherview.webcamviewer.app.model.Country;
import cz.yetanotherview.webcamviewer.app.model.WebCam;

public class JsonFetcherDialog extends DialogFragment {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();

    private DatabaseHelper db;
    private List<WebCam> importWebCams;
    private List<WebCam> allWebCams;
    private MaterialDialog initDialog;
    private MaterialDialog progressDialog;

    private int selection;
    private boolean noNewWebCams = true;
    private List<Country> countryList;
    private int newWebCams;
    private int duplicityWebCams;
    private String importProgress;
    private int maxProgressValue;

    private static final String TAG = "JsonFetcher";
    private static final String JSON_FILE_URL = "http://api.yetanotherview.cz/webcams";
    private static final int latest = 14;

    private Activity mActivity;
    private EditText filterBox;
    private ManualSelectionAdapter manualSelectionAdapter;
    private ReloadInterface mListener;

    protected Location mLastLocation;
    private double mLatitude;
    private double mLongitude;
    private SeekBar seekBar;
    private TextView seekBarText;
    private int seekBarProgress;
    private int seekBarCorrection;
    private String units;

    private BackupManager backupManager;

    public static interface ReloadInterface {
        public void invokeReload();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        mListener = (ReloadInterface) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(mActivity);
        allWebCams = db.getAllWebCams("id ASC");

        Bundle bundle = this.getArguments();
        selection = bundle.getInt("selection", 0);
        String plsWait = getString(R.string.please_wait);
        importProgress = getString(R.string.import_progress) + " " + plsWait;

        initDialog = new MaterialDialog.Builder(mActivity)
                .title(R.string.importing_from_server)
                .content(plsWait)
                .progress(true, 0)
                .build();

        backupManager = new BackupManager(mActivity);

        WebCamsFromJsonFetcher fetcher = new WebCamsFromJsonFetcher();
        fetcher.execute();

        return initDialog;
    }

    private class WebCamsFromJsonFetcher extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {

            try {
                URL url = new URL(JSON_FILE_URL);
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                InputStream content = new BufferedInputStream(urlConn.getInputStream());

                urlConn.connect();
                Assert.assertEquals(HttpURLConnection.HTTP_OK, urlConn.getResponseCode());

                try {
                    //Read the server response and attempt to parse it as JSON
                    Reader reader = new InputStreamReader(content);

                    Gson gson = new GsonBuilder().setDateFormat("dd.MM.yyyy HH:mm:ss, zzzz").create();
                    importWebCams = Arrays.asList(gson.fromJson(reader, WebCam[].class));
                    content.close();

                    // Swap dialogs
                    maxProgressValue = importWebCams.size();
                    if ((selection == 0) || (selection == 4 || (selection == 5))) {
                        swapProgressDialog();
                    }

                    // Handle WebCams importing task
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
                    SharedPreferences.Editor editor = preferences.edit();
                    long now = Utils.getDate();
                    newWebCams = 0;
                    duplicityWebCams = 0;

                        if (selection == 0) {

                            synchronized (sDataLock) {
                                long lastFetchPopular = preferences.getLong("pref_last_fetch_popular", 0);
                                long categoryPopular = db.createCategory(new Category(getString(R.string.popular) + " " + Utils.getDateString()));

                                for (WebCam webCam : importWebCams) {
                                    long webCamDateAdded = webCam.getDateAdded().getTime();
                                    long differenceBetweenLastFetch = lastFetchPopular - webCamDateAdded;

                                    if (webCam.isPopular() && differenceBetweenLastFetch < 0) {
                                        if (allWebCams.size() != 0) {
                                            boolean notFound = false;
                                            for (WebCam allWebCam : allWebCams) {
                                                if (webCam.getUniId() == allWebCam.getUniId()) {
                                                    db.createWebCamCategory(allWebCam.getId(), categoryPopular);
                                                    noNewWebCams = false;
                                                    notFound = false;
                                                    duplicityWebCams++;
                                                    break;
                                                }
                                                else notFound = true;
                                            }
                                            if (notFound) {
                                                db.createWebCam(webCam, new long[]{categoryPopular});
                                                noNewWebCams = false;
                                                newWebCams++;
                                            }
                                        }
                                        else {
                                            db.createWebCam(webCam, new long[]{categoryPopular});
                                            noNewWebCams = false;
                                            newWebCams++;
                                        }
                                    }
                                    progressUpdate();
                                }
                                if (noNewWebCams) {
                                    db.deleteCategory(categoryPopular, false);
                                }
                                else {
                                    editor.putLong("pref_last_fetch_popular", now);
                                    editor.apply();
                                }
                                showResult();
                            }
                            db.closeDB();
                            backupManager.dataChanged();
                        }
                        else if (selection == 1) {
                            getLastKnownLocation();

                            noNewWebCams = false;
                            handleNearSelection();
                        }
                        else if (selection == 2) {
                            Collections.sort(importWebCams, new WebCamNameComparator());

                            noNewWebCams = false;
                            handleManualSelection();
                        }
                        else if (selection == 3) {

                            List<String> tempList = new ArrayList<>();
                            List<String> listAllCountries = new ArrayList<>();
                            countryList = new ArrayList<>();
                            for (WebCam webCam : importWebCams) {

                                String countryName = webCam.getCountry();
                                listAllCountries.add(countryName);
                                if (!tempList.contains(countryName)) {
                                    tempList.add(countryName);

                                    Country country = new Country();
                                    country.setCountryName(countryName);
                                    String drawable =  countryName.toLowerCase().replace(" ", "_").replace(".","").replace("é","e").trim();
                                    country.setIcon(Utils.getResId(drawable, R.drawable.class));

                                    countryList.add(country);
                                }
                            }

                            Collections.sort(countryList, new CountryNameComparator());
                            Collections.sort(listAllCountries);

                            for (Country country : countryList) {
                                String countryName = country.getCountryName();
                                int occurrences = Collections.frequency(listAllCountries, countryName);
                                country.setCount(occurrences);
                            }

                            noNewWebCams = false;
                            handleCountrySelection();
                        }
                        else if (selection == 4) {

                            synchronized (sDataLock) {
                                long lastFetchLatest = preferences.getLong("pref_last_fetch_latest", 0);
                                long categoryLatest = db.createCategory(new Category(getString(R.string.latest) + " " + Utils.getDateString()));

                                for (WebCam webCam : importWebCams) {
                                    long webCamDateAdded = webCam.getDateAdded().getTime();
                                    long differenceBetweenLastFetch = lastFetchLatest - webCamDateAdded;
                                    int differenceBetweenDates = (int) ((now - webCamDateAdded)/86400000);

                                    if (differenceBetweenDates < latest && differenceBetweenLastFetch < 0) {
                                        if (allWebCams.size() != 0) {
                                            boolean notFound = false;
                                            for (WebCam allWebCam : allWebCams) {
                                                if (webCam.getUniId() == allWebCam.getUniId()) {
                                                    db.createWebCamCategory(allWebCam.getId(), categoryLatest);
                                                    noNewWebCams = false;
                                                    notFound = false;
                                                    duplicityWebCams++;
                                                    break;
                                                }
                                                else notFound = true;
                                            }
                                            if (notFound) {
                                                db.createWebCam(webCam, new long[]{categoryLatest});
                                                noNewWebCams = false;
                                                newWebCams++;
                                            }
                                        }
                                        else {
                                            db.createWebCam(webCam, new long[]{categoryLatest});
                                            noNewWebCams = false;
                                            newWebCams++;
                                        }
                                    }
                                    progressUpdate();
                                }
                                if (noNewWebCams) {
                                    db.deleteCategory(categoryLatest, false);
                                }
                                else {
                                    editor.putLong("pref_last_fetch_latest", now);
                                    editor.apply();
                                }
                                showResult();
                            }
                            db.closeDB();
                            backupManager.dataChanged();
                        }
                        else if (selection == 5) {
                            noNewWebCams = false;

                            synchronized (sDataLock) {
                                long categoryAll = db.createCategory(new Category(getString(R.string.all) + " " + Utils.getDateString()));
                                for (WebCam webCam : importWebCams) {
                                        if (allWebCams.size() != 0) {
                                            boolean notFound = false;
                                            for (WebCam allWebCam : allWebCams) {
                                                if (webCam.getUniId() == allWebCam.getUniId()) {
                                                    db.createWebCamCategory(allWebCam.getId(), categoryAll);
                                                    noNewWebCams = false;
                                                    notFound = false;
                                                    duplicityWebCams++;
                                                    break;
                                                }
                                                else notFound = true;
                                            }
                                            if (notFound) {
                                                db.createWebCam(webCam, new long[]{categoryAll});
                                                noNewWebCams = false;
                                                newWebCams++;
                                            }
                                        }
                                        else {
                                            db.createWebCam(webCam, new long[]{categoryAll});
                                            noNewWebCams = false;
                                            newWebCams++;
                                        }

                                    progressUpdate();
                                }
                                if (noNewWebCams) {
                                    db.deleteCategory(categoryAll, false);
                                }

                                showResult();
                            }
                            db.closeDB();
                            backupManager.dataChanged();
                        }

                } catch (Exception ex) {
                    Log.e(TAG, "Failed to parse JSON due to: " + ex);
                }
            } catch (IOException e) {
                System.err.println("Error creating HTTP connection");

                initDialog.dismiss();
                this.publishProgress();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            dialogUnavailable();
        }
    }

    private void swapProgressDialog() {

        mActivity.runOnUiThread(new Runnable() {
            public void run() {

                if ((selection == 0) || (selection == 4) || (selection == 5)) {
                    initDialog.dismiss();
                }
                progressDialog = new MaterialDialog.Builder(mActivity)
                        .title(R.string.importing_from_server)
                        .content(importProgress)
                        .progress(false, maxProgressValue)
                        .show();
            }
        });
    }

    private void progressUpdate() {

        mActivity.runOnUiThread(new Runnable() {
            public void run() {

                progressDialog.incrementProgress(1);
            }
        });
    }

    private void handleNearSelection() {

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                MaterialDialog dialog = new MaterialDialog.Builder(mActivity)
                        .title(R.string.select_radius)
                        .customView(R.layout.seekbar_dialog, false)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {

                                new nearSelectionBackgroundTask().execute();
                                swapProgressDialog();
                            }
                        })
                        .positiveText(android.R.string.ok)
                        .build();

                seekBar = (SeekBar) dialog.getCustomView().findViewById(R.id.seekbar_seek);
                seekBarText = (TextView) dialog.getCustomView().findViewById(R.id.seekbar_text);

                units = " km";
                String mLocale = getResources().getConfiguration().locale.getISO3Country();
                if (mLocale.equalsIgnoreCase(Locale.US.getISO3Country())) {
                    units = " mi";
                }

                seekBarCorrection = 10;
                seekBar.setMax(290);
                seekBarProgress = 50;
                seekBar.setProgress(seekBarProgress - seekBarCorrection);
                seekBarText.setText((seekBar.getProgress() + seekBarCorrection) + units);

                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    int val = seekBar.getProgress();

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        seekBarProgress = val;
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                        val = progressValue + seekBarCorrection;
                        seekBarText.setText(val + units);
                    }
                });

                initDialog.dismiss();
                dialog.show();
            }
        });
    }

    private class nearSelectionBackgroundTask extends AsyncTask<String, Integer, Long> {

        @Override
        protected Long doInBackground(String... texts) {

            synchronized (sDataLock) {
                String selected = mActivity.getString(R.string.nearby);
                long categoryNear = db.createCategory(new Category(selected + " " + Utils.getDateString()));
                float selectedDistance = seekBarProgress * 1000;

                for (WebCam webCam : importWebCams) {

                    float[] distance = new float[1];
                    Location.distanceBetween(webCam.getLatitude(), webCam.getLongitude(), mLatitude, mLongitude, distance);

                    if (distance[0] < selectedDistance) {
                        if (allWebCams.size() != 0) {
                            boolean notFound = false;
                            for (WebCam allWebCam : allWebCams) {
                                if (webCam.getUniId() == allWebCam.getUniId()) {
                                    db.createWebCamCategory(allWebCam.getId(), categoryNear);
                                    notFound = false;
                                    duplicityWebCams++;
                                    break;
                                }
                                else notFound = true;
                            }
                            if (notFound) {
                                db.createWebCam(webCam, new long[]{categoryNear});
                                newWebCams++;
                            }
                        }
                        else {
                            db.createWebCam(webCam, new long[]{categoryNear});
                            newWebCams++;
                        }
                    }
                    progressUpdate();
                }

                if (newWebCams + duplicityWebCams == 0) {
                    db.deleteCategory(categoryNear, false);
                }

                showResult();
            }
            db.closeDB();
            backupManager.dataChanged();

            return null;
        }
    }

    private void handleManualSelection() {

        mActivity.runOnUiThread(new Runnable() {
            public void run() {

                MaterialDialog dialog = new MaterialDialog.Builder(mActivity)
                        .title(R.string.manual_selection)
                        .customView(R.layout.manual_selection_dialog, false)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {

                                new manualSelectionBackgroundTask().execute();
                                swapProgressDialog();
                            }

                        })
                        .positiveText(R.string.import_selected)
                        .build();

                ListView manualSelectionList = (ListView) dialog.getCustomView().findViewById(R.id.filtered_list_view);
                manualSelectionAdapter = new ManualSelectionAdapter(mActivity, importWebCams);
                manualSelectionList.setAdapter(manualSelectionAdapter);

                filterBox = (EditText) dialog.getCustomView().findViewById(R.id.ms_filter);
                filterBox.addTextChangedListener(new TextWatcher() {

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        String text = s.toString().trim().toLowerCase(Locale.getDefault());
                        manualSelectionAdapter.getFilter().filter(text);
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count,
                                                  int after) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });

                initDialog.dismiss();
                dialog.show();
            }
        });
    }

    private class manualSelectionBackgroundTask extends AsyncTask<String, Integer, Long> {

        @Override
        protected Long doInBackground(String... texts) {

            synchronized (sDataLock) {
                String selected = mActivity.getString(R.string.selected);
                long categorySelected = db.createCategory(new Category(selected + " " + Utils.getDateString()));

                for (WebCam webCam : importWebCams) {
                    if (webCam.isSelected()) {
                        if (allWebCams.size() != 0) {
                            boolean notFound = false;
                            for (WebCam allWebCam : allWebCams) {
                                if (webCam.getUniId() == allWebCam.getUniId()) {
                                    db.createWebCamCategory(allWebCam.getId(), categorySelected);
                                    notFound = false;
                                    duplicityWebCams++;
                                    break;
                                }
                                else notFound = true;
                            }
                            if (notFound) {
                                db.createWebCam(webCam, new long[]{categorySelected});
                                newWebCams++;
                            }
                        }
                        else {
                            db.createWebCam(webCam, new long[]{categorySelected});
                            newWebCams++;
                        }
                    }
                    progressUpdate();
                }

                if (newWebCams + duplicityWebCams == 0) {
                    db.deleteCategory(categorySelected, false);
                }

                showResult();
            }
            db.closeDB();
            backupManager.dataChanged();

            return null;
        }
    }

    private void handleCountrySelection() {

        mActivity.runOnUiThread(new Runnable() {
            public void run() {

                final MaterialDialog dialog = new MaterialDialog.Builder(mActivity)
                        .title(R.string.countries)
                        .adapter(new CountryAdapter(mActivity, countryList))
                        .build();

                ListView listView = dialog.getListView();
                if (listView != null) {
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                            Country country = countryList.get(position);
                            new countrySelectionBackgroundTask().execute(country.getCountryName());
                            dialog.dismiss();
                            swapProgressDialog();
                        }
                    });
                }

                initDialog.dismiss();
                dialog.show();
            }
        });
    }

    private class countrySelectionBackgroundTask extends AsyncTask<String, Integer, Long> {

        @Override
        protected Long doInBackground(String... texts) {

            synchronized (sDataLock) {
                String text = texts[0];
                long categoryCountry = db.createCategory(new Category(text + " " + Utils.getDateString()));

                for (WebCam webCam : importWebCams) {
                    if (webCam.getCountry().equals(text)) {
                        if (allWebCams.size() != 0) {
                            boolean notFound = false;
                            for (WebCam allWebCam : allWebCams) {
                                if (webCam.getUniId() == allWebCam.getUniId()) {
                                    db.createWebCamCategory(allWebCam.getId(), categoryCountry);
                                    notFound = false;
                                    duplicityWebCams++;
                                    break;
                                }
                                else notFound = true;
                            }
                            if (notFound) {
                                db.createWebCam(webCam, new long[]{categoryCountry});
                                newWebCams++;
                            }
                        }
                        else {
                            db.createWebCam(webCam, new long[]{categoryCountry});
                            newWebCams++;
                        }
                    }
                    progressUpdate();
                }

                showResult();
            }
            db.closeDB();
            backupManager.dataChanged();

            return null;
        }
    }

    private void showResult() {

        mActivity.runOnUiThread(new Runnable() {
            public void run() {

                progressDialog.dismiss();
                if (!noNewWebCams) {
                    if (selection == 1 && (newWebCams + duplicityWebCams == 0)) {
                        noNearbyWebCamsDialog();
                    }
                    else {
                        mListener = (ReloadInterface) mActivity;
                        mListener.invokeReload();
                        reportDialog(newWebCams, duplicityWebCams);
                    }
                }
                else {
                    noNewWebCamsDialog();
                }

            }
        });
    }

    private void getLastKnownLocation () {
        LocationManager locationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
        mLastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (mLastLocation != null) {
            mLatitude = Utils.roundDouble(mLastLocation.getLatitude(), 6);
            mLongitude = Utils.roundDouble(mLastLocation.getLongitude(), 6);
            Log.i(TAG, String.valueOf(mLatitude) + " " + String.valueOf(mLongitude));
        } else {
            Log.i(TAG, "No location detected");
        }
    }

    private void reportDialog(int newWebCams, int duplicityWebCams) {
        new MaterialDialog.Builder(mActivity)
                .title(R.string.report)
                .content(mActivity.getString(R.string.import_successfully) + "\n\n"
                        + mActivity.getString(R.string.new_webcams) + " " + newWebCams
                        + "\n" + mActivity.getString(R.string.reassigned) + " " + duplicityWebCams)
                .positiveText(android.R.string.ok)
                .show();
    }

    private void noNearbyWebCamsDialog() {
        new MaterialDialog.Builder(mActivity)
                .title(R.string.no_nearby_webcams)
                .content(R.string.no_nearby_webcams_summary)
                .positiveText(android.R.string.ok)
                .show();
    }

    private void noNewWebCamsDialog() {
        new MaterialDialog.Builder(mActivity)
                .title(R.string.no_new_webcams)
                .content(R.string.no_new_webcams_summary)
                .positiveText(android.R.string.ok)
                .show();
    }

    private void dialogUnavailable() {
        new MaterialDialog.Builder(mActivity)
                .title(R.string.server_unavailable)
                .content(R.string.server_unavailable_summary)
                .positiveText(android.R.string.ok)
                .show();
    }
}
