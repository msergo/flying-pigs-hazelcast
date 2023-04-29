package sinks;

import clients.FlyingPigsApiClient;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import models.FlightResult;

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
