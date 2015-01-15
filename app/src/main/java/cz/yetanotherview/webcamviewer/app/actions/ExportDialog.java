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
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.nispok.snackbar.Snackbar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.Utils;
import cz.yetanotherview.webcamviewer.app.helper.DatabaseHelper;
import cz.yetanotherview.webcamviewer.app.model.WebCam;

public class ExportDialog extends DialogFragment {

    private View positiveAction;

    private String inputName;
    private EditText input;
    private List<WebCam> allWebCams;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DatabaseHelper db = new DatabaseHelper(getActivity().getApplicationContext());

        allWebCams = db.getAllWebCams("id ASC");
        db.closeDB();

        MaterialDialog dialog;
        if (allWebCams.size() != 0) {

            View view = getActivity().getLayoutInflater().inflate(R.layout.enter_name_dialog, null);

            dialog = new MaterialDialog.Builder(getActivity())
                    .title(R.string.export_title)
                    .customView(view, true)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .callback(new MaterialDialog.Callback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            inputName = input.getText().toString().trim();
                            exportJson(inputName);
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                        }
                    }).build();

            input = (EditText) view.findViewById(R.id.input_name);
            input.requestFocus();
            input.setHint(R.string.export_input_sample);

            TextView message = (TextView) view.findViewById(R.id.message);
            message.setText(getString(R.string.export_message) + "\n" + Utils.folderWCVPath + "\n");

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

            positiveAction.setEnabled(false);

        }
        else dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.nothing_to_export)
                .content(R.string.nothing_to_export_summary)
                .positiveText(android.R.string.ok)
                .build();

        return dialog;
    }

    private void exportJson(String fileName) {

        File exportDirectory = new File(Utils.folderWCVPath);

        if (!exportDirectory.exists()) {
            exportDirectory.mkdir();
        }

        try {
            File sd = Environment.getExternalStorageDirectory();
            if (sd.canWrite()) {
                Gson gson = new Gson();
                String json = gson.toJson(allWebCams);

                FileWriter writer = new FileWriter(Utils.folderWCVPath + fileName + Utils.extension);
                writer.write(json);
                writer.close();

                Snackbar.with(getActivity().getApplicationContext())
                        .text(R.string.export_done)
                        .actionLabel(R.string.dismiss)
                        .actionColor(getResources().getColor(R.color.yellow))
                        .show(getActivity());
            }
        } catch (IOException e) {
            e.printStackTrace();
            Snackbar.with(getActivity().getApplicationContext())
                    .text(R.string.export_failed)
                    .actionLabel(R.string.dismiss)
                    .actionColor(getResources().getColor(R.color.yellow))
                    .show(getActivity());
        }
    }
}
