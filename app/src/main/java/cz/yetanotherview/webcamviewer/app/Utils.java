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

package cz.yetanotherview.webcamviewer.app;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

public class Utils {

    public static String folderWCVPath = Environment.getExternalStorageDirectory() + "/WebCamViewer/";
    public static String extension = ".wcv";
    public static String oldExtension = ".db";

    /**
     * Get All files from given Directory
     * @param DirectoryPath Directory patch
     * @return An array of files
     */
    public static File[] GetFiles(String DirectoryPath) {
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
        ArrayList<String> arrayFiles = new ArrayList<String>();
        if (file.length == 0)
            return null;
        else {
            for (File aFile : file) arrayFiles.add(aFile.getName());
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
     * Delete Picasso Cache.
     */
    public static void deletePicassoCache(File cache) {
        String folderPicassoCache = "/picasso-cache";
        File picassoCache = new File (cache + folderPicassoCache);
        if (picassoCache.isDirectory()) {
            String[] children = picassoCache.list();
            for (String aChildren : children) {
                new File(picassoCache, aChildren).delete();
            }
        }
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
}
