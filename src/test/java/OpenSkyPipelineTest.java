import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.test.AssertionCompletedException;
import models.FlightResult;
import models.Location;
import models.StateVector;
import okhttp3.OkHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import pipelines.OpenSkyPipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

import static com.hazelcast.jet.pipeline.test.AssertionSinks.assertCollectedEventually;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class OpenSkyPipelineTest {
    private static JetInstance jet;

    @BeforeClass
    public static void setup() {
        jet = Jet.newJetInstance();
    }

    @AfterClass
    public static void teardown() {
        Jet.shutdownAll();
    }

    @Test
    public void testCollectingStateVectors() {
        Location location = new Location();
        location.setName("test-location");
        location.setId("test-location-id");
        location.setLaMin(100);
        location.setLaMax(200);
        location.setLoMin(100);
        location.setLoMax(200);
        location.setPollingInterval(100);

        StateVector stateVector1 = new StateVector("4aca07");
        stateVector1.setLatitude(111.11);
        stateVector1.setLongitude(111.11);
        stateVector1.setLastContact(1682972714.0);

        StateVector stateVector2 = new StateVector("4aca07");
        stateVector2.setLatitude(122.22);
        stateVector2.setLongitude(122.22);
        stateVector2.setLastContact(1682972887.0);

        // create list of 2 elements
        List<StateVector> list = new ArrayList<>();
        list.add(stateVector1);
        list.add(stateVector2);

        StreamSource<StateVector> source = SourceBuilder.timestampedStream("source", ctx -> new OkHttpClient())
                .fillBufferFn((OkHttpClient client, SourceBuilder.TimestampedSourceBuffer<StateVector> buffer) -> {
                    if (list.isEmpty()) {
                        return;
                    }
                    // remove the first element from the list
                    StateVector stateVector = list.remove(0);
                    // add the element to the buffer
                    buffer.add(stateVector);
                })
                .build();

        Pipeline pipeline = OpenSkyPipeline.createPipeline(location,
                source,
                assertCollectedEventually(3, (List<FlightResult> c) -> {
                    assertEquals("Job produced 1 FlightResult item", 1, c.size());
                }));

        // It is expected that the job will fail with CompletionException
        try {
            jet.newJob(pipeline).join();
        } catch (CompletionException e) {
            // AssertionCompletedException is used to terminate the streaming job
            if (!(e.getCause().getCause() instanceof AssertionCompletedException)) {
                fail(e.getMessage());
            }
        }
    }
}

