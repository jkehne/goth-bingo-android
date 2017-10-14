package de.int80.gothbingo;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by jens on 02.10.17.
 */

public class FieldContentFetcher extends AsyncTask<Void, Void, ArrayList<String>> {
    private static final String TAG = FieldContentFetcher.class.getSimpleName();

    private ProgressDialog dialog;
    private static final String fieldsURL = "https://int80.de/bingo/js/fields.php?pure_json=1";

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

    private String getContentsString(String url) throws IOException {
        GameState state = parentActivity.getState();

        Request.Builder builder = new Request.Builder();
        builder.url(url);

        if (state.getLastEtag() != null)
            builder.header("If-None-Match", state.getLastEtag());

        Response response = new OkHttpClient().newCall(builder.build()).execute();

        if (!response.isSuccessful())
            throw new IOException();
        else if (response.networkResponse().code() == HttpURLConnection.HTTP_NOT_MODIFIED)
            return "";

        state.setLastEtag(response.header("Etag"));

        return response.body().string();
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
