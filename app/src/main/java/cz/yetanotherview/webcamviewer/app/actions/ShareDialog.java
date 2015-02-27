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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import com.afollestad.materialdialogs.MaterialDialog;

import junit.framework.Assert;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.Utils;

public class ShareDialog extends DialogFragment {

    private Uri bmpUri;
    private String url;
    private MaterialDialog mProgressDialog;

    private static String baseFolderPath = Utils.folderWCVPath;
    private static String tmpFolderPath = Utils.folderWCVPathTmp;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle bundle = this.getArguments();
        url = bundle.getString("url", "");

        mProgressDialog = new MaterialDialog.Builder(getActivity())
                .content(R.string.please_wait)
                .progress(false, 100)
                .build();

        createFolders();
        new ConnectionTester().execute();

        return mProgressDialog;
    }

    private void createFolders() {
        File baseFolder = new File(baseFolderPath);
        File tmpFolder = new File(tmpFolderPath);
        if (!baseFolder.exists()) {
            baseFolder.mkdir();
        }
        if (!tmpFolder.exists()) {
            tmpFolder.mkdir();
        }
    }

    private class ConnectionTester extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {

            try {
                URL mUrl = new URL(url);
                HttpURLConnection urlConn = (HttpURLConnection) mUrl.openConnection();
                urlConn.connect();
                Assert.assertEquals(HttpURLConnection.HTTP_OK, urlConn.getResponseCode());

                new ShareImage().execute();
            }
            catch (IOException e) {
                System.err.println("Error creating HTTP connection");

                mProgressDialog.dismiss();
                this.publishProgress();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            dialogUnavailable();
        }
    }

    private class ShareImage extends AsyncTask <String,Integer,Long> {

        @Override
        protected Long doInBackground(String... args) {
            int count;
            try {
                URL mUrl = new URL(url);
                URLConnection connexion = mUrl.openConnection();
                connexion.connect();
                String targetFileName = "share_image_" + System.currentTimeMillis() + ".jpg";
                int lengthOfFile = connexion.getContentLength();
                InputStream input = new BufferedInputStream(mUrl.openStream());
                OutputStream output = new FileOutputStream(tmpFolderPath + targetFileName);
                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress ((int)(total*100/lengthOfFile));
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();

                // Compress
                File file =  new File(tmpFolderPath + targetFileName);
                Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                FileOutputStream out = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.JPEG, 78, out);
                out.close();

                bmpUri = Uri.fromFile(file);

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

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/jpeg");
                shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share)));

            }
        });
    }

    private void dialogUnavailable() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.server_unavailable)
                .content(R.string.server_unavailable_summary)
                .positiveText(android.R.string.ok)
                .show();
    }
}
