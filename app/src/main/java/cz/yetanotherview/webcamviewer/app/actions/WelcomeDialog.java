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

import com.afollestad.materialdialogs.MaterialDialog;

public class WelcomeDialog extends DialogFragment {

    private MaterialDialog dialog;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dialog = new MaterialDialog.Builder(getActivity())
                .title("Welcome")
                .content("A few words of introduction...")
                .positiveText("IMPORT WEBCAMS NOW!")
                .neutralText("SHOW ME HELP")
                .negativeText("NO THANKS")
                .forceStacking(true)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        DialogFragment fetcher = new JsonFetcher();
                        fetcher.show(getFragmentManager(), "JsonFetcher");
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                    }

                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                    }
                })
                .show();
        return dialog;
    }
}
