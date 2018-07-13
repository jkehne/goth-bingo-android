package de.int80.gothbingo;

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

class FieldContentFetcher extends AsyncTask<Void, Void, ArrayList<String>> {
    private static final String TAG = FieldContentFetcher.class.getSimpleName();

    private Throwable lastError = null;

    private static boolean running;

    static boolean isRunning() {
        return running;
    }

    @Override
    protected void onPreExecute() {
        MainActivity parentActivity = MainActivity.getCurrentInstance();
        parentActivity.showProgressDialog(parentActivity.getString(R.string.fields_downloading_message));
        running = true;
        super.onPreExecute();
    }

    @SuppressWarnings("ConstantConditions")
    private String getContentsString() throws IOException {
        MainActivity parentActivity = MainActivity.getCurrentInstance();

        if (parentActivity == null)
            return "";

        GameState state = parentActivity.getState();

        OkHttpClient client = HTTPClientFactory.getHTTPClient();

        Request.Builder builder = new Request.Builder();
        builder.url(GothBingo.getContext().getString(R.string.fields_url));

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

        MainActivity parentActivity = MainActivity.getCurrentInstance();
        running = false;

        if (parentActivity != null) {
            parentActivity.setFieldContents(result, false, lastError);
            parentActivity.dismissProgressDialog();
        }
    }
}
