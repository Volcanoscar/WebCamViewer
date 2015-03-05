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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.nispok.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.Utils;
import cz.yetanotherview.webcamviewer.app.helper.DatabaseHelper;
import cz.yetanotherview.webcamviewer.app.model.Category;
import cz.yetanotherview.webcamviewer.app.model.WebCam;

public class ImportDialog extends DialogFragment {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();

    private MaterialDialog importDialog;
    private String[] items;
    private String inputName;
    private String imported;
    private List<WebCam> allWebCams;
    private int actionColor;
    private File extRootDirectory;
    private File inputDB;

    private MaterialDialog progressDialog;
    private int maxProgressValue;

    private DatabaseHelper db;
    private Activity mActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(mActivity);

        extRootDirectory = Environment.getExternalStorageDirectory();
        actionColor = getResources().getColor(R.color.yellow);

        File[] filesList = Utils.getFiles(Utils.folderWCVPath);
        ArrayList<String> fileNames = Utils.getFileNames(filesList);

        imported = getString(R.string.imported);

        if (fileNames.size() != 0) {
            items = fileNames.toArray(new String[fileNames.size()]);
            importDialog = new MaterialDialog.Builder(mActivity)
                    .title(R.string.external_files)
                    .items(items)
                    .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                            if (which >= 0) {
                                inputName = (items[which]);
                                if (inputName.contains(Utils.extension)) {
                                    importDialog = new MaterialDialog.Builder(mActivity)
                                            .title(getString(R.string.pref_delete_all) + "?")
                                            .content(R.string.import_summary)
                                            .positiveText(R.string.Yes)
                                            .negativeText(R.string.No)
                                            .callback(new MaterialDialog.ButtonCallback() {
                                                @Override
                                                public void onPositive(MaterialDialog dialog) {
                                                    new importJsonBackgroundTask().execute(inputName,"clean");
                                                }

                                                @Override
                                                public void onNegative(MaterialDialog dialog) {
                                                    new importJsonBackgroundTask().execute(inputName,"not");
                                                }
                                            })
                                            .show();
                                } else if (inputName.contains(Utils.oldExtension)) {
                                    importDialog = new MaterialDialog.Builder(mActivity)
                                            .title(R.string.old_database_detected)
                                            .content(R.string.old_database_detected_summary)
                                            .positiveText(android.R.string.ok)
                                            .callback(new MaterialDialog.ButtonCallback() {
                                                @Override
                                                public void onPositive(MaterialDialog dialog) {
                                                    importOldDb(inputName);
                                                }

                                            })
                                            .show();
                                }
                            }
                        }
                    })
                    .positiveText(R.string.choose)
                    .build();
        }
        else importDialog = new MaterialDialog.Builder(mActivity)
                .title(R.string.nothing_to_import)
                .content(R.string.nothing_to_import_summary)
                .positiveText(android.R.string.ok)
                .build();

        return importDialog;
    }

    private class importJsonBackgroundTask extends AsyncTask<String, Integer, Long> {

        @Override
        protected Long doInBackground(String... texts) {

            String text = texts[0];
            if (texts[1].contains("clean")) {
                db.deleteAllWebCams(true);
                db.closeDB();
            }

            try {
                if (extRootDirectory.canRead()) {

                    Gson gson = new Gson();
                    BufferedReader bufferedReader = new BufferedReader(
                            new FileReader(Utils.folderWCVPath + text));

                    allWebCams = Arrays.asList(gson.fromJson(bufferedReader, WebCam[].class));
                    bufferedReader.close();

                    maxProgressValue = allWebCams.size();
                    showProgressDialog();

                    long categoryFromCurrentDate = db.createCategory(new Category(imported + " " + Utils.getDateString()));
                    synchronized (sDataLock) {
                        for(WebCam webCam : allWebCams) {
                            db.createWebCam(webCam, new long[] {categoryFromCurrentDate});
                            progressUpdate();
                        }
                    }
                    db.closeDB();
                    BackupManager backupManager = new BackupManager(mActivity);
                    backupManager.dataChanged();
                    snackBarImportDone();
                }
            } catch (IOException e) {
                e.printStackTrace();
                snackBarImportFailed();
            }

            return null;
        }
    }

    private void importOldDb(String fileName) {
        try {
            if (extRootDirectory.canRead()) {

                File currentDB = new File(mActivity.getDatabasePath(DatabaseHelper.DATABASE_NAME).getPath());
                inputDB = new File(Utils.folderWCVPath + fileName);
                synchronized (sDataLock) {
                    FileChannel src = new FileInputStream(inputDB).getChannel();
                    FileChannel dst = new FileOutputStream(currentDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
                BackupManager backupManager = new BackupManager(mActivity);
                backupManager.dataChanged();

                db = new DatabaseHelper(mActivity);
                allWebCams = db.getAllWebCams("id ASC");
                db.closeDB();
                exportJsonFromOldBackup(inputName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            snackBarImportFromOldDbFailed();
        }
    }

    private void exportJsonFromOldBackup(String fileName) {

        String newFileName = fileName.replace(Utils.oldExtension,"");
        try {
            if (extRootDirectory.canWrite()) {
                Gson gson = new Gson();
                String json = gson.toJson(allWebCams);

                FileWriter writer = new FileWriter(Utils.folderWCVPath + newFileName + Utils.extension);
                writer.write(json);
                writer.close();

                Utils.removeDB(inputDB);
                snackBarImportFromOldDbDone();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showProgressDialog() {

        mActivity.runOnUiThread(new Runnable() {
            public void run() {

                progressDialog = new MaterialDialog.Builder(mActivity)
                        .title(R.string.pref_import_from_ext)
                        .content(R.string.please_wait)
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

    private void snackBarImportDone() {

        mActivity.runOnUiThread(new Runnable() {
            public void run() {

                progressDialog.dismiss();
                Snackbar.with(mActivity)
                        .text(R.string.import_done)
                        .actionLabel(R.string.dismiss)
                        .actionColor(actionColor)
                        .show(mActivity);
            }
        });
    }

    private void snackBarImportFailed() {

        mActivity.runOnUiThread(new Runnable() {
            public void run() {

                progressDialog.dismiss();
                Snackbar.with(mActivity)
                        .text(R.string.import_failed)
                        .actionLabel(R.string.dismiss)
                        .actionColor(actionColor)
                        .show(mActivity);
            }
        });
    }

    private void snackBarImportFromOldDbFailed() {
        Snackbar.with(mActivity)
                .text(R.string.import_failed)
                .actionLabel(R.string.dismiss)
                .actionColor(actionColor)
                .show(mActivity);
    }

    private void snackBarImportFromOldDbDone() {
        Snackbar.with(mActivity)
                .text(R.string.import_done)
                .actionLabel(R.string.dismiss)
                .actionColor(actionColor)
                .show(mActivity);
    }
}
