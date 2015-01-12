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

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.backup.BackupManager;
import android.content.Intent;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import cz.yetanotherview.webcamviewer.app.MainActivity;
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
    private List<WebCam> allWebCams;
    private long categoryFromCurrentDate;
    private int actionColor;
    private File extRootDirectory;
    private File inputDB;

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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(mActivity);

        extRootDirectory = Environment.getExternalStorageDirectory();
        actionColor = getResources().getColor(R.color.yellow);

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("DD.MM.yyyy HH:mm");
        String formattedDate = df.format(c.getTime());
        categoryFromCurrentDate = db.createCategory(new Category(getString(R.string.imported) + " " + formattedDate));

        File[] filesList = Utils.GetFiles(Utils.folderWCVPath);
        ArrayList<String> fileNames = Utils.getFileNames(filesList);

        if (fileNames != null) {
            items = fileNames.toArray(new String[fileNames.size()]);

            importDialog = new MaterialDialog.Builder(mActivity)
                    .title(R.string.choose_title)
                    .items(items)
                    .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                            inputName = (items[which]);
                            if (inputName.contains(Utils.extension)) {
                                importDialog = new MaterialDialog.Builder(mActivity)
                                        .title(getString(R.string.pref_delete_all) + "?")
                                        .content(R.string.import_summary)
                                        .positiveText(R.string.Yes)
                                        .negativeText(R.string.No)
                                        .callback(new MaterialDialog.Callback() {
                                            @Override
                                            public void onPositive(MaterialDialog dialog) {
                                                db.deleteAllWebCams();
                                                db.closeDB();
                                                importJson(inputName);
                                            }

                                            @Override
                                            public void onNegative(MaterialDialog dialog) {
                                                importJson(inputName);
                                            }
                                        })
                                        .show();
                            } else if (inputName.contains(Utils.oldExtension)) {
                                importDialog = new MaterialDialog.Builder(mActivity)
                                        .title(R.string.old_database_detected)
                                        .content(R.string.old_database_detected_summary)
                                        .positiveText(android.R.string.ok)
                                        .callback(new MaterialDialog.Callback() {
                                            @Override
                                            public void onPositive(MaterialDialog dialog) {
                                                importOldDb(inputName);
                                            }

                                            @Override
                                            public void onNegative(MaterialDialog materialDialog) {

                                            }
                                        })
                                        .show();
                            }
                        }
                    })
                    .show();
        }
        else importDialog = new MaterialDialog.Builder(mActivity)
                .title(R.string.nothing_to_import)
                .content(R.string.nothing_to_import_summary)
                .positiveText(android.R.string.ok)
                .show();

        return importDialog;
    }

    private void importJson(String fileName) {
        try {
            if (extRootDirectory.canRead()) {

                Gson gson = new Gson();
                BufferedReader bufferedReader = new BufferedReader(
                        new FileReader(Utils.folderWCVPath + fileName));

                allWebCams = Arrays.asList(gson.fromJson(bufferedReader, WebCam[].class));
                bufferedReader.close();

                synchronized (sDataLock) {
                    for(WebCam webCam : allWebCams) {
                        db.createWebCam(webCam, new long[] { categoryFromCurrentDate });
                    }
                }
                db.closeDB();
                BackupManager backupManager = new BackupManager(mActivity);
                backupManager.dataChanged();

                reloadMainActivity();
            }
        } catch (IOException e) {
            e.printStackTrace();
            snackBarImportFailed();
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

                reloadMainActivity();
                db = new DatabaseHelper(mActivity);
                allWebCams = db.getAllWebCams("id ASC");
                db.closeDB();
                exportJsonFromOldBackup(inputName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            snackBarImportFailed();
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void snackBarImportFailed() {
        Snackbar.with(mActivity)
                .text(R.string.import_failed)
                .actionLabel(R.string.dismiss)
                .actionColor(actionColor)
                .show(mActivity);
    }

    private void reloadMainActivity() {
        Intent intent = new Intent(mActivity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mActivity.startActivity(intent);
    }
}
