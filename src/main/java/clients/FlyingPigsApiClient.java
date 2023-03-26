package clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import models.FlyingPigsApiAuthRequest;
import models.FlyingPigsApiAuthResult;
import okhttp3.*;

import java.io.IOException;

public class FlyingPigsApiClient {
    private final OkHttpClient client;
    private String token;
    private String userEmail;
    private String userPassword;
    private String apiHost;

    public FlyingPigsApiClient(String apiHost, String userEmail, String userPassword) throws Exception {
        this.userEmail = userEmail;
        this.userPassword = userPassword;
        this.apiHost = apiHost;

        client = new OkHttpClient.Builder()
                .addInterceptor(new TokenInterceptor())
                .build();
    }

    public String post(String url, String body) throws Exception {
        RequestBody requestBody = RequestBody.create(body, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
        String responseBody = response.body().string();
        response.body().close();
        return responseBody;
    }

    public String get(String url) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
        String responseBody = response.body().string();
        response.body().close();
        return responseBody;
    }

    private void obtainToken() throws Exception {
        FlyingPigsApiAuthRequest requestBody = new FlyingPigsApiAuthRequest(userEmail, userPassword);
        RequestBody body = RequestBody.create(requestBody.toJsonString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(apiHost + "/authentication")
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Failed to obtain token");
        }

        String responseBody = response.body().string();
        // Parse the JWT token from the response body
        // This assumes that the token is returned as a JSON object with a "token" field
        this.token = parseTokenFromResponse(responseBody);
    }

    private String parseTokenFromResponse(String responseBody) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        FlyingPigsApiAuthResult result = mapper.readValue(responseBody, FlyingPigsApiAuthResult.class);
        return result.getAccessToken();
    }

    private class TokenInterceptor implements Interceptor {
        @SneakyThrows
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request.Builder requestBuilder = originalRequest.newBuilder();

            if (token != null) {
                requestBuilder.header("Authorization", "Bearer " + token);
            }

            Response response = chain.proceed(requestBuilder.build());

            // Handle token expiration
            if (response.code() == 401) {
                response.close();
                obtainToken();
                requestBuilder.header("Authorization", "Bearer " + token);
                return chain.proceed(requestBuilder.build());
            }

            return response;
        }
    }
}