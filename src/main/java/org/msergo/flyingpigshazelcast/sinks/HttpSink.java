package org.msergo.flyingpigshazelcast.sinks;

import org.msergo.flyingpigshazelcast.clients.FlyingPigsApiClient;
import org.msergo.flyingpigshazelcast.models.FlightResult;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;

public final class HttpSink {
    public static Sink<FlightResult> getSink(String apiHost, String userEmail, String userPassword) {
        return SinkBuilder.<FlyingPigsApiClient>sinkBuilder(
                        "http-sink", ctx -> new FlyingPigsApiClient(apiHost, userEmail, userPassword))
                .receiveFn((FlyingPigsApiClient client, FlightResult item) -> {
                    client.post(apiHost + "/flights", item.toJsonString());
                })
                .build();
    }
}
