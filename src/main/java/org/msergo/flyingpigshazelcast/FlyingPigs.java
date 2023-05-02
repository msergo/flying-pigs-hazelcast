package org.msergo.flyingpigshazelcast;

import org.msergo.flyingpigshazelcast.models.FlightResult;
import org.msergo.flyingpigshazelcast.models.Location;
import org.msergo.flyingpigshazelcast.models.LocationData;
import org.msergo.flyingpigshazelcast.models.StateVector;
import org.msergo.flyingpigshazelcast.clients.FlyingPigsApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.StreamSource;
import com.typesafe.config.Config;
import org.msergo.flyingpigshazelcast.config.ConfigManager;
import org.msergo.flyingpigshazelcast.datasources.RabbitMQDataSource;
import org.msergo.flyingpigshazelcast.pipelines.OpenSkyPipeline;
import org.msergo.flyingpigshazelcast.sinks.HttpSink;

import java.util.List;

public class FlyingPigs {
    public static void main(String[] args) throws Exception {
        // Get environment variables here now because job will run in a cluster
        Config config = ConfigManager.getConfig();

        String apiHost = config.getString("api.host");
        String locationsUri = apiHost + config.getString("api.locationsUri");
        String userEmail = config.getString("api.userEmail");
        String userPassword = config.getString("api.userPassword");

        FlyingPigsApiClient flyingPigsApiClient = new FlyingPigsApiClient(apiHost, userEmail, userPassword);

        String response = flyingPigsApiClient.get(locationsUri);

        ObjectMapper objectMapper = new ObjectMapper();
        LocationData locationData = objectMapper.readValue(response, LocationData.class);
        List<Location> locations = locationData.getData();

        JetInstance jet = Jet.bootstrappedInstance();

        locations.stream().forEach(location -> {
            JobConfig jobConfig = new JobConfig().setName(location.getName());
            jobConfig.setSnapshotIntervalMillis(60000);
            jobConfig.setProcessingGuarantee(ProcessingGuarantee.AT_LEAST_ONCE);

            Sink<FlightResult> httpSink = HttpSink.getSink(apiHost, userEmail, userPassword);
            StreamSource<StateVector> source = RabbitMQDataSource.getDataSource(location);

            Pipeline pipeline = OpenSkyPipeline.createPipeline(location, source, httpSink);

            try {
                jet.getJob(location.getName()).cancel();
            } finally {
                jet.newJob(pipeline, jobConfig);
            }
        });
    }
}
