package clients;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class OpenSkyApiClient {
    private final OkHttpClient client;
    private String apiHost;

    public OpenSkyApiClient() {
        this.apiHost = "https://opensky-network.org/api";
        this.client = new OkHttpClient.Builder()
                .build();
    }

    public String getStatesForLast2Hours() throws Exception {
        return getStatesByTimeRange(System.currentTimeMillis() - 2 * 60 * 60 * 1000, System.currentTimeMillis());
    }

    public String getStatesByTimeRange(long beginTimeStamp, long endTimeStamp) throws Exception {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(apiHost + "/flights/all").newBuilder();
        urlBuilder.addQueryParameter("begin", String.valueOf(beginTimeStamp));
        urlBuilder.addQueryParameter("end", String.valueOf(endTimeStamp));

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }

        String responseBody = response.body().string();
        response.body().close();

        return responseBody;
    }
}
