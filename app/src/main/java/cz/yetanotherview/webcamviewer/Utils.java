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

import android.util.Log;

import java.io.File;

public class Utils {

    public static boolean checkIfExist(String filePath) {
        Log.d("", "Check is running...");
        Log.d("", filePath);
        File dbFile = new File(filePath);
        return dbFile.exists();
    }

    public static boolean removeDB(String filePath) {
        Log.d("", "Delete is running...");
        Log.d("", filePath);
        File file = new File(filePath);
        return file.delete();
    }

    /**
     * Delete Picasso Cache.
     */
    public static void deletePicassoCache(File cache) {
        File picassoCache = new File (cache+"/picasso-cache");
        if (picassoCache.isDirectory()) {
            String[] children = picassoCache.list();
            for (int i = 0; i < children.length; i++) {
                new File(picassoCache, children[i]).delete();
            }
        }
    }
}
