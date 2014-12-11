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
}
