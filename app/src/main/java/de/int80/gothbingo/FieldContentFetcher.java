package de.int80.gothbingo;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class FieldContentFetcher extends AsyncTask<Void, Void, List<String>> {
    private static final String TAG = FieldContentFetcher.class.getSimpleName();

    private Throwable lastError = null;
    private IMainActivityModel model;

    public FieldContentFetcher(IMainActivityModel model) {
        this.model = model;
    }

    @SuppressWarnings("ConstantConditions")
    private String getContentsString() throws IOException {
        OkHttpClient client = HTTPClientFactory.getHTTPClient();

        Request.Builder builder = new Request.Builder();
        builder.url(GothBingo.getContext().getString(R.string.fields_url));

        Response response = client.newCall(builder.build()).execute();

        if (!response.isSuccessful())
            throw new IOException(response.message());

            if ((response.networkResponse().code() == HttpURLConnection.HTTP_NOT_MODIFIED))
                return "";

        return response.body().string();
    }

    private List<String> buildFieldsList(String fieldsString) {
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
    protected List<String> doInBackground(Void... voids) {
        String fieldsString;
        List<String> result = null;

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
    protected void onPostExecute(List<String> result) {
        super.onPostExecute(result);
        model.onFieldContentFetcherComplete(result, lastError);
    }
}
