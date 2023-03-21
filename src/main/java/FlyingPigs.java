import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import models.Location;
import models.LocationData;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import pipelines.OpenSkyFlightStats;

import java.io.IOException;
import java.util.List;

public class FlyingPigs {
    public static void main(String[] args) throws IOException {
        String locationsUrl = System.getenv("LOCATIONS_URL");
        String sinkUrl = System.getenv("SINK_URL");

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(locationsUrl)
                .build();

        Call call = client.newCall(request);
        String response = call.execute().body().string();
        ObjectMapper objectMapper = new ObjectMapper();
        LocationData locationData = objectMapper.readValue(response, LocationData.class);

        List<Location> locations = locationData.getData();

        JetInstance jet = Jet.bootstrappedInstance();

        locations.stream().forEach(location -> {
            JobConfig jobConfig = new JobConfig().setName(location.getName());
            OpenSkyFlightStats flightStats = new OpenSkyFlightStats(location);
            Pipeline pipeline = flightStats.createPipeline(location.getId(), sinkUrl);

            jet.newJob(pipeline, jobConfig);
        });
    }
}
