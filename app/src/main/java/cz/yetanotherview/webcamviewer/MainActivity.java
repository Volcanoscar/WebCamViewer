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

package cz.yetanotherview.webcamviewer;

import android.app.DialogFragment;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.melnykov.fab.FloatingActionButton;
import com.nispok.snackbar.Snackbar;

import java.util.List;

import cz.yetanotherview.webcamviewer.actions.AddDialog;
import cz.yetanotherview.webcamviewer.actions.EditDialog;
import cz.yetanotherview.webcamviewer.fullscreen.FullScreenImage;
import cz.yetanotherview.webcamviewer.helper.RecyclerItemClickListener;
import cz.yetanotherview.webcamviewer.adapter.WebCamAdapter;
import cz.yetanotherview.webcamviewer.db.DatabaseHelper;
import cz.yetanotherview.webcamviewer.helper.WebCamListener;
import cz.yetanotherview.webcamviewer.model.Category;
import cz.yetanotherview.webcamviewer.model.Webcam;

public class MainActivity extends ActionBarActivity implements WebCamListener {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();

    private DatabaseHelper db;
    private List<Webcam> allWebCams;
    private RecyclerView mRecyclerView;
    private WebCamAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initToolbar();
        initRecyclerView();
        initFab();
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        //Title and subtitle
        toolbar.setTitle(R.string.app_name);
        //toolbar.setSubtitle("Subtitle");

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                switch (menuItem.getItemId()){
                    case R.id.menu_about:
                        //Cursor cursor = dbManager.fetch();
                        //exportToExcel(cursor);
                        return true;
                }

                return false;
            }
        });

        //Navigation Icon
        //toolbar.setNavigationIcon(R.drawable.ic_launcher);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(ToolbarActivity.this,"Navigation",Toast.LENGTH_SHORT).show();
            }
        });

        // Inflate a menu to be displayed in the toolbar
        toolbar.inflateMenu(R.menu.toolbar_menu);
    }

    private void initRecyclerView() {
        mLayoutManager = new LinearLayoutManager(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.cardList);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        db = new DatabaseHelper(getApplicationContext());
        allWebCams = db.getAllWebCams();
        //db.closeDB(); ToDo...

        mAdapter = new WebCamAdapter(allWebCams);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getApplicationContext(),
            mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    showImageFullscreen(position);
                }
                @Override
                public void onItemLongClick(View view, int position) {
                    showEditDialog(position);
                }
            })
        );
    }

    private void initFab() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(mRecyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddDialog();
            }
        });
    }

    private void showImageFullscreen(int position) {
        Webcam webcam = (Webcam) mAdapter.getItem(position);
        Intent fullScreenIntent = new Intent(getApplicationContext(), FullScreenImage.class);
        fullScreenIntent.putExtra("url", webcam.getUrl());
        startActivity(fullScreenIntent);
    }

    private void showAddDialog() {
        DialogFragment newFragment = AddDialog.newInstance(this);
        newFragment.show(getFragmentManager(), "AddDialog");
    }

    private void showEditDialog(int position) {
        DialogFragment newFragment = EditDialog.newInstance(this);
        Webcam webcam = (Webcam) mAdapter.getItem(position);

        Bundle bundle = new Bundle();
        bundle.putLong("id", webcam.getId());
        bundle.putString("name",webcam.getName());
        bundle.putString("url",webcam.getUrl());
        bundle.putInt("pos",webcam.getPosition());
        bundle.putInt("status",webcam.getStatus());
        bundle.putInt("position",position);
        newFragment.setArguments(bundle);

        newFragment.show(getFragmentManager(), "EditDialog");
    }

    @Override
    public void webcamAdded(String name, String url, int pos, int status) {
        Webcam webcam = new Webcam(
                name,
                url,
                pos,
                status);
        synchronized (sDataLock) {
            db.createWebCam(webcam,
                    new long[]{db.createCategory(new Category(""))});
            //db.closeDB(); ToDo...
        }
        BackupManager backupManager = new BackupManager(this);
        backupManager.dataChanged();

        mAdapter.addItem(mAdapter.getItemCount(), webcam);
        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);

        saveDone();
    }

    @Override
    public void webcamEdited(long id, String name, String url, int pos, int status, int position) {
        Webcam webcam = db.getWebcam(id);
        webcam.setName(name);
        webcam.setUrl(url);
        webcam.setPosition(pos);
        webcam.setStatus(status);

        synchronized (sDataLock) {
            db.updateWebCam(webcam);
            //db.closeDB();
        }
        BackupManager backupManager = new BackupManager(this);
        backupManager.dataChanged();

        mAdapter.modifyItem(position,webcam);

        saveDone();
    }

    @Override
    public void webcamDeleted(long id, int position) {
        synchronized (sDataLock) {
            db.deleteWebCam(id);
            //db.closeDB(); ToDo...
        }
        BackupManager backupManager = new BackupManager(this);
        backupManager.dataChanged();

        if (mAdapter != null && mAdapter.getItemCount() > 0) {
            mAdapter.removeItem(mAdapter.getItemAt(position));
        }
        delDone();
    }

    private void saveDone() {
        Snackbar.with(getApplicationContext())
                .text(R.string.dialog_positive_toast_message)
                .actionLabel(R.string.dismiss)
                .actionColor(getResources().getColor(R.color.yellow))
                .show(this);
    }

    private void delDone() {
        Snackbar.with(getApplicationContext())
                .text(R.string.action_deleted)
                .actionLabel(R.string.dismiss)
                .actionColor(getResources().getColor(R.color.yellow))
                .show(this);
    }

//    /**
//     * Exports the cursor value to an excel sheet.
//     * Recommended to call this method in a separate thread,
//     * especially if you have more number of threads.
//     *
//     * @param cursor
//     */
//    private void exportToExcel(Cursor cursor) {
//        final String fileName = "WebcamList.xls";
//        //Saving file in external storage
//        File sdCard = Environment.getExternalStorageDirectory();
//        File directory = new File(sdCard.getAbsolutePath() + "/javatechig.webcam");
//        //create directory if not exist
//        if(!directory.isDirectory()){
//            directory.mkdirs();
//        }
//        //file path
//        File file = new File(directory, fileName);
//        WorkbookSettings wbSettings = new WorkbookSettings();
//        wbSettings.setLocale(new Locale("en", "EN"));
//        WritableWorkbook workbook;
//        try {
//            workbook = Workbook.createWorkbook(file, wbSettings);
//            //Excel sheet name. 0 represents first sheet
//            WritableSheet sheet = workbook.createSheet("MyShoppingList", 0);
//            try {
//                sheet.addCell(new Label(0, 0, "Subject")); // column and row
//                sheet.addCell(new Label(1, 0, "Description"));
//                if (cursor.moveToFirst()) {
//                    do {
//                        String title = cursor.getString(cursor.getColumnIndex(DatabaseHelper.WEBCAM_SUBJECT));
//                        String desc = cursor.getString(cursor.getColumnIndex(DatabaseHelper.WEBCAM_DESC));
//                        int i = cursor.getPosition() + 1;
//                        sheet.addCell(new Label(0, i, title));
//                        sheet.addCell(new Label(1, i, desc));
//                    } while (cursor.moveToNext());
//                }
//                //closing cursor
//                cursor.close();
//            } catch (RowsExceededException e) {
//                e.printStackTrace();
//            } catch (WriteException e) {
//                e.printStackTrace();
//            }
//            workbook.write();
//            try {
//                workbook.close();
//            } catch (WriteException e) {
//                e.printStackTrace();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
