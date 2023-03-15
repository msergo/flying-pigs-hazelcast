package datasources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.internal.util.ExceptionUtil;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.logging.ILogger;
import models.OpenSkyStates;
import models.StateVector;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class OpenSkyDataSource {
    private final URL url;
    private final long pollIntervalMillis;

    private final ILogger logger;

    private long lastPoll;

    private OpenSkyDataSource(ILogger logger, String url, long pollIntervalMillis) {
        this.logger = logger;
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw ExceptionUtil.rethrow(e);
        }
        this.pollIntervalMillis = pollIntervalMillis;
    }


    private void fillBuffer(SourceBuilder.TimestampedSourceBuffer<StateVector> buffer) throws IOException {
        long now = System.currentTimeMillis();
        if (now < (lastPoll + pollIntervalMillis)) {
            return;
        }
        lastPoll = now;

        OpenSkyStates openSkyStates = pollForOpenSkyStates();
        openSkyStates.getStates().stream().forEach(buffer::add);

        logger.info("Polled " + openSkyStates.getStates().size() + " positions.");
    }

    private OpenSkyStates pollForOpenSkyStates() throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        StringBuilder response = new StringBuilder();
        try {
            con.setRequestMethod("GET");
            con.addRequestProperty("User-Agent", "Mozilla / 5.0 (Windows NT 6.1; WOW64) AppleWebKit / 537.36 (KHTML, like Gecko) Chrome / 40.0.2214.91 Safari / 537.36");
            int responseCode = con.getResponseCode();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }
            if (responseCode != 200) {
                logger.info("API returned error: " + responseCode + " " + response);
                return new OpenSkyStates();
            }
        } finally {
            con.disconnect();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        // Deserialize the JSON data into an OpenSkyStatesResponse instance
        OpenSkyStates openSkyStates = objectMapper.readValue(response.toString(), OpenSkyStates.class);

        return openSkyStates;
    }

    public static StreamSource<StateVector> getDataSource(String url, long pollIntervalMillis) {
        return SourceBuilder.timestampedStream("OpenSky Data Source",
                        ctx -> new OpenSkyDataSource(ctx.logger(), url, pollIntervalMillis))
                .fillBufferFn(OpenSkyDataSource::fillBuffer)
                .build();
    }
}
