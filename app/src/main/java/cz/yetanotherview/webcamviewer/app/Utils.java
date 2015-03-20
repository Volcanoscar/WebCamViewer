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

package cz.yetanotherview.webcamviewer.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import com.bumptech.glide.Glide;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Utils {

    public static String folderWCVPath = Environment.getExternalStorageDirectory() + "/WebCamViewer/";
    public static String folderWCVPathTmp = folderWCVPath + "Tmp/";
    public static String extension = ".wcv";
    public static String oldExtension = ".db";
    public static String email = "cz840311@gmail.com";

    /**
     * Get current date
     * @return Date
     */
    public static long getDate() {
        Calendar c = Calendar.getInstance();
        return c.getTimeInMillis();
    }

    /**
     * Get current date based on location
     * @return Date based on location
     */
    public static String getDateString() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat();
        return df.format(c.getTime());
    }

    /**
     * Get current date for files in specific format
     * @return Date in specific format
     */
    public static String getCustomDateString() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("HH.mm_d.M.yy");
        return df.format(c.getTime());
    }

    /**
     * Return String with Stripped Accents
     * @param name Input String
     * @return Stripped String
     */
    public static String getNameStrippedAccents(String name) {
        String normalizedName = name;
        normalizedName = Normalizer.normalize(normalizedName, Normalizer.Form.NFD);
        normalizedName = normalizedName.replaceAll("[^\\p{ASCII}]", "");
        return normalizedName;
    }

    /**
     * Get All files from given Directory
     * @param DirectoryPath Directory patch
     * @return An array of files
     */
    public static File[] getFiles(String DirectoryPath) {
        File f = new File(DirectoryPath);
        f.mkdirs();
        return f.listFiles();
    }

    /**
     * Get file names
     * @param file An array list of files
     * @return An array list of strings file names
     */
    public static ArrayList<String> getFileNames(File[] file){
        ArrayList<String> arrayFiles = new ArrayList<>();
        if (file.length == 0)
            return null;
        else {
            for (File aFile : file) {
                if (aFile.isFile()) {
                    arrayFiles.add(aFile.getName());
                }
            }
        }
        return arrayFiles;
    }

    /**
     * Remove old database file
     * @param fileDB old file
     * @return statement
     */
    public static boolean removeDB(File fileDB) {
        Log.d("", "Delete is running...");
        Log.d("", String.valueOf(fileDB));
        return fileDB.delete();
    }

    /**
     * Clean backup folder.
     */
    public static void cleanBackupFolder() {
        File backupFolder = new File(folderWCVPath);
        if (backupFolder.isDirectory()) {
            String[] children = backupFolder.list();
            for (String aChildren : children) {
                new File(backupFolder, aChildren).delete();
            }
        }
    }

    /**
     * Get the resources Id.
     */
    public static int getResId(String resourceName, Class<?> c) {

        List<String> us_states = Arrays.asList("alabama", "alaska", "arizona", "arkansas", "california",
                "colorado", "connecticut", "delaware", "florida", "georgia", "hawaii", "idaho", "illinois",
                "indiana", "iowa", "kansas", "kentucky", "louisiana", "maine", "maryland", "massachusetts",
                "michigan", "minnesota", "mississippi", "missouri", "montana", "nebraska", "nevada",
                "new_hampshire", "new_jersey", "new_mexico", "new_york", "north_carolina", "north_dakota",
                "ohio", "oklahoma", "oregon", "pennsylvania", "rhode_island", "south_carolina",
                "south_dakota", "tennessee", "texas", "utah", "vermont", "virginia", "washington",
                "washington_dc", "west_virginia", "wisconsin", "wyoming");
        boolean contains = us_states.contains(resourceName);

            try {
                int resId;
                if (contains) {
                    resId = R.drawable.united_states;
                }
                else {
                    Field idField = c.getDeclaredField(resourceName);
                    resId = idField.getInt(idField);
                }
                return resId;

            } catch (Exception e) {
                return R.drawable.unknown;
            }

    }

    /**
     * Round double
     */
    public static double roundDouble(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Clear Cache and Tmp folder
     */
    public static void deleteCache(Context context) {

        PackageManager pm = context.getPackageManager();
        // Get all methods on the PackageManager
        Method[] methods = pm.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (m.getName().equals("freeStorageAndNotify")) {
                // Found the method I want to use
                try {
                    m.invoke(pm, Long.MAX_VALUE , null);
                } catch (Exception e) {
                    Log.d("","Method invocation failed. Could be a permission problem");
                }
                break;
            }
        }

        try {
            File tmpFolder = new File(folderWCVPathTmp);
            if (tmpFolder.isDirectory()) {
                String[] children = tmpFolder.list();
                for (String aChildren : children) {
                    new File(tmpFolder, aChildren).delete();
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Clear image cache
     */
    public static void clearImageCache(Context context){
        File cacheDir = Glide.getPhotoCacheDir(context, "image_manager_disk_cache");
        Log.d("", String.valueOf(cacheDir));
        if (cacheDir.isDirectory()){
            for (File child : cacheDir.listFiles()){
                child.delete();
            }
        }
    }

    /**
     * Get locale code
     */
    public static String getLocaleCode(){
        return Locale.getDefault().toString();
    }
}
