package de.int80.gothbingo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class AppUpdater extends AsyncTask<Void, Void, Boolean> {

    private final String TAG = AppUpdater.class.getSimpleName();

    @SuppressLint("StaticFieldLeak")
    private final Context mContext;

    AppUpdater(Context context) {
        mContext = context;
    }

    private long parseLastModifiedString(String lastModified) {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        try {
            Date date = format.parse(lastModified);
            return date.getTime();
        } catch (ParseException e) {
            Log.e(TAG, "Unable to parse Last-Modified header: " + lastModified, e);
            return -1;
        }
    }

    private long getLastModifiedTime(Response response) {
        Headers responseHeaders = response.headers();
        String lastModified = responseHeaders.get("Last-Modified");

        Log.d(TAG, "Got last modified time: " + lastModified);

        return parseLastModifiedString(lastModified);
    }

    private long getAPKTime() {
        final OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(mContext.getString(R.string.app_url))
                .head()
                .build();

        try {
            Response response = client.newCall(request).execute();

            return getLastModifiedTime(response);
        } catch (IOException e) {
            Log.e(TAG,"Update check failed", e);
            return -1;
        }
    }

    private long getLastUpdateTime() {
        PackageManager pm = mContext.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(mContext.getPackageName(), 0);
            return info.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to find package name: " + mContext.getPackageName(), e);
            return -1;
        }
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        long apkTime = getAPKTime();
        long lastUpdateTime = getLastUpdateTime();

        if (lastUpdateTime == -1) {
            Log.e(TAG, "Failed to get last update time, aborting");
            return false;
        }

        Log.d(TAG, "Last update time: " + lastUpdateTime + ", server APK time: " + apkTime);

        return apkTime > lastUpdateTime;
    }

    private void queueUpdateDownload() {

    }

    private Context getDialogContext() {
        Context dialogContext = LoginActivity.getCurrentInstance();

        if (dialogContext == null)
            dialogContext = MainActivity.getCurrentInstance();

        if (dialogContext == null)
            Log.e(TAG, "Unable to find a valid context for update message");

        return dialogContext;
    }

    private void showUpdateAvailableDialog() {
        Context dialogContext = getDialogContext();
        if (dialogContext == null)
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);

        builder.setTitle(R.string.update_available_title);
        builder.setMessage(R.string.update_available_message);

        builder.setPositiveButton(mContext.getString(R.string.yes_action_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                queueUpdateDownload();
            }
        });

        builder.setNegativeButton(mContext.getString(R.string.no_action_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.show();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if (result)
            showUpdateAvailableDialog();
    }
}
