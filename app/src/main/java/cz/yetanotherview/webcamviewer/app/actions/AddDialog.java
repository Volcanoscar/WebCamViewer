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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.helper.DatabaseHelper;
import cz.yetanotherview.webcamviewer.app.helper.WebCamListener;
import cz.yetanotherview.webcamviewer.app.model.Category;
import cz.yetanotherview.webcamviewer.app.model.WebCam;

/**
 * Input dialog fragment
 */
public class AddDialog extends DialogFragment {

    private EditText mWebCamName;
    private EditText mWebCamUrl;
    private WebCam webCam;
    private WebCamListener mOnAddListener;
    private View positiveAction;

    private List<Category> allCategories;
    private Category category;

    private Button webCamCategoryButton;
    private String[] items;
    private long[] category_ids;

    private CheckBox shareCheckBox;

    private Activity mActivity;

    public AddDialog() {
    }

    public static AddDialog newInstance(WebCamListener listener) {
        AddDialog frag = new AddDialog();
        frag.setOnAddListener(listener);
        return frag;
    }

    private void setOnAddListener(WebCamListener onAddListener) {
        mOnAddListener = onAddListener;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String[] items = {getString(R.string.pref_import_from_server),getString(R.string.add_manually)};
        return new MaterialDialog.Builder(mActivity)
                .title(R.string.add_options)
                .items(items)
                .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        if (which == 0) {
                            DialogFragment selection = new SelectionDialog();
                            selection.show(getFragmentManager(), "SelectionDialog");
                        }
                        else if (which == 1){
                            addManuallyDialog();
                        }
                    }
                })
                .positiveText(R.string.choose)
                .build();
    }

    private void addManuallyDialog() {

        DatabaseHelper db = new DatabaseHelper(mActivity);
        allCategories = db.getAllCategories();
        db.closeDB();

        items = new String[allCategories.size()];
        int count = 0;
        for (Category category : allCategories) {
            items[count] = category.getcategoryName();
            count++;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(mActivity)
                .title(R.string.input_dialog_title)
                .customView(R.layout.add_edit_dialog, true)
                .positiveText(R.string.dialog_positive_text)
                .negativeText(android.R.string.cancel)
                .neutralText(R.string.how_to)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        boolean shareIsChecked = false;

                        webCam = new WebCam(
                                mWebCamName.getText().toString().trim(),
                                mWebCamUrl.getText().toString().trim(),
                                0,
                                0);

                        if (shareCheckBox.isChecked()) {
                            shareIsChecked = true;
                        }

                        if (mOnAddListener != null) {
                            mOnAddListener.webCamAdded(webCam, category_ids, shareIsChecked);
                        }

                    }

                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://youtu.be/liYtvXE0JTI"));
                        mActivity.startActivity(browserIntent);
                    }
                }).build();

        shareCheckBox = (CheckBox) dialog.getCustomView().findViewById(R.id.shareCheckBox);

        mWebCamName = (EditText) dialog.getCustomView().findViewById(R.id.webcam_name);
        mWebCamName.requestFocus();

        mWebCamUrl = (EditText) dialog.getCustomView().findViewById(R.id.webcam_url);

        webCamCategoryButton = (Button) dialog.getCustomView().findViewById(R.id.webcam_category_button);
        if (allCategories.size() == 0 ) {
            webCamCategoryButton.setText(R.string.category_array_empty);
        }
        else {
            webCamCategoryButton.setText(R.string.category_array_choose);
            webCamCategoryButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    new MaterialDialog.Builder(mActivity)
                            .title(R.string.webcam_category)
                            .autoDismiss(false)
                            .items(items)
                            .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMulti() {
                                @Override
                                public void onSelection(MaterialDialog multidialog, Integer[] which, CharSequence[] text) {
                                }
                            })
                            .positiveText(R.string.choose)
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog multidialog) {
                                    Integer[] which = multidialog.getSelectedIndices();

                                    if (which != null && which.length != 0) {
                                        StringBuilder str = new StringBuilder();

                                        category_ids = new long[which.length];
                                        int count = 0;

                                        for (Integer aWhich : which) {
                                            category = allCategories.get(aWhich);

                                            category_ids[count] = category.getId();
                                            count++;

                                            str.append("[");
                                            str.append(category.getcategoryName());
                                            str.append("] ");
                                        }
                                        webCamCategoryButton.setText(str);
                                    } else
                                        webCamCategoryButton.setText(R.string.category_array_choose);

                                    multidialog.dismiss();
                                }
                            })
                            .show();
                }

            });
        }

        positiveAction = dialog.getActionButton(DialogAction.POSITIVE);

        mWebCamUrl.addTextChangedListener(new TextWatcher() {
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
}