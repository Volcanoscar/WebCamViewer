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

package cz.yetanotherview.webcamviewer.actions;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.backup.BackupManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import cz.yetanotherview.webcamviewer.R;
import cz.yetanotherview.webcamviewer.db.DatabaseHelper;
import cz.yetanotherview.webcamviewer.helper.WebCamListener;
import cz.yetanotherview.webcamviewer.model.Webcam;

/**
 * Edit dialog fragment
 */
public class EditDialog extends DialogFragment {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();

    private EditText mWebcamName;
    private EditText mWebcamUrl;
    private Spinner spinner;
    private ArrayAdapter<CharSequence> categoryAdapter;
    private WebCamListener mOnAddListener;
    private View positiveAction;

    private Webcam webcam;
    private long id;
    private String mainHeader;
    private String resourceThumb;

    private int position;

    private DatabaseHelper db;

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

        db = new DatabaseHelper(getActivity().getApplicationContext());
        webcam = db.getWebcam(id);

        mainHeader = webcam.getName();
        resourceThumb = webcam.getUrl();

        View view = getActivity().getLayoutInflater().inflate(R.layout.add_edit_dialog, null);

        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.action_edit)
                .customView(view)
                .positiveText(R.string.dialog_positive_text)
                .negativeText(android.R.string.cancel)
                .neutralText(R.string.action_delete)
                .callback(new MaterialDialog.FullCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        editWebCam();
                        notifyWebCamEdited();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                    }

                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        notifyWebCamDeleted(id,position);
                    }
                }).build();

        mWebcamName = (EditText) view.findViewById(R.id.webcam_name);
        mWebcamName.setText(mainHeader);
        mWebcamName.requestFocus();

        mWebcamUrl = (EditText) view.findViewById(R.id.webcam_url);
        mWebcamUrl.setText(resourceThumb);

        spinner = (Spinner) view.findViewById(R.id.category_spinner);
        categoryAdapter = ArrayAdapter.createFromResource(getActivity(),R.array.category_array, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(categoryAdapter);

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

    private void notifyWebCamEdited() {
        if (mOnAddListener != null)
            mOnAddListener.webcamEdited();
    }

    private void notifyWebCamDeleted(long id, int position) {
        if (mOnAddListener != null)
            mOnAddListener.webcamDeleted(id,position);
    }

    private void editWebCam() {
        synchronized (EditDialog.sDataLock) {
            webcam.setName(mWebcamName.getText().toString());
            webcam.setUrl(mWebcamUrl.getText().toString());
            db.updateWebCam(webcam);
            db.closeDB();
        }
        BackupManager backupManager = new BackupManager(getActivity());
        backupManager.dataChanged();
    }
}
