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
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

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
import cz.yetanotherview.webcamviewer.app.adapter.ManualSelectionAdapter;
import cz.yetanotherview.webcamviewer.app.helper.DatabaseHelper;
import cz.yetanotherview.webcamviewer.app.helper.NameComparator;
import cz.yetanotherview.webcamviewer.app.model.Category;
import cz.yetanotherview.webcamviewer.app.model.WebCam;

public class JsonFetcherDialog extends DialogFragment {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();
    private DatabaseHelper db;
    private List<WebCam> importWebCams;
    private List<WebCam> allWebCams;
    private ProgressDialog progressDialog;

    private int selection;
    private boolean noNewWebCams = true;

    private static final String TAG = "JsonFetcherDialog";
    private static final String JSON_FILE_URL = "http://api.yetanotherview.cz/webcams";
    private static final int latest = 14;

    private String plsWait;
    private String importProgress;
    private Activity mActivity;

    private EditText filterBox;
    private ManualSelectionAdapter manualSelectionAdapter;

    private ReloadInterface mListener;

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
        mListener = null;
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(mActivity);

        Bundle bundle = this.getArguments();
        selection = bundle.getInt("selection", 0);

        plsWait = getString(R.string.please_wait);

        progressDialog = new ProgressDialog(mActivity, getTheme());
        progressDialog.setTitle(R.string.importing_from_server);
        progressDialog.setMessage(plsWait);
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        WebCamsFromJsonFetcher fetcher = new WebCamsFromJsonFetcher();
        fetcher.execute();

        return progressDialog;
    }

    private class WebCamsFromJsonFetcher extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {

            try {
                URL url = new URL(JSON_FILE_URL);
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                urlConn.connect();

                Assert.assertEquals(HttpURLConnection.HTTP_OK, urlConn.getResponseCode());

                try {
                    //Create an HTTP client
                    HttpClient client = new DefaultHttpClient();
                    HttpPost post = new HttpPost(JSON_FILE_URL);

                    //Perform the request and check the status code
                    HttpResponse response = client.execute(post);
                    StatusLine statusLine = response.getStatusLine();
                    if(statusLine.getStatusCode() == 200) {
                        HttpEntity entity = response.getEntity();
                        InputStream content = entity.getContent();

                        try {
                            //Read the server response and attempt to parse it as JSON
                            Reader reader = new InputStreamReader(content);

                            Gson gson = new GsonBuilder().setDateFormat("dd.MM.yyyy HH:mm:ss, zzzz").create();
                            importWebCams = Arrays.asList(gson.fromJson(reader, WebCam[].class));
                            content.close();

                            handleWebCamList();
                        } catch (Exception ex) {
                            Log.e(TAG, "Failed to parse JSON due to: " + ex);
                        }
                    } else {
                        Log.e(TAG, "Server responded with status code: " + statusLine.getStatusCode());
                    }
                } catch(Exception ex) {
                    Log.e(TAG, "Failed to send HTTP POST request due to: " + ex);
                }
            } catch (IOException e) {
                System.err.println("Error creating HTTP connection");

                progressDialog.dismiss();
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

    private void handleWebCamList() {

        mActivity.runOnUiThread(new Runnable() {
            public void run() {

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
                SharedPreferences.Editor editor = preferences.edit();

                long now = Utils.getDate();
                importProgress = getString(R.string.import_progress);

                allWebCams = db.getAllWebCams("id ASC");

                synchronized (sDataLock) {
                    if (selection == 0) {
                        progressDialog.setMessage(importProgress + " " + plsWait);

                        int newWebCams = 0;
                        int duplicityWebCams = 0;
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
                        }
                        if (noNewWebCams) {
                            db.deleteCategory(categoryPopular, false);
                            noNewWebCamsDialog();
                        }
                        else {
                            editor.putLong("pref_last_fetch_popular", now);
                            mListener.invokeReload();
                            reportDialog(newWebCams, duplicityWebCams);
                        }
                    }
                    else if (selection == 1) {

                        MaterialDialog dialog = new MaterialDialog.Builder(mActivity)
                                .title(R.string.manual_selection)
                                .customView(R.layout.manual_selection_dialog, false)
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onPositive(MaterialDialog dialog) {
                                        progressDialog.setMessage(importProgress + " " + plsWait);

                                        String selected = mActivity.getString(R.string.selected);
                                        int newWebCams = 0;
                                        int duplicityWebCams = 0;
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
                                        }

                                        if (newWebCams + duplicityWebCams == 0) {
                                            db.deleteCategory(categorySelected, false);
                                        }

                                        mListener = (ReloadInterface) mActivity;
                                        mListener.invokeReload();
                                        reportDialog(newWebCams, duplicityWebCams);
                                    }

                                })
                                .positiveText(R.string.import_selected)
                                .build();

                        ListView manualSelectionList = (ListView) dialog.getCustomView().findViewById(R.id.filtered_list_view);
                        Collections.sort(importWebCams, new NameComparator());
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

                        dialog.show();
                    }
                    else if (selection == 2) {

                        List<String> list = new ArrayList<>();
                        List<String> listAllCountires = new ArrayList<>();
                        for (WebCam webCam : importWebCams) {
                            String country = webCam.getCountry();
                            listAllCountires.add(country);
                            if (!list.contains(country)) {
                                list.add(country);
                            }
                        }

                        Collections.sort(listAllCountires);
                        Log.d("", String.valueOf(listAllCountires));

                        Collections.sort(list);
                        String[] items = list.toArray(new String[list.size()]);

                        new MaterialDialog.Builder(mActivity)
                                .title(R.string.countries)
                                .items(items)
                                .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallback() {
                                    @Override
                                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                        progressDialog.setMessage(importProgress + " " + plsWait);

                                        int newWebCams = 0;
                                        int duplicityWebCams = 0;
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
                                        }

                                        mListener = (ReloadInterface) mActivity;
                                        mListener.invokeReload();
                                        reportDialog(newWebCams, duplicityWebCams);
                                    }
                                })
                                .positiveText(R.string.choose)
                                .show();
                    }
                    else if (selection == 3) {
                        progressDialog.setMessage(importProgress + " " + plsWait);

                        int newWebCams = 0;
                        int duplicityWebCams = 0;
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
                        }
                        if (noNewWebCams) {
                            db.deleteCategory(categoryLatest, false);
                            noNewWebCamsDialog();
                        }
                        else {
                            editor.putLong("pref_last_fetch_latest", now);
                            mListener.invokeReload();
                            reportDialog(newWebCams,duplicityWebCams);
                        }
                    }
                }
                db.closeDB();
                BackupManager backupManager = new BackupManager(mActivity);
                backupManager.dataChanged();

                editor.apply();
                progressDialog.dismiss();
            }
        });
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
