import clients.FlyingPigsApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.pipeline.Pipeline;
import models.Location;
import models.LocationData;
import pipelines.OpenSkyFlightDataPipeline;
import pipelines.OpenSkyStatesPipeline;

import java.util.List;

public class FlyingPigs {
    public static void main(String[] args) throws Exception {
        // Get environment variables now because job will run in a cluster
        String apiHost = System.getenv("API_URL");
        String locationsUrl = apiHost + "/locations";
        String userEmail = System.getenv("USER_EMAIL");
        String userPassword = System.getenv("USER_PASSWORD");

        FlyingPigsApiClient flyingPigsApiClient = new FlyingPigsApiClient(apiHost, userEmail, userPassword);

        String response = flyingPigsApiClient.get(locationsUrl);

        ObjectMapper objectMapper = new ObjectMapper();
        LocationData locationData = objectMapper.readValue(response, LocationData.class);

        List<Location> locations = locationData.getData();

        JetInstance jet = Jet.bootstrappedInstance();

        OpenSkyFlightDataPipeline flightDataPipeline = new OpenSkyFlightDataPipeline();
        JobConfig flightDataJobConfig = new JobConfig().setName("flights-data-preloader");
        // Pipeline for preloading flight data from OpenSky /api/flights/all endpoint
        Pipeline flightDataPipelinePipeline = flightDataPipeline.createPipeline();
        try {
            jet.getJob(flightDataJobConfig.getName()).cancel();
        } finally {
            jet.newJob(flightDataPipelinePipeline, flightDataJobConfig);
        }

        locations.stream().forEach(location -> {
            JobConfig jobConfig = new JobConfig().setName(location.getName());
            jobConfig.setSnapshotIntervalMillis(60000);
            jobConfig.setProcessingGuarantee(ProcessingGuarantee.AT_LEAST_ONCE);
            OpenSkyStatesPipeline flightStats = new OpenSkyStatesPipeline(location);
            Pipeline pipeline = flightStats.createPipeline(location.getId(), apiHost, userEmail, userPassword);

            try {
                jet.getJob(location.getName()).cancel();
            } finally {
                jet.newJob(pipeline, jobConfig);
            }
        });
    }
}
