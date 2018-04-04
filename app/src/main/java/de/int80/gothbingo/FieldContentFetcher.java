package de.int80.gothbingo;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FieldContentFetcher extends AsyncTask<Void, Void, ArrayList<String>> {
    private static final String TAG = FieldContentFetcher.class.getSimpleName();

    private ProgressDialog dialog;
    private static final String fieldsURL = "https://int80.de/bingo/js/fields.php?pure_json=1";
    private Throwable lastError = null;

    @SuppressLint("StaticFieldLeak")
    private MainActivity parentActivity;

    FieldContentFetcher(MainActivity activity) {
        dialog = new ProgressDialog(activity);
        dialog.setMessage(activity.getString(R.string.fields_downloading_message));

        parentActivity = activity;
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
        super.onPreExecute();
    }

    @SuppressWarnings("ConstantConditions")
    private String getContentsString() throws IOException {
        GameState state = parentActivity.getState();

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        clientBuilder.cache(new Cache(parentActivity.getCacheDir(), 1024 * 1024));
        OkHttpClient client = clientBuilder.build();

        Request.Builder builder = new Request.Builder();
        builder.url(fieldsURL);

        Response response = client.newCall(builder.build()).execute();

        if (!response.isSuccessful())
            throw new IOException(response.message());
        else if ((response.networkResponse().code() == HttpURLConnection.HTTP_NOT_MODIFIED) && (state.getAllFields() != null))
            return "";

        return response.body().string();
    }

    private ArrayList<String> buildFieldsList(String fieldsString) {
        int i;
        ArrayList<String> result = new ArrayList<>();

        try {
            JSONObject json = new JSONObject(fieldsString);

            JSONArray squares = json.getJSONArray("squares");

            for (i = 0; i < squares.length(); ++i) {
                result.add(squares.getJSONObject(i).getString("square"));
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing failed: ", e);
            lastError = e;
            return null;
        }

        return result;
    }

    @Override
    protected ArrayList<String> doInBackground(Void... voids) {
        String fieldsString;
        ArrayList<String> result = null;

        try {
            fieldsString = getContentsString();
        } catch (IOException e) {
            Log.e(TAG, "Field download failed: ", e);
            lastError = e;
            return null;
        }

        if (!fieldsString.isEmpty())
            result = buildFieldsList(fieldsString);

        return result;
    }

    @Override
    protected void onPostExecute(ArrayList<String> result) {
        super.onPostExecute(result);

        parentActivity.setFieldContents(result, false, lastError);

        if (dialog.isShowing())
            dialog.dismiss();
    }
}
