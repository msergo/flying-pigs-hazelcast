package datasources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.logging.ILogger;
import models.FlightData;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OpenSkyFlightDataSource {
    private final long pollIntervalMillis;

    private final ILogger logger;

    private long lastPoll;

    private OkHttpClient client;

    private OpenSkyFlightDataSource(ILogger logger, long pollIntervalMillis) {
        this.logger = logger;

        this.pollIntervalMillis = pollIntervalMillis;
        this.client = new OkHttpClient();
    }

    private void fillBuffer(SourceBuilder.TimestampedSourceBuffer<FlightData> buffer) throws IOException {
        long now = System.currentTimeMillis();
        if (now < (lastPoll + pollIntervalMillis)) {
            return;
        }
        lastPoll = now;

        FlightData[] flightData = pollData();
        logger.info("Polled " + flightData.length + " positions.");

        for (FlightData data : flightData) {
            buffer.add(data);
        }
    }

    private FlightData[] pollData() throws IOException {
        List<FlightData> emptyResult = new ArrayList<>();
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://opensky-network.org/api/flights/all").newBuilder();
        Long ts2hrsAgoInSec = (System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2)) / 1000;
        Long currentTsInSec = System.currentTimeMillis() / 1000;
        urlBuilder.addQueryParameter("begin", String.valueOf(ts2hrsAgoInSec));
        urlBuilder.addQueryParameter("end", String.valueOf(currentTsInSec));

        try {
            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .build();

            Call call = client.newCall(request);
            Response response = call.execute();
            int responseCode = response.code();
            String responseBody = response.body().string();

            if (responseCode != 200) {
                logger.info("API returned error: " + response.code() + " " + response);
                return emptyResult.toArray(new FlightData[0]);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(responseBody, FlightData[].class);
        } catch (IOException e) {
            logger.info("Error while polling OpenSky API: " + e.getMessage());
            return emptyResult.toArray(new FlightData[0]);
        }
    }

    public static StreamSource<FlightData> getDataSource(long pollIntervalMillis) {
        return SourceBuilder.timestampedStream("OpenSky FlightData Source",
                        ctx -> new OpenSkyFlightDataSource(ctx.logger(), pollIntervalMillis))
                .fillBufferFn(OpenSkyFlightDataSource::fillBuffer)
                .build();
    }
}
