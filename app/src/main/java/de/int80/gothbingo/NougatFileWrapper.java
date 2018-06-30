package de.int80.gothbingo;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class NougatFileWrapper extends UpdateFileWrapper {
    private String localPath;

    NougatFileWrapper(Context context, long downloadID) {
        super(context, downloadID);

        copyFileToData();
    }

    @Override
    Uri getUri() {
        return downloadManager.getUriForDownloadedFile(downloadID);
    }

    private void copyFileToData() {
        ParcelFileDescriptor pfd;
        try {
            pfd = downloadManager.openDownloadedFile(downloadID);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        InputStream is = new FileInputStream(pfd.getFileDescriptor());

        localPath = mContext.getCacheDir().getPath() + "/update.apk";
        OutputStream os;
        try {
            os = new FileOutputStream(localPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            localPath = null;
            try {
                is.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }

        int len;
        byte[] buf = new byte[1024];
        try {
            while ((len = is.read(buf)) > 0)
                os.write(buf, 0, len);
        } catch (IOException e) {
            e.printStackTrace();
            localPath = null;
            try {
                is.close();
                os.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }

        try {
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    String getPath() {
        return localPath;
    }

    @Override
    void close() {
        if (localPath != null) {
            //noinspection ResultOfMethodCallIgnored
            new File(localPath).delete();
            localPath = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
