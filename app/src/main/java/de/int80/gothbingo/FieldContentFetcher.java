package de.int80.gothbingo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TableRow;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by jens on 02.10.17.
 */

public class FieldContentFetcher extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = FieldContentFetcher.class.getSimpleName();

    private ProgressDialog dialog;
    private static final URL fieldsURL;

    static {URL fieldsURL1;
        try {
            fieldsURL1 = new URL("https://int80.de/bingo/js/fields.php?pure_json=1");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            fieldsURL1 = null;
        }
        fieldsURL = fieldsURL1;
    }

    private static ArrayList<String> fields = new ArrayList<String>();
    private static String lastEtag;

    private MainActivity parentActivity;

    public FieldContentFetcher(MainActivity activity) {
        dialog = new ProgressDialog(activity);
        dialog.setMessage("Getting field contents, please wait");

        parentActivity = activity;
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
        super.onPreExecute();
    }

    private String getContentsString(URL url) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("ETag", lastEtag);
        connection.connect();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED)
            return "";
        else if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
            throw new IOException();

        lastEtag = connection.getHeaderField("ETag");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        StringBuilder result = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null)
            result.append(line);

        return result.toString();
    }

    private boolean updateFieldsList(String fieldsString) {
        int i;

        try {
            JSONObject json = new JSONObject(fieldsString);

            JSONArray squares = json.getJSONArray("squares");

            fields.clear();

            for (i = 0; i < squares.length(); ++i) {
                fields.add(squares.getJSONObject(i).getString("square"));
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing failed: " + e.toString());
            return false;
        }

        return true;
    }

    private void setFieldContents(ArrayList<String> fields) {
        int row, col;
        ViewGroup root, rowHandle;
        BingoFieldView field;

        Collections.shuffle(fields);

        root = (ViewGroup) parentActivity.findViewById(R.id.BingoFieldLayout);

        for (row = 0; row < root.getChildCount(); ++row) {
            rowHandle = (ViewGroup) root.getChildAt(row);

            for (col = 0; col < rowHandle.getChildCount(); ++col) {
                if (!(rowHandle.getChildAt(col) instanceof BingoFieldView))
                    continue;

                field = (BingoFieldView) rowHandle.getChildAt(col);
                field.setText(fields.get(row * 5 + col));
            }
        }
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        String fieldsString;

        try {
            fieldsString = getContentsString(fieldsURL);
        } catch (IOException e) {
            return false;
        }

        if (!fieldsString.isEmpty())
            if (!updateFieldsList(fieldsString))
                return false;

        return true;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);

        if (aBoolean)
            setFieldContents(fields);

        if (dialog.isShowing())
            dialog.dismiss();

        if (!aBoolean) {
            AlertDialog alert = new AlertDialog.Builder(parentActivity).create();
            alert.setMessage("Download failed");
            alert.setCancelable(false);
            alert.show();
        }
    }
}
