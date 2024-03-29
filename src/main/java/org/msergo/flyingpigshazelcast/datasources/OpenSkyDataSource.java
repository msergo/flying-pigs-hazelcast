package org.msergo.flyingpigshazelcast.datasources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.internal.util.ExceptionUtil;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.logging.ILogger;
import org.msergo.flyingpigshazelcast.models.StateVector;
import org.msergo.flyingpigshazelcast.models.StateVectorsResponse;
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

    private void fillBuffer(SourceBuilder.TimestampedSourceBuffer<StateVector> buffer) throws IOException {
        long now = System.currentTimeMillis();
        if (now < (lastPoll + pollIntervalMillis)) {
            return;
        }
        lastPoll = now;

        StateVectorsResponse stateVectorsResponse = pollForOpenSkyStates();
        stateVectorsResponse.getStates().stream().forEach(buffer::add);

        logger.info("Polled " + stateVectorsResponse.getStates().size() + " positions.");
    }

    private StateVectorsResponse pollForOpenSkyStates() throws IOException {
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
                return new StateVectorsResponse();
            }

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(responseBody, StateVectorsResponse.class);
        } catch (IOException e) {
            logger.info("Error while polling OpenSky API: " + e.getMessage());
            return new StateVectorsResponse();
        }
    }

    public static StreamSource<StateVector> getDataSource(long pollIntervalMillis) {
        return SourceBuilder.timestampedStream("OpenSky Data Source",
                        ctx -> new OpenSkyDataSource(ctx.logger(), "https://opensky-network.org/api/states/all", pollIntervalMillis))
                .fillBufferFn(OpenSkyDataSource::fillBuffer)
                .build();
    }
}
