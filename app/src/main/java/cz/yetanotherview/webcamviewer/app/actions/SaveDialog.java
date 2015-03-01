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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import junit.framework.Assert;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cz.yetanotherview.webcamviewer.app.R;

public class SaveDialog extends DialogFragment {

    private File parentFolder;
    private File[] parentContents;
    private boolean canGoUp = false;
    private String topFolder;

    private String name;
    private String url;

    private MaterialDialog dialog;

    public SaveDialog() {
        parentFolder = Environment.getExternalStorageDirectory();
        topFolder = String.valueOf(parentFolder);
        parentContents = listFiles();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle bundle = this.getArguments();
        name = bundle.getString("name", "");
        url = bundle.getString("url", "");

        dialog = new MaterialDialog.Builder(getActivity())
                .title(parentFolder.getAbsolutePath())
                .items(getContentsArray())
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        if (canGoUp && which == 0) {
                            parentFolder = parentFolder.getParentFile();
                            canGoUp = !parentFolder.toString().equals(topFolder);

                        } else {
                            parentFolder = parentContents[canGoUp ? which - 1 : which];
                            canGoUp = true;
                        }
                        parentContents = listFiles();
                        dialog.setTitle(parentFolder.getAbsolutePath());
                        dialog.setItems(getContentsArray());
                    }
                })
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        new ConnectionTester().execute();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        dialog.dismiss();
                    }
                })
                .autoDismiss(false)
                .positiveText(R.string.choose)
                .negativeText(android.R.string.cancel)
                .build();
        return dialog;
    }

    private class ConnectionTester extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {

            try {
                URL mUrl = new URL(url);
                HttpURLConnection urlConn = (HttpURLConnection) mUrl.openConnection();
                urlConn.connect();
                Assert.assertEquals(HttpURLConnection.HTTP_OK, urlConn.getResponseCode());

                continueOnUiThread();
            }
            catch (IOException e) {
                System.err.println("Error creating HTTP connection");

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

    private static class FolderSorter implements Comparator<File> {
        @Override
        public int compare(File lhs, File rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    }

    String[] getContentsArray() {
        String[] results = new String[parentContents.length + (canGoUp ? 1 : 0)];
        if (canGoUp) results[0] = "...";
        for (int i = 0; i < parentContents.length; i++)
            results[canGoUp ? i + 1 : i] = parentContents[i].getName();
        return results;
    }

    File[] listFiles() {
        File[] contents = parentFolder.listFiles();
        List<File> results = new ArrayList<>();
        for (File fi : contents) {
            char fiFirstChar = fi.getName().charAt(0);
            if (fi.isDirectory() && fiFirstChar != '.') results.add(fi);
        }
        Collections.sort(results, new FolderSorter());
        return results.toArray(new File[results.size()]);
    }

    private void continueOnUiThread() {

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                dialog.dismiss();
                DialogFragment saveProgressDialog = new SaveProgressDialog();
                Bundle bundle = new Bundle();
                bundle.putString("name", name);
                bundle.putString("url", url);
                bundle.putString("path", parentFolder.getAbsolutePath());
                saveProgressDialog.setArguments(bundle);
                saveProgressDialog.show(getFragmentManager(), "SaveProgressDialog");
            }
        });
    }

    private void dialogUnavailable() {
        dialog.dismiss();
        new MaterialDialog.Builder(getActivity())
                .title(R.string.server_unavailable)
                .content(R.string.server_unavailable_summary)
                .positiveText(android.R.string.ok)
                .show();
    }
}