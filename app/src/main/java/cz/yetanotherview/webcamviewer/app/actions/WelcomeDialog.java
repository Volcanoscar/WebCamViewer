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
import android.os.Bundle;

import com.afollestad.materialdialogs.MaterialDialog;

import cz.yetanotherview.webcamviewer.app.R;

public class WelcomeDialog extends DialogFragment {

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.welcome)
                .content(R.string.welcome_content)
                .positiveText(R.string.import_webcams_now)
                .neutralText(R.string.show_me_help)
                .negativeText(R.string.no_thanks)
                .forceStacking(true)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        DialogFragment selection = new SelectionDialog();
                        selection.show(getFragmentManager(), "SelectionDialog");
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                    }

                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                    }
                })
                .build();
    }
}
