package de.int80.gothbingo;

import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

class HTTPClientFactory {
    private static OkHttpClient client;

    private static OkHttpClient makeClient() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        clientBuilder.cache(new Cache(GothBingo.getContext().getCacheDir(), 1024 * 1024));
        return clientBuilder.build();
    }

    static OkHttpClient getHTTPClient() {
        if (client == null)
            client = makeClient();
        return client;
    }

    static OkHttpClient getWebsocketClient() {
        if (client == null)
            client = makeClient();
        return client.newBuilder().pingInterval(30, TimeUnit.SECONDS).build();
    }

}
