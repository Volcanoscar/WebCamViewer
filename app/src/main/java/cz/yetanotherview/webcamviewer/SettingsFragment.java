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

package cz.yetanotherview.webcamviewer;

import android.app.backup.BackupManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import cz.yetanotherview.webcamviewer.db.DatabaseHelper;

public class SettingsFragment extends PreferenceFragment {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();

    private String currentDBPath = "/data/" + "cz.yetanotherview.webcamviewer.app"
            + "/databases/" + DatabaseHelper.DATABASE_NAME;
    private String folderName = "/WebCamViewer";
    private String inputName;
    private String extension = ".wcv";
    private View positiveAction;
    private EditText input;
    private String[] items;
    private ContentResolver resolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("",currentDBPath); //ToDo

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Enable immersive mode only on Kitkat and up
        if (Build.VERSION.SDK_INT >= 19) {
            getPreferenceScreen().findPreference("pref_full_screen").setEnabled(true);
        }

        // Category Add OnPreferenceClickListener
        Preference pref_category_add = findPreference("pref_category_add");
        pref_category_add.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference activity_preference) {

                View view = getActivity().getLayoutInflater().inflate(R.layout.enter_name_dialog, null);

                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.new_category_name)
                        .customView(view)
                        .positiveText(R.string.dialog_positive_text)
                        .negativeText(android.R.string.cancel)
                        .callback(new MaterialDialog.Callback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                inputName = input.getText().toString().trim();
                                String inputNameClean = inputName.replaceAll("[^a-zA-Z0-9]+", "").replaceAll("\\s+", "").toLowerCase();
                                synchronized (SettingsFragment.sDataLock) {
//                                    ContentValues newValues = new ContentValues();
//                                    newValues.put(CardCursorContract.CardCursor.KeyColumns.KEY_CATEGORY_NAME, inputName);
//                                    newValues.put(CardCursorContract.CardCursor.KeyColumns.KEY_CATEGORY, inputNameClean);
//                                    resolver = getActivity().getContentResolver();
//                                    resolver.insert(CardCursorContract.CardCursor.CONTENT_URI2, newValues);
                                }
                                BackupManager backupManager = new BackupManager(getActivity());
                                backupManager.dataChanged();

                                Snackbar.with(getActivity().getApplicationContext())
                                        .text(R.string.dialog_positive_toast_message)
                                        .actionLabel(R.string.dismiss)
                                        .actionColor(getResources().getColor(R.color.yellow))
                                        .show(getActivity());
                            }

                            @Override
                            public void onNegative(MaterialDialog dialog) {
                            }
                        }).build();

                input = (EditText) view.findViewById(R.id.input_name);
                input.requestFocus();
                input.setHint(R.string.new_category_hint);

                positiveAction = dialog.getActionButton(DialogAction.POSITIVE);

                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        positiveAction.setEnabled(s.toString().trim().length() > 0);
                    }
                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });

                dialog.show();
                positiveAction.setEnabled(false);

                return true;
            }
        });

        // Category Edit OnPreferenceClickListener

        // Category Delete OnPreferenceClickListener
        Preference pref_category_delete = findPreference("pref_category_delete");
        pref_category_delete.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {





                resolver = getActivity().getContentResolver();
//                String[] projection = { People._ID, People.NAME, People.NUMBER };
//                String sa1 = "%A%"; // contains an "A"
//                String name =  "Donald Duck' OR name = 'Mickey Mouse"; // notice the " and '
//                Cursor c = resolver.query(People.CONTENT_URI, projection,
//                        People.NAME + " = '" + name + "'",
//                        new String[] { sa1 }, null);
//
//                String[] result = new String[c.getCount()];
//                c.moveToFirst();
//                for(int i = 0; i < c.getCount(); i++){
//                    String row = c.getString(c.getColumnIndex(ReminderProvider.COLUMN_BODY));
//                    //You can here manipulate a single string as you please
//                    result[i] = row;
//                    c.moveToNext();
//                }



                new MaterialDialog.Builder(getActivity())
                        .title(R.string.choose_title)
                        .positiveText(android.R.string.ok)
                                //.items(result)
                        .itemsCallbackMultiChoice(null,new MaterialDialog.ListCallbackMulti() {
                            @Override
                            public void onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                                synchronized (SettingsFragment.sDataLock) {





//                                    resolver.delete(CardCursorContract.CardCursor.CONTENT_URI2, null, null);




                                }
                                BackupManager backupManager = new BackupManager(getActivity());
                                backupManager.dataChanged();

                                Snackbar.with(getActivity().getApplicationContext())
                                        .text(R.string.action_deleted)
                                        .actionLabel(R.string.dismiss)
                                        .actionColor(getResources().getColor(R.color.yellow))
                                        .show(getActivity());
                            }
                        })
                        .build()
                        .show();

                return true;
            }
        });

        // Delete all OnPreferenceClickListener
        Preference pref_delete_all = findPreference("pref_delete_all");
        pref_delete_all.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                new MaterialDialog.Builder(getActivity())
                        .title(R.string.pref_delete_all)
                        .content(R.string.are_you_sure)
                        .positiveText(R.string.Yes)
                        .negativeText(R.string.No)
                        .callback(new MaterialDialog.Callback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                resolver = getActivity().getContentResolver();
//                                resolver.delete(CardCursorContract.CardCursor.CONTENT_URI, null, null);

                                Snackbar.with(getActivity().getApplicationContext())
                                        .text(R.string.action_deleted)
                                        .actionLabel(R.string.dismiss)
                                        .actionColor(getResources().getColor(R.color.yellow))
                                        .show(getActivity());
                            }
                            @Override
                            public void onNegative(MaterialDialog dialog) {
                            }
                        })
                        .build()
                        .show();

                return true;
            }
        });

        // Import from Ext OnPreferenceClickListener
        Preference pref_import_from_ext = findPreference("pref_import_from_ext");
        pref_import_from_ext.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                File[] filesList = GetFiles(Environment.getExternalStorageDirectory().toString() + folderName);
                ArrayList<String> fileNames = getFileNames(filesList);

                if (fileNames != null) {
                    items = fileNames.toArray(new String[fileNames.size()]);

                    new MaterialDialog.Builder(getActivity())
                            .title(R.string.choose_title)
                            .positiveText(android.R.string.ok)
                            .items(items)
                            .itemsCallbackSingleChoice(0,new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                    resolver = getActivity().getContentResolver();
//                                    resolver.delete(CardCursorContract.CardCursor.CONTENT_URI, null, null);
                                    inputName = (items[which]);
                                    importDB(inputName);
                                }
                            })
                            .build()
                            .show();
                }
                else Snackbar.with(getActivity().getApplicationContext())
                        .text(R.string.nothing_to_import)
                        .actionLabel(R.string.dismiss)
                        .actionColor(getResources().getColor(R.color.yellow))
                        .show(getActivity());

                return true;
            }
        });

        // Export to Ext OnPreferenceClickListener
        Preference pref_export_to_ext = findPreference("pref_export_to_ext");
        pref_export_to_ext.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                View view = getActivity().getLayoutInflater().inflate(R.layout.enter_name_dialog, null);

                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.export_title)
                        .customView(view)
                        .positiveText(android.R.string.ok)
                        .negativeText(android.R.string.cancel)
                        .callback(new MaterialDialog.Callback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                inputName = input.getText().toString().trim();
                                exportDB(inputName + extension);
                            }

                            @Override
                            public void onNegative(MaterialDialog dialog) {
                            }
                        }).build();

                input = (EditText) view.findViewById(R.id.input_name);
                input.requestFocus();
                input.setHint(R.string.export_input_sample);

                TextView message = (TextView) view.findViewById(R.id.message);
                message.setText(getString(R.string.export_message) + "\n" + Environment.getExternalStorageDirectory().toString() + folderName + "\n");

                positiveAction = dialog.getActionButton(DialogAction.POSITIVE);

                input.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        positiveAction.setEnabled(s.toString().trim().length() > 0);
                    }
                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });

                dialog.show();
                positiveAction.setEnabled(false);

                return true;
            }
        });

        // Clean Folder OnPreferenceClickListener
        Preference pref_clean_folder = findPreference("pref_clean_folder");
        pref_clean_folder.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                new MaterialDialog.Builder(getActivity())
                        .title(R.string.pref_clean_folder)
                        .content(R.string.are_you_sure)
                        .positiveText(R.string.Yes)
                        .negativeText(R.string.No)
                        .callback(new MaterialDialog.Callback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                File folder = new File(Environment.getExternalStorageDirectory().toString() + folderName);
                                deleteRecursive(folder);

                                Snackbar.with(getActivity().getApplicationContext())
                                        .text(R.string.action_deleted)
                                        .actionLabel(R.string.dismiss)
                                        .actionColor(getResources().getColor(R.color.yellow))
                                        .show(getActivity());
                            }
                            @Override
                            public void onNegative(MaterialDialog dialog) {
                            }
                        })
                        .build()
                        .show();

                return true;
            }
        });
    }

    private File[] GetFiles(String DirectoryPath) {
        File f = new File(DirectoryPath);
        f.mkdirs();
        return f.listFiles();
    }

    private ArrayList<String> getFileNames(File[] file){
        ArrayList<String> arrayFiles = new ArrayList<String>();
        if (file.length == 0)
            return null;
        else {
            for (File aFile : file) arrayFiles.add(aFile.getName());
        }
        return arrayFiles;
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }

    private void importDB(String backupDBPath) {
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();
            if (sd.canWrite()) {
                File backupDB = new File(data, currentDBPath);
                File currentDB = new File(sd + folderName, backupDBPath); // From SD directory.

                FileChannel src = new FileInputStream(currentDB).getChannel();
                FileChannel dst = new FileOutputStream(backupDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();

                Snackbar.with(getActivity().getApplicationContext())
                        .text(R.string.import_done)
                        .actionLabel(R.string.dismiss)
                        .actionColor(getResources().getColor(R.color.yellow))
                        .show(getActivity());
            }
        } catch (Exception e) {
            Snackbar.with(getActivity().getApplicationContext())
                    .text(R.string.import_failed)
                    .actionLabel(R.string.dismiss)
                    .actionColor(getResources().getColor(R.color.yellow))
                    .show(getActivity());
        }
    }

    private void exportDB(String backupDBPath) {

        //creating a new folder for the database to be backuped to
        File directory = new File(Environment.getExternalStorageDirectory() + folderName);

        if (!directory.exists()) {
            directory.mkdir();
        }

        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            if (sd.canWrite()) {
                File currentDB = new File(data, currentDBPath);
                File backupDB = new File(sd + folderName, backupDBPath);

                FileChannel src = new FileInputStream(currentDB).getChannel();
                FileChannel dst = new FileOutputStream(backupDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();

                Snackbar.with(getActivity().getApplicationContext())
                        .text(R.string.export_done)
                        .actionLabel(R.string.dismiss)
                        .actionColor(getResources().getColor(R.color.yellow))
                        .show(getActivity());
            }
        } catch (Exception e) {
            Snackbar.with(getActivity().getApplicationContext())
                    .text(R.string.export_failed)
                    .actionLabel(R.string.dismiss)
                    .actionColor(getResources().getColor(R.color.yellow))
                    .show(getActivity());
        }
    }

}
