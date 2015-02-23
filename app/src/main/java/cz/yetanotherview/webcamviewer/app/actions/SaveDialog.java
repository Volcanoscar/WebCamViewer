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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.Utils;

public class SaveDialog extends DialogFragment {

    private File parentFolder;
    private File[] parentContents;
    private boolean canGoUp = true;

    private String strippedName;
    private String path;

    private String name;
    private String url;

    public SaveDialog() {
        parentFolder = Environment.getExternalStorageDirectory();
        parentContents = listFiles();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle bundle = this.getArguments();
        name = bundle.getString("name", "");
        url = bundle.getString("url", "");

        return new MaterialDialog.Builder(getActivity())
                .title(parentFolder.getAbsolutePath())
                .items(getContentsArray())
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        if (canGoUp && which == 0) {
                            parentFolder = parentFolder.getParentFile();
                            canGoUp = parentFolder.getParent() != null;
                        } else {
                            parentFolder = parentContents[canGoUp ? which - 1 : which];
                            canGoUp = true;
                        }
                        parentContents = listFiles();
                        dialog.setTitle(parentFolder.getAbsolutePath());
                        dialog.setItems(getContentsArray());
                    }
                })
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        saveImage();
                        dialog.dismiss();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        dialog.dismiss();
                    }
                })
                .autoDismiss(false)
                .positiveText(R.string.choose)
                .negativeText(android.R.string.cancel)
                .build();
    }

    private void saveImage() {
        path = parentFolder.getAbsolutePath();
        strippedName = Utils.getNameStrippedAccents(name);

        Target saveFileTarget = new Target() {
            @Override
            public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        File file = new File(path + "/" + strippedName + " " + Utils.getDateString() + ".jpg");
                        try
                        {
                            boolean bool = file.createNewFile();
                            Log.d("", "File created: " + bool);
                            FileOutputStream ostream = new FileOutputStream(file);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 89, ostream);
                            ostream.close();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                    }
                }).start();
            }
            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        };

        Picasso.with(getActivity())
                .load(url)
                .into(saveFileTarget);

        Toast.makeText(getActivity(), R.string.dialog_positive_toast_message, Toast.LENGTH_SHORT).show();
    }

    private static class FolderSorter implements Comparator<File> {
        @Override
        public int compare(File lhs, File rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    }

    String[] getContentsArray() {
        String[] results = new String[parentContents.length + (canGoUp ? 1 : 0)];
        if (canGoUp) results[0] = "...";
        for (int i = 0; i < parentContents.length; i++)
            results[canGoUp ? i + 1 : i] = parentContents[i].getName();
        return results;
    }

    File[] listFiles() {
        File[] contents = parentFolder.listFiles();
        List<File> results = new ArrayList<>();
        for (File fi : contents) {
            if (fi.isDirectory()) results.add(fi);
        }
        Collections.sort(results, new FolderSorter());
        return results.toArray(new File[results.size()]);
    }
}