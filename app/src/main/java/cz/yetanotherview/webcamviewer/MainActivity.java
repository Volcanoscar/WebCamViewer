package cz.yetanotherview.webcamviewer;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

import cz.yetanotherview.webcamviewer.actions.AddWebcam;
import cz.yetanotherview.webcamviewer.actions.ModifyWebcam;
import cz.yetanotherview.webcamviewer.helper.RecyclerItemClickListener;
import cz.yetanotherview.webcamviewer.adapter.WebCamAdapter;
import cz.yetanotherview.webcamviewer.db.DatabaseHelper;
import cz.yetanotherview.webcamviewer.model.Webcam;

public class MainActivity extends ActionBarActivity {

    private DatabaseHelper db;
    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private WebCamAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.cardList);
        mRecyclerView.setHasFixedSize(true);

        mEmptyView = findViewById(R.id.empty);

        mLayoutManager = new LinearLayoutManager(this);
        //layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        db = new DatabaseHelper(getApplicationContext());
        List<Webcam> allWebCams = db.getAllWebCams();
        db.closeDB();

        mAdapter = new WebCamAdapter(allWebCams);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkAdapterIsEmpty();
            }
        });

        //mAdapter.notifyDataSetChanged();
        mRecyclerView.setAdapter(mAdapter);
        checkAdapterIsEmpty();

        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getApplicationContext(), mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        // do whatever
                    }
                    @Override
                    public void onItemLongClick(View view, int position)
                    {
                        Webcam webcam = (Webcam) mAdapter.getItem(position);
                        long id = webcam.getId();

                        Intent modify_intent = new Intent(getApplicationContext(), ModifyWebcam.class);
                        modify_intent.putExtra("id", id);
                        startActivity(modify_intent);
                    }
                })
        );

        // OnCLickListiner For List Items
//        listView.setOnItemClickListener(new OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long viewId) {
//
//            }
//        });
//
//        // OnLongCLickListiner For List Items
//        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long viewId) {
//
//                Webcam webcam = (Webcam) adapter.getItem(position);
//                long id = webcam.getId();
//
//                Intent modify_intent = new Intent(getApplicationContext(), ModifyWebcam.class);
//                modify_intent.putExtra("id", id);
//                startActivity(modify_intent);
//                return false;
//            }
//        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.add_record) {
            Intent add_mem = new Intent(this, AddWebcam.class);
            startActivity(add_mem);
        }
        else if (id == R.id.export_records) {
//            Cursor cursor = dbManager.fetch();
//            exportToExcel(cursor);
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkAdapterIsEmpty () {
        if (mAdapter.getItemCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
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
