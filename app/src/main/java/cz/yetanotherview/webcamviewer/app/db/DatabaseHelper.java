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

package cz.yetanotherview.webcamviewer.app.db;

import cz.yetanotherview.webcamviewer.app.model.Category;
import cz.yetanotherview.webcamviewer.app.model.Webcam;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Logcat category
    private static final String LOG = DatabaseHelper.class.getName();

    // Database Version
    private static final int DATABASE_VERSION = 4;

    // Database Name
    public static final String DATABASE_NAME = "webCamDatabase.db";

    // Table Names
    private static final String TABLE_WEBCAM = "webcams";
    private static final String TABLE_CATEGORY = "categories";
    private static final String TABLE_WEBCAM_CATEGORY = "webcam_categories";

    // Common column names
    private static final String KEY_ID = "id";
    private static final String KEY_CREATED_AT = "created_at";

    // WEBCAM Table - column nmaes
    private static final String KEY_WEBCAM = "webcam_name";
    private static final String KEY_WEBCAM_URL = "webcam_url";
    private static final String KEY_POSITION = "position";
    private static final String KEY_STATUS = "status";

    // CATEGORYS Table - column names
    private static final String KEY_CATEGORY_NAME = "category_name";

    // WEBCAM_CATEGORYS Table - column names
    private static final String KEY_WEBCAM_ID = "webcam_id";
    private static final String KEY_CATEGORY_ID = "category_id";

    // Table Create Statements
    // Webcam table create statement
    private static final String CREATE_TABLE_WEBCAM = "CREATE TABLE IF NOT EXISTS "
            + TABLE_WEBCAM + "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_WEBCAM
            + " TEXT," + KEY_WEBCAM_URL + " TEXT," + KEY_POSITION + " INTEGER," + KEY_STATUS + " INTEGER," + KEY_CREATED_AT
            + " DATETIME" + ")";

    // Category table create statement
    private static final String CREATE_TABLE_CATEGORY = "CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORY
            + "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_CATEGORY_NAME + " TEXT,"
            + KEY_CREATED_AT + " DATETIME" + ")";

    // webcam_category table create statement
    private static final String CREATE_TABLE_WEBCAM_CATEGORY = "CREATE TABLE IF NOT EXISTS "
            + TABLE_WEBCAM_CATEGORY + "(" + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_WEBCAM_ID + " INTEGER," + KEY_CATEGORY_ID + " INTEGER,"
            + KEY_CREATED_AT + " DATETIME" + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // creating required tables
        db.execSQL(CREATE_TABLE_WEBCAM);
        db.execSQL(CREATE_TABLE_CATEGORY);
        db.execSQL(CREATE_TABLE_WEBCAM_CATEGORY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int upgradeTo = oldVersion + 1;
        while (upgradeTo <= newVersion)
        {
            switch (upgradeTo)
            {
                case 2:
                    //badUpgrade(db);
                    break;
                case 3:
                    migrateOldTables(db);
                    break;
            }
            upgradeTo++;
        }
    }

    public void migrateOldTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + "CardCursorTableCategory");

        onCreate(db);

        String oldWebCamTable = "CardCursorTable";
        db.execSQL("INSERT INTO " + TABLE_WEBCAM +"(" + KEY_WEBCAM + "," + KEY_WEBCAM_URL + ")"
                + " SELECT header,thumb FROM " + oldWebCamTable);
        db.execSQL("DROP TABLE IF EXISTS " + oldWebCamTable);
    }

    // ------------------------ "webcams" table methods ----------------//

    /**
     * Creating a webcam
     */
    public long createWebCam(Webcam webcam, long[] category_ids) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_WEBCAM, webcam.getName());
        values.put(KEY_WEBCAM_URL, webcam.getUrl());
        values.put(KEY_POSITION, webcam.getPosition());
        values.put(KEY_STATUS, webcam.getStatus());
        values.put(KEY_CREATED_AT, getDateTime());

        // insert row
        long webcam_id = db.insert(TABLE_WEBCAM, null, values);

        if (category_ids != null) {
            // insert category_ids
            for (long category_id : category_ids) {
                createWebcamCategory(webcam_id, category_id);
            }
        }

        return webcam_id;
    }

    /**
     * get single webcam
     */
    public Webcam getWebcam(long webcam_id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_WEBCAM + " WHERE "
                + KEY_ID + " = " + webcam_id;

        Log.d(LOG, selectQuery);

        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null)
            c.moveToFirst();

        Webcam wc = new Webcam();
        wc.setId(c.getInt(c.getColumnIndex(KEY_ID)));
        wc.setName((c.getString(c.getColumnIndex(KEY_WEBCAM))));
        wc.setUrl((c.getString(c.getColumnIndex(KEY_WEBCAM_URL))));
        wc.setPosition((c.getInt(c.getColumnIndex(KEY_POSITION))));
        wc.setCreatedAt(c.getString(c.getColumnIndex(KEY_CREATED_AT)));

        return wc;
    }

    /**
     * getting all webcams
     * */
    public List<Webcam> getAllWebCams(String orderby) {
        List<Webcam> webcams = new ArrayList<Webcam>();
        String selectQuery = "SELECT * FROM " + TABLE_WEBCAM + " ORDER BY " + orderby;

        Log.d(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                Webcam wc = new Webcam();
                wc.setId(c.getInt((c.getColumnIndex(KEY_ID))));
                wc.setName((c.getString(c.getColumnIndex(KEY_WEBCAM))));
                wc.setUrl((c.getString(c.getColumnIndex(KEY_WEBCAM_URL))));
                wc.setPosition((c.getInt(c.getColumnIndex(KEY_POSITION))));
                wc.setCreatedAt(c.getString(c.getColumnIndex(KEY_CREATED_AT)));

                // adding to webcam list
                webcams.add(wc);
            } while (c.moveToNext());
        }

        return webcams;
    }

    /**
     * getting all webcams under single category
     * */
    public List<Webcam> getAllWebCamsByCategory(String category_name, String orderby) {
        List<Webcam> webcams = new ArrayList<Webcam>();

        String selectQuery = "SELECT * FROM " + TABLE_WEBCAM + " td, "
                + TABLE_CATEGORY + " tg, " + TABLE_WEBCAM_CATEGORY + " tt WHERE tg."
                + KEY_CATEGORY_NAME + " = '" + category_name + "'" + " AND tg." + KEY_ID
                + " = " + "tt." + KEY_CATEGORY_ID + " AND td." + KEY_ID + " = "
                + "tt." + KEY_WEBCAM_ID + " ORDER BY " + orderby;

        Log.d(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                Webcam wc = new Webcam();
                wc.setId(c.getInt(0));
                wc.setName((c.getString(c.getColumnIndex(KEY_WEBCAM))));
                wc.setUrl((c.getString(c.getColumnIndex(KEY_WEBCAM_URL))));
                wc.setPosition((c.getInt(c.getColumnIndex(KEY_POSITION))));
                wc.setCreatedAt(c.getString(c.getColumnIndex(KEY_CREATED_AT)));

                // adding to webcam list
                webcams.add(wc);
            } while (c.moveToNext());
        }

        return webcams;
    }

    /**
     * getting webcam count
     */
    public int getWebCamCount() {
        String countQuery = "SELECT * FROM " + TABLE_WEBCAM;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);

        int count = cursor.getCount();
        cursor.close();

        // return count
        return count;
    }

    /**
     * Updating a webcam
     */
    public void updateWebCam(Webcam webcam, long[] category_ids) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_WEBCAM, webcam.getName());
        values.put(KEY_WEBCAM_URL, webcam.getUrl());
        values.put(KEY_POSITION, webcam.getPosition());
        values.put(KEY_STATUS, webcam.getStatus());

        long webcam_id = webcam.getId();

        // updating row
        db.update(TABLE_WEBCAM, values, KEY_ID + " = ?",
                new String[] { String.valueOf(webcam_id) });

        //remove all assigned categories
        deleteWebCamCategory(webcam_id);

        //assign new categories
        if (category_ids != null) {
            // insert category_ids
            for (long category_id : category_ids) {
                createWebcamCategory(webcam_id, category_id);
            }
        }

    }

    /**
     * Deleting a webcam
     */
    public void deleteWebCam(long webcam_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WEBCAM, KEY_ID + " = ?",
                new String[] { String.valueOf(webcam_id) });

        //remove all assigned categories
        deleteWebCamCategory(webcam_id);
    }

    /**
     * Delete all webcams
     */
    public void deleteAllWebCams() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WEBCAM, null, null);
    }

    // ------------------------ "categorys" table methods ----------------//

    /**
     * Creating category
     */
    public long createCategory(Category category) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_CATEGORY_NAME, category.getcategoryName());
        values.put(KEY_CREATED_AT, getDateTime());

        // insert row
        long category_id = db.insert(TABLE_CATEGORY, null, values);

        return category_id;
    }

    /**
     * getting all categories
     * */
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<Category>();
        String selectQuery = "SELECT * FROM " + TABLE_CATEGORY;

        Log.d(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                Category t = new Category();
                t.setId(c.getInt((c.getColumnIndex(KEY_ID))));
                t.setcategoryName(c.getString(c.getColumnIndex(KEY_CATEGORY_NAME)));

                // adding to categories list
                categories.add(t);
            } while (c.moveToNext());
        }
        return categories;
    }

    /**
     * Updating a category
     */
    public int updateCategory(Category category) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_CATEGORY_NAME, category.getcategoryName());

        // updating row
        return db.update(TABLE_CATEGORY, values, KEY_ID + " = ?",
                new String[] { String.valueOf(category.getId()) });
    }

    /**
     * Deleting a category
     */
    public void deleteCategory(Category category, boolean should_delete_all_category_webcams) {
        SQLiteDatabase db = this.getWritableDatabase();

        // before deleting category
        // check if webcams under this category should also be deleted
        if (should_delete_all_category_webcams) {
            // get all webcams under this category
            List<Webcam> allCategoryWebCams = getAllWebCamsByCategory(category.getcategoryName(),"id ASC");

            // delete all webcams
            for (Webcam webcam : allCategoryWebCams) {
                // delete webcam
                deleteWebCam(webcam.getId());
            }
        }

        // now delete the category
        db.delete(TABLE_CATEGORY, KEY_ID + " = ?",
                new String[] { String.valueOf(category.getId()) });
    }

    // ------------------------ "webcam_categorys" table methods ----------------//

    /**
     * Creating webcam_category
     */
    public long[] getWebcamCategoriesIds(long webcam_id) {

        String selectQuery = "SELECT * FROM " + TABLE_WEBCAM_CATEGORY + " WHERE "
                + KEY_WEBCAM_ID + " = " + webcam_id;
        Log.d(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        long[] categories_ids = new long[c.getCount()];
        // looping through all rows and adding to list
        int i = 0;
        if (c.moveToFirst()) {
            do {
                categories_ids[i] = c.getInt(c.getColumnIndex(KEY_CATEGORY_ID));
                i++;
            } while (c.moveToNext());
        }

        return categories_ids;
    }

    /**
     * Creating webcam_category
     */
    public long createWebcamCategory(long webcam_id, long category_id) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_WEBCAM_ID, webcam_id);
        values.put(KEY_CATEGORY_ID, category_id);
        values.put(KEY_CREATED_AT, getDateTime());

        return db.insert(TABLE_WEBCAM_CATEGORY, null, values);
    }

    /**
     * Deleting a webcam category
     */
    public void deleteWebCamCategory(long webcam_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WEBCAM_CATEGORY, KEY_WEBCAM_ID + " = ?",
                new String[] { String.valueOf(webcam_id) });
    }

    // closing database
    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }

    /**
     * get datetime
     * */
    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
}