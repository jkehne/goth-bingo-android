package de.int80.gothbingo;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

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

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by jens on 02.10.17.
 */

public class FieldContentFetcher extends AsyncTask<Void, Void, ArrayList<String>> {
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
        connection.setRequestProperty("If-None-Match", lastEtag);
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

    private ArrayList<String> buildFieldsList(String fieldsString) {
        int i;
        ArrayList<String> result = new ArrayList<String>();

        try {
            JSONObject json = new JSONObject(fieldsString);

            JSONArray squares = json.getJSONArray("squares");

            for (i = 0; i < squares.length(); ++i) {
                result.add(squares.getJSONObject(i).getString("square"));
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing failed: " + e.toString());
            return null;
        }

        return result;
    }

    @Override
    protected ArrayList<String> doInBackground(Void... voids) {
        String fieldsString;
        ArrayList<String> result = null;

        try {
            fieldsString = getContentsString(fieldsURL);
        } catch (IOException e) {
            return null;
        }

        if (!fieldsString.isEmpty())
            result = buildFieldsList(fieldsString);

        return result;
    }

    @Override
    protected void onPostExecute(ArrayList<String> result) {
        super.onPostExecute(result);

        parentActivity.setFieldContents(result, false);

        if (dialog.isShowing())
            dialog.dismiss();
    }
}
