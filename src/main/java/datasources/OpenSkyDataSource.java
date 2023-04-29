package datasources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.internal.util.ExceptionUtil;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.logging.ILogger;
import models.OpenSkyStates;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class OpenSkyDataSource {
    private final URL url;
    private final long pollIntervalMillis;

    private final ILogger logger;

    private long lastPoll;

    private OkHttpClient client;

    private OpenSkyDataSource(ILogger logger, String url, long pollIntervalMillis) {
        this.logger = logger;
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw ExceptionUtil.rethrow(e);
        }

        this.pollIntervalMillis = pollIntervalMillis;
        this.client = new OkHttpClient();
    }

    private void fillBuffer(SourceBuilder.TimestampedSourceBuffer<OpenSkyStates> buffer) throws IOException {
        long now = System.currentTimeMillis();
        if (now < (lastPoll + pollIntervalMillis)) {
            return;
        }
        lastPoll = now;

        OpenSkyStates openSkyStates = pollForOpenSkyStates();
        buffer.add(openSkyStates);

        logger.info("Polled " + openSkyStates.getStates().size() + " positions.");
    }

    private OpenSkyStates pollForOpenSkyStates() throws IOException {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            Call call = client.newCall(request);
            Response response = call.execute();
            int responseCode = response.code();
            String responseBody = response.body().string();

            if (responseCode != 200) {
                logger.info("API returned error: " + response.code() + " " + response);
                return new OpenSkyStates();
            }

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(responseBody, OpenSkyStates.class);
        } catch (IOException e) {
            logger.info("Error while polling OpenSky API: " + e.getMessage());
            return new OpenSkyStates();
        }
    }

    public static StreamSource<OpenSkyStates> getDataSource(long pollIntervalMillis) {
        return SourceBuilder.timestampedStream("OpenSky Data Source",
                        ctx -> new OpenSkyDataSource(ctx.logger(), "https://opensky-network.org/api/states/all", pollIntervalMillis))
                .fillBufferFn(OpenSkyDataSource::fillBuffer)
                .build();
    }
}
