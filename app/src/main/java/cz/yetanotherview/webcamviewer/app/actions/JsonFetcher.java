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

package cz.yetanotherview.webcamviewer.app.actions;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
import java.util.Arrays;
import java.util.List;

import cz.yetanotherview.webcamviewer.app.MainActivity;
import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.Utils;
import cz.yetanotherview.webcamviewer.app.helper.DatabaseHelper;
import cz.yetanotherview.webcamviewer.app.model.Category;
import cz.yetanotherview.webcamviewer.app.model.WebCam;

public class JsonFetcher extends DialogFragment {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();
    private DatabaseHelper db;
    private List<WebCam> webCams;
    private ProgressDialog dialog;

    private static final String TAG = "JsonFetcher";
    private static final String SERVER_URL = "yetanotherview.cz";
    private static final String JSON_FILE_URL = "http://api." + SERVER_URL + "/webcams";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(getActivity());

        dialog = new ProgressDialog(getActivity(), getTheme());
        dialog.setTitle(R.string.importing_from_server);
        dialog.setMessage(getString(R.string.please_wait));
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        WebCamsFromJsonFetcher fetcher = new WebCamsFromJsonFetcher();
        fetcher.execute();

        return dialog;
    }

    private void handlePostsList(List<WebCam> webCams) {
        this.webCams = webCams;

        long categoryFromCurrentDate = db.createCategory(new Category(getString(R.string.imported) + " " + Utils.getFormattedDate()));
        synchronized (sDataLock) {
            for(WebCam webCam : webCams) {
                db.createWebCam(webCam, new long[] {categoryFromCurrentDate});
            }
        }
        db.closeDB();
        BackupManager backupManager = new BackupManager(getActivity());
        backupManager.dataChanged();
        dialog.dismiss();

        reloadMainActivity();
    }

    private class WebCamsFromJsonFetcher extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            Runtime runtime = Runtime.getRuntime();
            Process proc;
            try {
                proc = runtime.exec("ping -c 1 " + SERVER_URL);
                proc.waitFor();
                int exit = proc.exitValue();
                if (exit == 0) {
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

                                GsonBuilder gsonBuilder = new GsonBuilder();
                                Gson gson = gsonBuilder.create();
                                webCams = Arrays.asList(gson.fromJson(reader, WebCam[].class));
                                content.close();

                                handlePostsList(webCams);
                            } catch (Exception ex) {
                                Log.e(TAG, "Failed to parse JSON due to: " + ex);
                            }
                        } else {
                            Log.e(TAG, "Server responded with status code: " + statusLine.getStatusCode());
                        }
                    } catch(Exception ex) {
                        Log.e(TAG, "Failed to send HTTP POST request due to: " + ex);
                    }
                } else {
                    dialog.dismiss();
                    this.publishProgress();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            dialogUnavailable();
        }
    }

    private void dialogUnavailable() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.server_unavailable)
                .content(R.string.server_unavailable_summary)
                .positiveText(android.R.string.ok)
                .show();
    }

    private void reloadMainActivity() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
