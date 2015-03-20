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

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.Utils;

public class SuggestionDialog extends DialogFragment {

    private EditText input;
    private String inputName;
    private View positiveAction;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.submit_suggestion)
                .customView(R.layout.enter_name_dialog, true)
                .positiveText(R.string.send_via_email)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        inputName = input.getText().toString().trim();

                        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                                "mailto", Utils.email, null));

                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "New Suggestion");
                        emailIntent.putExtra(Intent.EXTRA_TEXT, "Place: " + inputName);

                        List<ResolveInfo> list = getActivity().getPackageManager().queryIntentActivities(emailIntent, 0);
                        if (list.isEmpty()) {
                            noEmailClientsFound();
                        } else {
                            try {
                                startActivity(Intent.createChooser(emailIntent, getString(R.string.send_via_email)));
                            } catch (android.content.ActivityNotFoundException ex) {
                                noEmailClientsFound();
                            }
                        }

                    }
                })
                .build();

        input = (EditText) dialog.getCustomView().findViewById(R.id.input_name);
        input.requestFocus();
        input.setHint(R.string.submit_suggestion_hint);

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

        return dialog;
    }

    private void noEmailClientsFound() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.oops)
                .content(getString(R.string.no_email_clients_installed))
                .positiveText(android.R.string.ok)
                .show();
    }
}
