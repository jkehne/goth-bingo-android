package de.int80.gothbingo;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;

class LegacyFileWrapper extends UpdateFileWrapper {
    LegacyFileWrapper(Context context, long downloadID) {
        super(context, downloadID);
    }

    @Override
    String getPath() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadID);

        Cursor cursor = downloadManager.query(query);
        cursor.moveToFirst();

        return cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
    }

    @Override
    Uri getUri() {
        File localFile = new File(getPath());
        return Uri.fromFile(localFile);

    }

    @Override
    void close() {
        //nothing to do here
    }
}
