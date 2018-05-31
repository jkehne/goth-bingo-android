package de.int80.gothbingo;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

class UpdateDownloader extends BroadcastReceiver {
    private final String TAG = UpdateDownloader.class.getSimpleName();

    private Context mContext;
    private long downloadID;
    private DownloadManager downloadManager;

    UpdateDownloader(Context context) {
        mContext = context;
        downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        mContext.registerReceiver(this, filter);
    }

    private void unregisterReceiver() {
        mContext.unregisterReceiver(this);
    }

    private void queueDownloadInDownloadManager() {
        Uri apkURI = Uri.parse(mContext.getString(R.string.app_url));
        DownloadManager.Request request = new DownloadManager.Request(apkURI);

        downloadID = downloadManager.enqueue(request);

        Log.d(TAG, "Queued download with ID " + downloadID);
    }

    void startDownload() {
        registerReceiver();
        queueDownloadInDownloadManager();
    }

    private void installUpdate(Uri apkURI, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkURI, mimeType);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private Uri getDownloadedFileUri(Cursor cursor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return downloadManager.getUriForDownloadedFile(downloadID);

        //noinspection deprecation
        String localFilename = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
        File localFile = new File(localFilename);
        return Uri.fromFile(localFile);
    }

    private void handleDownloadComplete() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadID);

        Cursor cursor = downloadManager.query(query);
        cursor.moveToFirst();

        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
        Log.d(TAG, "Download id " + downloadID + " status: " + status);


        if (status == DownloadManager.STATUS_FAILED) {
            Toast.makeText(mContext, R.string.update_download_failed_message, Toast.LENGTH_LONG).show();
            unregisterReceiver();
            return;
        }

        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            unregisterReceiver();

            Uri uri = getDownloadedFileUri(cursor);
            String mimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
            Log.d(TAG, "Download URI: " + uri.toString() + ", MIME type: " + mimeType);

            installUpdate(uri, mimeType);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long completedDownloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        Log.d(TAG, "Received completion for download ID " + completedDownloadID);

        if (downloadID == completedDownloadID) {
            handleDownloadComplete();
        }
    }
}
