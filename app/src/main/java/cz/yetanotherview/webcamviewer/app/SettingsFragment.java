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

import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cz.yetanotherview.webcamviewer.app.db.DatabaseHelper;
import cz.yetanotherview.webcamviewer.app.model.Category;

public class SettingsFragment extends PreferenceFragment {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();

    private String currentDBPath = "/data/" + "cz.yetanotherview.webcamviewer.app"
            + "/databases/" + DatabaseHelper.DATABASE_NAME;
    private String inputName;
    private String extension = ".wcv";
    private View positiveAction;
    private EditText input;
    private String[] items;
    private List<Category> allCategories;
    private Category category;

    private DatabaseHelper db;
    private SharedPreferences sharedPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Enable immersive mode only on Kitkat and up
        if (Build.VERSION.SDK_INT >= 19) {
            getPreferenceScreen().findPreference("pref_full_screen").setEnabled(true);
        }

        db = new DatabaseHelper(getActivity().getApplicationContext());

        setAutoRefresh();
        setZoom();

        categoryAdd();
        categoryEdit();
        categoryDelete();

        deleteAllWebCams();
        importFromExt();
        exportToExt();
        cleanExtFolder();

    }

    private void setAutoRefresh() {
        // Import from Ext OnPreferenceClickListener
        Preference pref_auto_refresh_interval = findPreference("pref_auto_refresh_interval");
        pref_auto_refresh_interval.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                sharedPref = getPreferenceManager().getSharedPreferences();
                int auto_refresh_interval_value = sharedPref.getInt("pref_auto_refresh_interval", 10000);

                View view = getActivity().getLayoutInflater().inflate(R.layout.enter_time_dialog, null);

                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.auto_refresh_interval)
                        .customView(view, false)
                        .positiveText(R.string.dialog_positive_text)
                        .negativeText(android.R.string.cancel)
                        .callback(new MaterialDialog.Callback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                int inputTime = Integer.parseInt(input.getText().toString().trim());

                                sharedPref.edit().putInt("pref_auto_refresh_interval", inputTime * 1000).apply();

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
                input.setText(String.valueOf(auto_refresh_interval_value / 1000));

                TextView info = (TextView) view.findViewById(R.id.time_message);
                info.setText(getString(R.string.auto_refresh_interval_summary) + ".");

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
    }

    private void setZoom() {
        // Import from Ext OnPreferenceClickListener
        Preference pref_zoom = findPreference("pref_zoom");
        pref_zoom.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                    sharedPref = getPreferenceManager().getSharedPreferences();
                    float zoom = sharedPref.getFloat("pref_zoom", 2);
                    int selected = Math.round(zoom);

                    new MaterialDialog.Builder(getActivity())
                            .title(R.string.choose_title)
                            .items(new CharSequence[]{
                                    String.valueOf(getString(R.string.no_zoom)),
                                    String.valueOf(getString(R.string.zoom_2x)),
                                    String.valueOf(getString(R.string.zoom_3x)),
                                    String.valueOf(getString(R.string.zoom_4x))})
                            .itemsCallbackSingleChoice(selected - 1, new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                    sharedPref.edit().putFloat("pref_zoom", which + 1).apply();
                                }
                            })
                            .show();

                return true;
            }
        });
    }

    private void categoryAdd() {
        // Category Add OnPreferenceClickListener
        Preference pref_category_add = findPreference("pref_category_add");
        pref_category_add.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference activity_preference) {

                View view = getActivity().getLayoutInflater().inflate(R.layout.enter_name_dialog, null);

                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.new_category_name)
                        .customView(view, false)
                        .positiveText(R.string.dialog_positive_text)
                        .negativeText(android.R.string.cancel)
                        .callback(new MaterialDialog.Callback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                inputName = input.getText().toString().trim();
                                synchronized (SettingsFragment.sDataLock) {
                                    Category category = new Category(inputName);
                                    db.createCategory(category);
                                    db.closeDB();
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
    }

    private void categoryEdit() {
        // Category Edit OnPreferenceClickListener
        Preference pref_category_edit = findPreference("pref_category_edit");
        pref_category_edit.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                allCategories = db.getAllCategories();

                String[] items = new String[allCategories.size()];
                int count = 0;
                for (Category category : allCategories) {
                    items[count] = category.getcategoryName();
                    count++;
                }

                if (allCategories.size() > 0) {
                    new MaterialDialog.Builder(getActivity())
                            .title(R.string.choose_title)
                            .items(items)
                            .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                                        Category editCategory = allCategories.get(which);
                                        categoryEditDialog(editCategory);

                                }
                            })
                            .show();
                } else Snackbar.with(getActivity().getApplicationContext())
                        .text("No categories found")
                        .actionLabel(R.string.dismiss)
                        .actionColor(getResources().getColor(R.color.yellow))
                        .show(getActivity());
                return true;
            }
        });
    }

    private void categoryEditDialog(Category editCategory) {

        this.category = editCategory;

        View view = getActivity().getLayoutInflater().inflate(R.layout.enter_name_dialog, null);

        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.new_category_name)
                .customView(view, false)
                .positiveText(R.string.dialog_positive_text)
                .negativeText(android.R.string.cancel)
                .callback(new MaterialDialog.Callback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        inputName = input.getText().toString().trim();
                        synchronized (SettingsFragment.sDataLock) {
                            category.setcategoryName(inputName);
                            db.updateCategory(category);
                            db.closeDB();
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
        input.setText(category.getcategoryName());

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
    }

    private void categoryDelete() {
        // Category Delete OnPreferenceClickListener
        Preference pref_category_delete = findPreference("pref_category_delete");
        pref_category_delete.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                allCategories = db.getAllCategories();

                String[] items = new String[allCategories.size()];
                int count = 0;
                for (Category category : allCategories) {
                    items[count] = category.getcategoryName();
                    count++;
                }

                if (allCategories.size() > 0) {
                    new MaterialDialog.Builder(getActivity())
                            .title(R.string.choose_title)
                            .items(items)
                            .autoDismiss(false)
                            .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMulti() {
                                @Override
                                public void onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                                }
                            })
                            .positiveText(android.R.string.ok)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    Integer[] which = dialog.getSelectedIndices();
                                    if (which != null && which.length != 0) {
                                        synchronized (SettingsFragment.sDataLock) {

                                            for (Integer aWhich : which) {
                                                Category deleteCategory = allCategories.get(aWhich);
                                                db.deleteCategory(deleteCategory, false);
                                            }
                                            db.closeDB();
                                        }
                                        BackupManager backupManager = new BackupManager(getActivity());
                                        backupManager.dataChanged();

                                        Snackbar.with(getActivity().getApplicationContext())
                                                .text(R.string.action_deleted)
                                                .actionLabel(R.string.dismiss)
                                                .actionColor(getResources().getColor(R.color.yellow))
                                                .show(getActivity());
                                    }
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
                else Snackbar.with(getActivity().getApplicationContext())
                        .text("No categories found")
                        .actionLabel(R.string.dismiss)
                        .actionColor(getResources().getColor(R.color.yellow))
                        .show(getActivity());
                return true;
            }
        });
    }

    private void deleteAllWebCams() {
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
                                db.deleteAllWebCams();
                                db.closeDB();
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
                        .show();

                return true;
            }
        });
    }

    private void exportToExt() {
        // Export to Ext OnPreferenceClickListener
        Preference pref_export_to_ext = findPreference("pref_export_to_ext");
        pref_export_to_ext.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

//                View view = getActivity().getLayoutInflater().inflate(R.layout.enter_name_dialog, null);
//
//                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
//                        .title(R.string.export_title)
//                        .customView(view)
//                        .positiveText(android.R.string.ok)
//                        .negativeText(android.R.string.cancel)
//                        .callback(new MaterialDialog.Callback() {
//                            @Override
//                            public void onPositive(MaterialDialog dialog) {
//                                inputName = input.getText().toString().trim();
////                                exportDB(inputName + extension);
//                            }
//
//                            @Override
//                            public void onNegative(MaterialDialog dialog) {
//                            }
//                        }).build();
//
//                input = (EditText) view.findViewById(R.id.input_name);
//                input.requestFocus();
//                input.setHint(R.string.export_input_sample);
//
//                TextView message = (TextView) view.findViewById(R.id.message);
////                message.setText(getString(R.string.export_message) + "\n" + Environment.getExternalStorageDirectory().toString() + folderName + "\n");
//
//                positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
//
//                input.addTextChangedListener(new TextWatcher() {
//                    @Override
//                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                    }
//                    @Override
//                    public void onTextChanged(CharSequence s, int start, int before, int count) {
//                        positiveAction.setEnabled(s.toString().trim().length() > 0);
//                    }
//                    @Override
//                    public void afterTextChanged(Editable s) {
//                    }
//                });
//
//                dialog.show();
//                positiveAction.setEnabled(false);

                return true;
            }
        });
    }

    private void importFromExt() {
        // Import from Ext OnPreferenceClickListener
        Preference pref_import_from_ext = findPreference("pref_import_from_ext");
        pref_import_from_ext.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

//                File[] filesList = GetFiles(Environment.getExternalStorageDirectory().toString() + Utils.folderWCV);
//                ArrayList<String> fileNames = getFileNames(filesList);
//
//                if (fileNames != null) {
//                    items = fileNames.toArray(new String[fileNames.size()]);
//
//                    new MaterialDialog.Builder(getActivity())
//                            .title(R.string.choose_title)
//                            .items(items)
//                            .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallback() {
//                                @Override
//                                public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
//
////                                    resolver.delete(CardCursorContract.CardCursor.CONTENT_URI, null, null);
//                                    inputName = (items[which]);
//                                    //importDB(inputName);
//                                }
//                            })
//                            .show();
//                }
//                else Snackbar.with(getActivity().getApplicationContext())
//                        .text(R.string.nothing_to_import)
//                        .actionLabel(R.string.dismiss)
//                        .actionColor(getResources().getColor(R.color.yellow))
//                        .show(getActivity());

                return true;
            }
        });
    }

    private void cleanExtFolder() {
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
                                Utils.cleanBackupFolder();
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

//    private void importDB(String backupDBPath) {
//        try {
//            File sd = Environment.getExternalStorageDirectory();
//            File data = Environment.getDataDirectory();
//            if (sd.canWrite()) {
//                File backupDB = new File(data, currentDBPath);
//                File currentDB = new File(sd + folderName, backupDBPath); // From SD directory.
//
//                FileChannel src = new FileInputStream(currentDB).getChannel();
//                FileChannel dst = new FileOutputStream(backupDB).getChannel();
//                dst.transferFrom(src, 0, src.size());
//                src.close();
//                dst.close();
//
//                Snackbar.with(getActivity().getApplicationContext())
//                        .text(R.string.import_done)
//                        .actionLabel(R.string.dismiss)
//                        .actionColor(getResources().getColor(R.color.yellow))
//                        .show(getActivity());
//            }
//        } catch (Exception e) {
//            Snackbar.with(getActivity().getApplicationContext())
//                    .text(R.string.import_failed)
//                    .actionLabel(R.string.dismiss)
//                    .actionColor(getResources().getColor(R.color.yellow))
//                    .show(getActivity());
//        }
//    }

//    private void exportDB(String backupDBPath) {
//
//        //creating a new folder for the database to be backuped to
//        File directory = new File(Environment.getExternalStorageDirectory() + folderName);
//
//        if (!directory.exists()) {
//            directory.mkdir();
//        }
//
//        try {
//            File sd = Environment.getExternalStorageDirectory();
//            File data = Environment.getDataDirectory();
//
//            if (sd.canWrite()) {
//                File currentDB = new File(data, currentDBPath);
//                File backupDB = new File(sd + folderName, backupDBPath);
//
//                FileChannel src = new FileInputStream(currentDB).getChannel();
//                FileChannel dst = new FileOutputStream(backupDB).getChannel();
//                dst.transferFrom(src, 0, src.size());
//                src.close();
//                dst.close();
//
//                Snackbar.with(getActivity().getApplicationContext())
//                        .text(R.string.export_done)
//                        .actionLabel(R.string.dismiss)
//                        .actionColor(getResources().getColor(R.color.yellow))
//                        .show(getActivity());
//            }
//        } catch (Exception e) {
//            Snackbar.with(getActivity().getApplicationContext())
//                    .text(R.string.export_failed)
//                    .actionLabel(R.string.dismiss)
//                    .actionColor(getResources().getColor(R.color.yellow))
//                    .show(getActivity());
//        }
//    }

}
