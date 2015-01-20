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

package cz.yetanotherview.webcamviewer.app.helper;

import cz.yetanotherview.webcamviewer.app.model.Category;
import cz.yetanotherview.webcamviewer.app.model.WebCam;

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
    private static final String KEY_UNI_ID = "uni_id";
    private static final String KEY_WEBCAM = "webcam_name";
    private static final String KEY_WEBCAM_URL = "webcam_url";
    private static final String KEY_POSITION = "position";
    private static final String KEY_STATUS = "status";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";

    // CATEGORYS Table - column names
    private static final String KEY_CATEGORY_NAME = "category_name";

    // WEBCAM_CATEGORYS Table - column names
    private static final String KEY_WEBCAM_ID = "webcam_id";
    private static final String KEY_CATEGORY_ID = "category_id";

    // Table Create Statements
    // WebCam table create statement
    private static final String CREATE_TABLE_WEBCAM = "CREATE TABLE IF NOT EXISTS "
            + TABLE_WEBCAM + "(" + KEY_ID + " INTEGER PRIMARY KEY," + KEY_UNI_ID + " INTEGER," + KEY_WEBCAM
            + " TEXT," + KEY_WEBCAM_URL + " TEXT," + KEY_POSITION + " INTEGER," + KEY_STATUS + " INTEGER,"
            + KEY_LATITUDE + " REAL,"  + KEY_LONGITUDE + " REAL," + KEY_CREATED_AT + " DATETIME" + ")";

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

    // ------------------------ "WebCams" table methods ----------------//

    /**
     * Creating a webCam
     */
    public long createWebCam(WebCam webCam, long[] category_ids) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_UNI_ID, webCam.getUniId());
        values.put(KEY_WEBCAM, webCam.getName());
        values.put(KEY_WEBCAM_URL, webCam.getUrl());
        values.put(KEY_POSITION, webCam.getPosition());
        values.put(KEY_STATUS, webCam.getStatus());
        values.put(KEY_LATITUDE, webCam.getLatitude());
        values.put(KEY_LONGITUDE, webCam.getLongitude());
        values.put(KEY_CREATED_AT, getDateTime());

        // insert row
        long webcam_id = db.insert(TABLE_WEBCAM, null, values);

        if (category_ids != null) {
            // insert category_ids
            for (long category_id : category_ids) {
                createWebCamCategory(webcam_id, category_id);
            }
        }

        return webcam_id;
    }

    /**
     * get single WebCam
     */
    public WebCam getWebCam(long webcam_id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_WEBCAM + " WHERE "
                + KEY_ID + " = " + webcam_id;

        Log.d(LOG, selectQuery);

        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null) {
            c.moveToFirst();
        }
        WebCam wc = new WebCam();
        wc.setId(c.getInt(c.getColumnIndex(KEY_ID)));
        wc.setUniId(c.getInt(c.getColumnIndex(KEY_UNI_ID)));
        wc.setName(c.getString(c.getColumnIndex(KEY_WEBCAM)));
        wc.setUrl(c.getString(c.getColumnIndex(KEY_WEBCAM_URL)));
        wc.setPosition(c.getInt(c.getColumnIndex(KEY_POSITION)));
        wc.setCreatedAt(c.getString(c.getColumnIndex(KEY_CREATED_AT)));

        return wc;
    }

    /**
     * getting all WebCams
     * */
    public List<WebCam> getAllWebCams(String orderby) {
        List<WebCam> webCams = new ArrayList<WebCam>();
        String selectQuery = "SELECT * FROM " + TABLE_WEBCAM + " ORDER BY " + orderby;

        Log.d(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                WebCam wc = new WebCam();
                wc.setId(c.getLong(c.getColumnIndex(KEY_ID)));
                wc.setUniId(c.getLong(c.getColumnIndex(KEY_UNI_ID)));
                wc.setName(c.getString(c.getColumnIndex(KEY_WEBCAM)));
                wc.setUrl(c.getString(c.getColumnIndex(KEY_WEBCAM_URL)));
                wc.setPosition(c.getInt(c.getColumnIndex(KEY_POSITION)));
                wc.setStatus(c.getInt(c.getColumnIndex(KEY_STATUS)));
                wc.setLatitude(c.getDouble(c.getColumnIndex(KEY_LATITUDE)));
                wc.setLongitude(c.getDouble(c.getColumnIndex(KEY_LONGITUDE)));
                wc.setCreatedAt(c.getString(c.getColumnIndex(KEY_CREATED_AT)));

                // adding to WebCam list
                webCams.add(wc);
            } while (c.moveToNext());
        }

        return webCams;
    }

    /**
     * getting all WebCams under single category
     * */
    public List<WebCam> getAllWebCamsByCategory(long category_id, String orderBy) {
        List<WebCam> webCams = new ArrayList<WebCam>();

        String selectQuery = "SELECT * FROM " + TABLE_WEBCAM + " td, "
                + TABLE_CATEGORY + " tg, " + TABLE_WEBCAM_CATEGORY + " tt WHERE tg."
                + KEY_ID + " = " + category_id + " AND tg." + KEY_ID
                + " = " + "tt." + KEY_CATEGORY_ID + " AND td." + KEY_ID + " = "
                + "tt." + KEY_WEBCAM_ID + " ORDER BY " + orderBy;

        Log.d(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                WebCam wc = new WebCam();
                wc.setId(c.getInt(0));
                wc.setName((c.getString(c.getColumnIndex(KEY_WEBCAM))));
                wc.setUrl((c.getString(c.getColumnIndex(KEY_WEBCAM_URL))));
                wc.setPosition((c.getInt(c.getColumnIndex(KEY_POSITION))));
                wc.setCreatedAt(c.getString(c.getColumnIndex(KEY_CREATED_AT)));

                // adding to WebCam list
                webCams.add(wc);
            } while (c.moveToNext());
        }

        return webCams;
    }

    /**
     * getting WebCam count
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
     * Updating a WebCam
     */
    public void updateWebCam(WebCam webCam, long[] category_ids) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_WEBCAM, webCam.getName());
        values.put(KEY_WEBCAM_URL, webCam.getUrl());
        values.put(KEY_POSITION, webCam.getPosition());
        values.put(KEY_STATUS, webCam.getStatus());

        long webcam_id = webCam.getId();

        // updating row
        db.update(TABLE_WEBCAM, values, KEY_ID + " = ?",
                new String[] { String.valueOf(webcam_id) });

        //remove all assigned categories
        deleteWebCamCategory(webcam_id);

        //assign new categories
        if (category_ids != null) {
            // insert category_ids
            for (long category_id : category_ids) {
                createWebCamCategory(webcam_id, category_id);
            }
        }

    }

    /**
     * Deleting a WebCam
     */
    public void deleteWebCam(long webcam_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WEBCAM, KEY_ID + " = ?",
                new String[] { String.valueOf(webcam_id) });

        //remove all assigned categories
        deleteWebCamCategory(webcam_id);
    }

    /**
     * Delete all WebCams
     */
    public void deleteAllWebCams(boolean should_delete_all_categories) {
        SQLiteDatabase db = this.getWritableDatabase();

        //WebCams table
        db.delete(TABLE_WEBCAM, null, null);

        //WebCam categories table
        db.delete(TABLE_WEBCAM_CATEGORY, null, null);

        //Category table
        if (should_delete_all_categories) {
            db.delete(TABLE_CATEGORY, null, null);
        }
    }

    // ------------------------ "categories" table methods ----------------//

    /**
     * Creating category
     */
    public long createCategory(Category category) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_CATEGORY_NAME, category.getcategoryName());
        values.put(KEY_CREATED_AT, getDateTime());

        // insert row
        return db.insert(TABLE_CATEGORY, null, values);
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
    public void deleteCategory(long categoryId, boolean should_delete_all_category_WebCams) {
        SQLiteDatabase db = this.getWritableDatabase();

        // before deleting category
        // check if WebCams under this category should also be deleted
        if (should_delete_all_category_WebCams) {
            // get all WebCams under this category
            List<WebCam> allCategoryWebCams = getAllWebCamsByCategory(categoryId,"id ASC");

            // delete all WebCams
            for (WebCam webCam : allCategoryWebCams) {
                // delete WebCam
                deleteWebCam(webCam.getId());
            }
        }

        // now delete the category
        db.delete(TABLE_CATEGORY, KEY_ID + " = ?",
                new String[] { String.valueOf(categoryId) });

        // and WebCam categories table
        db.delete(TABLE_WEBCAM_CATEGORY, KEY_CATEGORY_ID + " = ?",
                new String[] { String.valueOf(categoryId) });
    }

    // ------------------------ "WebCam_categories" table methods ----------------//

    /**
     * Getting WebCam categories Ids
     */
    public long[] getWebCamCategoriesIds(long webCam_id) {

        String selectQuery = "SELECT * FROM " + TABLE_WEBCAM_CATEGORY + " WHERE "
                + KEY_WEBCAM_ID + " = " + webCam_id;
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
     * Creating WebCam_category
     */
    public long createWebCamCategory(long webCam_id, long category_id) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_WEBCAM_ID, webCam_id);
        values.put(KEY_CATEGORY_ID, category_id);
        values.put(KEY_CREATED_AT, getDateTime());

        return db.insert(TABLE_WEBCAM_CATEGORY, null, values);
    }

    /**
     * Deleting a WebCam category
     */
    public void deleteWebCamCategory(long webCam_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WEBCAM_CATEGORY, KEY_WEBCAM_ID + " = ?",
                new String[] { String.valueOf(webCam_id) });
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