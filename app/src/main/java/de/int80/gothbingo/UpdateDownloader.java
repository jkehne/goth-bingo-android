package de.int80.gothbingo;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

class UpdateDownloader extends BroadcastReceiver {
    private final String TAG = UpdateDownloader.class.getSimpleName();

    private final Context mContext;
    private long downloadID;
    private final DownloadManager downloadManager;

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
        request.setTitle(mContext.getString(R.string.update_notification_title));
        request.setVisibleInDownloadsUi(false);

        downloadID = downloadManager.enqueue(request);

        Log.d(TAG, "Queued download with ID " + downloadID);
    }

    void startDownload() {
        registerReceiver();
        queueDownloadInDownloadManager();
    }

    private void installUpdate(Uri apkURI, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setDataAndType(apkURI, mimeType);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    @SuppressLint("PackageManagerGetSignatures")
    private boolean verifySignature(UpdateFileWrapper file) {
        PackageManager pm = mContext.getPackageManager();

        Signature[] localSignatures;
        Signature[] fileSignatures;

        String filePath = file.getPath();

        if (filePath == null)
            return false;

        fileSignatures = pm.getPackageArchiveInfo(filePath, PackageManager.GET_SIGNATURES).signatures;

        try {
            localSignatures = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_SIGNATURES).signatures;

            for (Signature localSig : localSignatures)
                for (Signature fileSig : fileSignatures)
                    if (localSig.equals(fileSig))
                        return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        return false;
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

            UpdateFileWrapper file = UpdateFileWrapper.makeUpdateFileWrapper(mContext, downloadID);

            if (!verifySignature(file)) {
                Log.e(TAG, "Failed to verify update signature");
                Toast.makeText(mContext, R.string.update_signature_mismatch, Toast.LENGTH_LONG).show();
                return;
            }

            Uri uri = file.getUri();
            String mimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
            Log.d(TAG, "Download URI: " + uri.toString() + ", MIME type: " + mimeType);

            installUpdate(uri, mimeType);

            file.close();
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
