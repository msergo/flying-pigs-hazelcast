import com.hazelcast.function.ComparatorEx;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.*;
import datasources.OpenSkyDataSource;
import models.FlightResult;
import models.Location;
import models.StateVector;
import okhttp3.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class OpenSkyFlightStats {
    private double loMin;
    private double loMax;
    private double laMin;
    private double laMax;
    private long pollingInterval;

    public OpenSkyFlightStats(long loMin, long loMax, long laMin, long laMax, long pollingInterval, String locationId) {
        this.loMin = loMin;
        this.loMax = loMax;
        this.laMin = laMin;
        this.laMax = laMax;
        this.pollingInterval = pollingInterval;
    }

    public OpenSkyFlightStats(Location location) {
        this.loMin = location.getLoMin();
        this.loMax = location.getLoMax();
        this.laMin = location.getLaMin();
        this.laMax = location.getLaMax();
        this.pollingInterval = location.getPollingInterval();
    }

    public Pipeline createPipeline(String locationId) {
        Pipeline pipeline = Pipeline.create();

        Sink httpSink = SinkBuilder.<OkHttpClient>sinkBuilder(
                        "http-sink", ctx -> new OkHttpClient())
                .receiveFn((client, item) -> {
                    FlightResult flightResult = new FlightResult((KeyedWindowResult<String, Tuple2<List<StateVector>, List<StateVector>>>) item, locationId);
                    Request request = new Request.Builder()
                            .url(System.getenv("SINK_URL"))
                            .post(RequestBody.create(MediaType.parse("application/json"), flightResult.toJsonString()))
                            .build();

                    Call call = client.newCall(request);
                    call.execute();
                })
                .build();

        String url = new HttpUrl.Builder()
                .scheme("https")
                .host("opensky-network.org")
                .addPathSegment("api/states/all")
                .addQueryParameter("lamin", String.valueOf(laMin))
                .addQueryParameter("lomin", String.valueOf(loMin))
                .addQueryParameter("lamax", String.valueOf(laMax))
                .addQueryParameter("lomax", String.valueOf(loMax))
                .build()
                .toString();

        // Read from http
        pipeline.readFrom(OpenSkyDataSource.getDataSource(url, pollingInterval))
                .withIngestionTimestamps()
                .groupingKey(message -> message.getCallsign().trim())
                .window(WindowDefinition.session(TimeUnit.MINUTES.toMillis(10)))
                .aggregate(AggregateOperations.allOf(
                        AggregateOperations.bottomN(1, ComparatorEx.comparing(StateVector::getLastContact)),
                        AggregateOperations.topN(1, ComparatorEx.comparing(StateVector::getLastContact)))
                )
                .writeTo(httpSink);

        return pipeline;
    }
}
