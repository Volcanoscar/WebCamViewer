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
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
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
 * Edit dialog fragment
 */
public class EditDialog extends DialogFragment {

    private EditText mWebcamName;
    private EditText mWebcamUrl;
    private WebCam webCam;
    private WebCamListener mOnAddListener;
    private View positiveAction;

    private DatabaseHelper db;
    private List<Category> allCategories;
    private Category category;

    private Button webcamCategoryButton;
    private String[] items;
    private long[] category_ids;
    private long[] webcam_category_ids;
    private Integer[] checked;

    private long id;
    private int pos;
    private int status;

    private int position;

    public EditDialog() {
    }

    public static EditDialog newInstance(WebCamListener listener) {
        EditDialog frag = new EditDialog();
        frag.setOnAddListener(listener);
        return frag;
    }

    public void setOnAddListener(WebCamListener onAddListener) {
        mOnAddListener = onAddListener;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle bundle = this.getArguments();
        id = bundle.getLong("id", 0);
        position = bundle.getInt("position", 0);

        db = new DatabaseHelper(getActivity());
        webCam = db.getWebCam(id);
        allCategories = db.getAllCategories();
        webcam_category_ids = db.getWebCamCategoriesIds(webCam.getId());
        db.closeDB();

        pos = webCam.getPosition();
        status = webCam.getStatus();

        int[] ids = new int[allCategories.size()];
        items = new String[allCategories.size()];
        int count = 0;
        for (Category category : allCategories) {
            ids [count] = category.getId();
            items[count] = category.getcategoryName();
            count++;
        }

        checked = new Integer[webcam_category_ids.length];
        StringBuilder checkedNames = new StringBuilder();
        int count2 = 0;
        for (int i=0; i < ids.length; i++) {
            for (long webcam_category_id : webcam_category_ids) {
                if (ids[i] == webcam_category_id) {
                    checkedNames.append("[");
                    checkedNames.append(items[i]);
                    checkedNames.append("] ");

                    checked[count2] = i;
                    count2++;
                }
            }
        }

        View view = getActivity().getLayoutInflater().inflate(R.layout.add_edit_dialog, null);

        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.action_edit)
                .customView(view, true)
                .positiveText(R.string.dialog_positive_text)
                .negativeText(android.R.string.cancel)
                .neutralText(R.string.action_delete)
                .callback(new MaterialDialog.FullCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {

                        webCam.setName(mWebcamName.getText().toString().trim());
                        webCam.setUrl(mWebcamUrl.getText().toString().trim());
                        webCam.setPosition(pos);
                        webCam.setStatus(status);
                        if (mOnAddListener != null)
                            mOnAddListener.webcamEdited(position, webCam, category_ids);
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                    }

                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        if (mOnAddListener != null)
                            mOnAddListener.webcamDeleted(id,position);
                    }
                }).build();

        mWebcamName = (EditText) view.findViewById(R.id.webcam_name);
        mWebcamName.setText(webCam.getName());
        mWebcamName.requestFocus();

        mWebcamUrl = (EditText) view.findViewById(R.id.webcam_url);
        mWebcamUrl.setText(webCam.getUrl());

        webcamCategoryButton = (Button) view.findViewById(R.id.webcam_category_button);
        if (allCategories.size() == 0 ) {
            webcamCategoryButton.setText(R.string.category_array_empty);
        }
        else {
            if (webcam_category_ids.length == 0) {
                webcamCategoryButton.setText(R.string.category_array_choose);
            }
            else webcamCategoryButton.setText(checkedNames);

            webcamCategoryButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    new MaterialDialog.Builder(getActivity())
                            .title(R.string.category_array_choose)
                            .autoDismiss(false)
                            .items(items)
                            .itemsCallbackMultiChoice(checked, new MaterialDialog.ListCallbackMulti() {
                                @Override
                                public void onSelection(MaterialDialog multidialog, Integer[] which, CharSequence[] text) {
                                }
                            })
                            .positiveText(android.R.string.ok)
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
                                        webcamCategoryButton.setText(str);
                                    } else {
                                        webcamCategoryButton.setText(R.string.category_array_choose);
                                    }
                                    checked = which;
                                    positiveAction.setEnabled(true);

                                    multidialog.dismiss();
                                }
                            })
                            .show();
                }

            });
        }

        positiveAction = dialog.getActionButton(DialogAction.POSITIVE);

        mWebcamName.addTextChangedListener(new TextWatcher() {
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
        mWebcamUrl.addTextChangedListener(new TextWatcher() {
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

        return dialog;
    }
}
