import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.Pipeline;
import models.dto.Location;
import models.dto.LocationData;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.util.List;

public class FlyingPigs {
    public static void main(String[] args) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(System.getenv("LOCATIONS_URL"))
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
            Pipeline pipeline = flightStats.createPipeline();
            jet.newJob(pipeline, jobConfig);
        });
    }
}
