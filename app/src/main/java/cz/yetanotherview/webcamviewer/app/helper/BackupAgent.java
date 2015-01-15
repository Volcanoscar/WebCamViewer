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

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.IOException;

import cz.yetanotherview.webcamviewer.app.MainActivity;
import cz.yetanotherview.webcamviewer.app.SettingsFragment;
import cz.yetanotherview.webcamviewer.app.actions.ImportDialog;
import cz.yetanotherview.webcamviewer.app.actions.JsonFetcherDialog;

public class BackupAgent extends BackupAgentHelper {

    private static final String DB_NAME = DatabaseHelper.DATABASE_NAME;
    private static final String PREFS = "cz.yetanotherview.webcamviewer.app_preferences";

    private static final String FILES_BACKUP_KEY = "dbs";
    private static final String PREFS_BACKUP_KEY = "prefs";

    @Override
    public void onCreate(){
        FileBackupHelper filehelper = new FileBackupHelper(this, DB_NAME);
        addHelper(FILES_BACKUP_KEY, filehelper);
        SharedPreferencesBackupHelper preferenceshelper = new SharedPreferencesBackupHelper(this, PREFS);
        addHelper(PREFS_BACKUP_KEY, preferenceshelper);
    }

    @Override
    public File getFilesDir(){
        File path = getDatabasePath(DB_NAME);
        return path.getParentFile();
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
        // Hold the lock while the FileBackupHelper performs backup
        synchronized (MainActivity.sDataLock) {
            super.onBackup(oldState, data, newState);
        }

        synchronized (SettingsFragment.sDataLock) {
            super.onBackup(oldState, data, newState);
        }

        synchronized (JsonFetcherDialog.sDataLock) {
            super.onBackup(oldState, data, newState);
        }

        synchronized (ImportDialog.sDataLock) {
            super.onBackup(oldState, data, newState);
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
                          ParcelFileDescriptor newState) throws IOException {
        // Hold the lock while the FileBackupHelper restores the file
        synchronized (MainActivity.sDataLock) {
            super.onRestore(data, appVersionCode, newState);
        }

        synchronized (SettingsFragment.sDataLock) {
            super.onRestore(data, appVersionCode, newState);
        }

        synchronized (JsonFetcherDialog.sDataLock) {
            super.onRestore(data, appVersionCode, newState);
        }

        synchronized (ImportDialog.sDataLock) {
            super.onRestore(data, appVersionCode, newState);
        }
    }

}