package pipelines;

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import datasources.OpenSkyFlightData;
import models.FlightData;

import java.util.concurrent.TimeUnit;

public class OpenSkyFlightDataPipeline {
    public Pipeline createPipeline() {
        Pipeline pipeline = Pipeline.create();

        Sink<FlightData[]> httpToImapSink = SinkBuilder.<JetInstance>sinkBuilder(
                        "flight-data-sink",
                        ctx -> Jet.bootstrappedInstance())
                .receiveFn((JetInstance jetInstance, FlightData[] items) -> {
                    jetInstance.getMap("all-flights").clear();

                    for (FlightData item : items) {
                        jetInstance.getMap("all-flights").put(item.getIcao24(), item);
                    }

                    System.out.println("Cached Flight data: " + jetInstance.getMap("all-flights").size());
                })
                .build();

        pipeline.readFrom(OpenSkyFlightData.getDataSource(TimeUnit.MINUTES.toMillis(5)))
                .withoutTimestamps()
                .writeTo(httpToImapSink);

        return pipeline;
    }
}
