package de.int80.gothbingo;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

abstract class UpdateFileWrapper {

    final DownloadManager downloadManager;
    final Context mContext;
    final long downloadID;

    UpdateFileWrapper(Context context, long downloadID) {
        mContext = context;
        downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        this.downloadID = downloadID;
    }

    static UpdateFileWrapper makeUpdateFileWrapper(Context context, long downloadID) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return new NougatFileWrapper(context, downloadID);
        } else
            return new LegacyFileWrapper(context, downloadID);

    }

    abstract Uri getUri();
    abstract String getPath();
    abstract void close();
}
