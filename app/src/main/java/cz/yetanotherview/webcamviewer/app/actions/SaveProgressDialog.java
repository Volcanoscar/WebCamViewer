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
import android.os.AsyncTask;
import android.os.Bundle;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.Utils;

public class SaveProgressDialog extends DialogFragment {

    private MaterialDialog mProgressDialog;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle bundle = this.getArguments();
        String url = bundle.getString("url", "");
        String name = bundle.getString("name", "");
        String path = bundle.getString("path", "");

        mProgressDialog = new MaterialDialog.Builder(getActivity())
                .content(R.string.please_wait)
                .progress(false, 100)
                .build();

        new SaveImage().execute(url, name, path);
        return mProgressDialog;
    }


    private class SaveImage extends AsyncTask <String,Integer,Long> {

        @Override
        protected Long doInBackground(String... args) {
            int count;
            try {
                URL url = new URL(args[0]);
                URLConnection connexion = url.openConnection();
                connexion.connect();
                String targetFileName = (Utils.getNameStrippedAccents(args[1]) + " " + Utils.getCustomDateString() + ".jpg")
                        .replaceAll("\\s+","_");
                int lengthOfFile = connexion.getContentLength();
                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output = new FileOutputStream(args[2] + "/" + targetFileName);
                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress ((int)(total*100 / lengthOfFile));
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
                continueOnUiThread();
            } catch (Exception ignored) {}
            return null;
        }
        protected void onProgressUpdate(Integer... progress) {
            mProgressDialog.setProgress(progress[0]);
        }
    }

    private void continueOnUiThread() {

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                mProgressDialog.dismiss();
                Snackbar.with(getActivity().getApplicationContext())
                        .text(R.string.dialog_positive_toast_message)
                        .actionLabel(R.string.dismiss)
                        .actionColor(getResources().getColor(R.color.yellow))
                        .show(getActivity());
            }
        });
    }
}