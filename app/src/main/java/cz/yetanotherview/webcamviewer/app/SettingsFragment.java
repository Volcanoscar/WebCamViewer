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

import java.util.List;

import cz.yetanotherview.webcamviewer.app.actions.ExportDialog;
import cz.yetanotherview.webcamviewer.app.actions.ImportDialog;
import cz.yetanotherview.webcamviewer.app.helper.DatabaseHelper;
import cz.yetanotherview.webcamviewer.app.model.Category;

public class SettingsFragment extends PreferenceFragment {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();

    private String inputName;
    private View positiveAction;
    private EditText input;
    private List<Category> allCategories;
    private Category category;
    private int actionColor;

    private Integer[] whichDelete;

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
        actionColor = getResources().getColor(R.color.yellow);

        setAutoRefreshInterval();
        setZoom();

        categoryAdd();
        categoryEdit();
        categoryDelete();

        importFromExt();
        exportToExt();
        deleteAllWebCams();
        cleanExtFolder();

        resetLastCheck();
    }

    private void setAutoRefreshInterval() {
        // Auto Refresh Interval OnPreferenceClickListener
        Preference pref_auto_refresh_interval = findPreference("pref_auto_refresh_interval");
        pref_auto_refresh_interval.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                sharedPref = getPreferenceManager().getSharedPreferences();
                int auto_refresh_interval_value = sharedPref.getInt("pref_auto_refresh_interval", 30000);

                View view = getActivity().getLayoutInflater().inflate(R.layout.enter_time_dialog, null);

                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.auto_refresh_interval)
                        .customView(view, true)
                        .positiveText(R.string.dialog_positive_text)
                        .negativeText(android.R.string.cancel)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                int inputTime = Integer.parseInt(input.getText().toString().trim());

                                sharedPref.edit().putInt("pref_auto_refresh_interval", inputTime * 1000).apply();

                                Snackbar.with(getActivity().getApplicationContext())
                                        .text(R.string.dialog_positive_toast_message)
                                        .actionLabel(R.string.dismiss)
                                        .actionColor(actionColor)
                                        .show(getActivity());
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
        // setZoom OnPreferenceClickListener
        Preference pref_zoom = findPreference("pref_zoom");
        pref_zoom.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                    sharedPref = getPreferenceManager().getSharedPreferences();
                    float zoom = sharedPref.getFloat("pref_zoom", 2);
                    int selected = Math.round(zoom);

                    new MaterialDialog.Builder(getActivity())
                            .title(R.string.available_options)
                            .items(new CharSequence[]{
                                    getString(R.string.no_zoom),
                                    getString(R.string.zoom_2x),
                                    getString(R.string.zoom_3x),
                                    getString(R.string.zoom_4x)})
                            .itemsCallbackSingleChoice(selected - 1, new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                    sharedPref.edit().putFloat("pref_zoom", which + 1).apply();
                                }
                            })
                            .positiveText(R.string.choose)
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
                        .customView(view, true)
                        .positiveText(R.string.dialog_positive_text)
                        .negativeText(android.R.string.cancel)
                        .callback(new MaterialDialog.ButtonCallback() {
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
                                        .actionColor(actionColor)
                                        .show(getActivity());
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
                            .title(R.string.webcam_category)
                            .items(items)
                            .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                                    Category editCategory = allCategories.get(which);
                                    categoryEditDialog(editCategory);

                                }
                            })
                            .positiveText(R.string.choose)
                            .show();
                } else Snackbar.with(getActivity().getApplicationContext())
                        .text("No categories found")
                        .actionLabel(R.string.dismiss)
                        .actionColor(actionColor)
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
                .customView(view, true)
                .positiveText(R.string.dialog_positive_text)
                .negativeText(android.R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
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
                                .actionColor(actionColor)
                                .show(getActivity());
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
                            .title(R.string.webcam_category)
                            .items(items)
                            .autoDismiss(false)
                            .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMulti() {
                                @Override
                                public void onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                                }
                            })
                            .positiveText(R.string.choose)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    whichDelete = dialog.getSelectedIndices();
                                    if (whichDelete != null) {
                                        if (whichDelete.length != 0) {
                                            categoryDeleteAlsoWebCamsDialog();
                                        }
                                    }
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
                else Snackbar.with(getActivity().getApplicationContext())
                        .text("No categories found")
                        .actionLabel(R.string.dismiss)
                        .actionColor(actionColor)
                        .show(getActivity());
                return true;
            }
        });
    }

    private void categoryDeleteAlsoWebCamsDialog (){

            new MaterialDialog.Builder(getActivity())
                    .title(R.string.action_delete)
                    .content(R.string.also_delete_webcams)
                    .positiveText(R.string.Yes)
                    .negativeText(R.string.No)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            categoryDeleteAlsoWebCams(true);
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            categoryDeleteAlsoWebCams(false);
                        }
                    })
                    .show();
    }

    private void categoryDeleteAlsoWebCams(boolean alsoWebCams){

        if (whichDelete != null && whichDelete.length != 0) {
            synchronized (SettingsFragment.sDataLock) {
                for (Integer aWhich : whichDelete) {
                    Category deleteCategory = allCategories.get(aWhich);
                    if (alsoWebCams) {
                        db.deleteCategory(deleteCategory.getId(), true);
                    }
                    else db.deleteCategory(deleteCategory.getId(), false);
                }
                db.closeDB();
            }
            BackupManager backupManager = new BackupManager(getActivity());
            backupManager.dataChanged();

            Snackbar.with(getActivity().getApplicationContext())
                    .text(R.string.action_deleted)
                    .actionLabel(R.string.dismiss)
                    .actionColor(actionColor)
                    .show(getActivity());
        }
    }

    private void exportToExt() {
        // Export to Ext OnPreferenceClickListener
        Preference pref_export_to_ext = findPreference("pref_export_to_ext");
        pref_export_to_ext.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                DialogFragment exportDialog = new ExportDialog();
                exportDialog.show(getFragmentManager(), "ExportDialog");
                return true;
            }
        });
    }

    private void importFromExt() {
        // Import from Ext OnPreferenceClickListener
        Preference pref_import_from_ext = findPreference("pref_import_from_ext");
        pref_import_from_ext.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                DialogFragment importDialog = new ImportDialog();
                importDialog.show(getFragmentManager(), "ImportDialog");
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
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                deleteAlsoCategoriesDialog();
                            }
                        })
                        .show();

                return true;
            }
        });
    }

    private void deleteAlsoCategoriesDialog (){

        new MaterialDialog.Builder(getActivity())
                .title(R.string.pref_delete_all)
                .content(R.string.also_delete_all_categories)
                .positiveText(R.string.Yes)
                .negativeText(R.string.No)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        deleteAlsoCategories(true);
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        deleteAlsoCategories(false);
                    }
                })
                .show();
    }

    private void deleteAlsoCategories(boolean alsoCategories){

            synchronized (SettingsFragment.sDataLock) {
                if (alsoCategories) {
                    db.deleteAllWebCams(true);
                }
                else {
                    db.deleteAllWebCams(false);
                }
                db.closeDB();
            }
            BackupManager backupManager = new BackupManager(getActivity());
            backupManager.dataChanged();

            Snackbar.with(getActivity().getApplicationContext())
                    .text(R.string.action_deleted)
                    .actionLabel(R.string.dismiss)
                    .actionColor(actionColor)
                    .show(getActivity());
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
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                Utils.cleanBackupFolder();

                                Snackbar.with(getActivity().getApplicationContext())
                                        .text(R.string.action_deleted)
                                        .actionLabel(R.string.dismiss)
                                        .actionColor(actionColor)
                                        .show(getActivity());
                            }
                        })
                        .show();

                return true;
            }
        });
    }

    private void resetLastCheck() {
        // Reset last check OnPreferenceClickListener
        Preference pref_reset_last_check = findPreference("pref_reset_last_check");
        pref_reset_last_check.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                new MaterialDialog.Builder(getActivity())
                        .title(R.string.pref_reset_last_check)
                        .content(R.string.reset_last_check_message)
                        .positiveText(R.string.Yes)
                        .negativeText(R.string.No)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                sharedPref = getPreferenceManager().getSharedPreferences();
                                sharedPref.edit().putLong("pref_last_fetch_popular", 0).apply();
                                sharedPref.edit().putLong("pref_last_fetch_latest", 0).apply();

                                Snackbar.with(getActivity().getApplicationContext())
                                        .text(R.string.done)
                                        .actionLabel(R.string.dismiss)
                                        .actionColor(actionColor)
                                        .show(getActivity());
                            }
                        })
                        .show();

                return true;
            }
        });
    }
}
