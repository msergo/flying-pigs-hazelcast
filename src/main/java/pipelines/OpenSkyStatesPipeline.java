package pipelines;

import clients.FlyingPigsApiClient;
import com.hazelcast.function.ComparatorEx;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.SinkBuilder;
import com.hazelcast.jet.pipeline.WindowDefinition;
import datasources.OpenSkyDataSource;
import models.FlightResult;
import models.FlightsWithingTimeRange;
import models.Location;
import models.StateVector;
import okhttp3.HttpUrl;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class OpenSkyStatesPipeline {
    private double loMin;
    private double loMax;
    private double laMin;
    private double laMax;
    private long pollingInterval;

    public OpenSkyStatesPipeline(long loMin, long loMax, long laMin, long laMax, long pollingInterval, String locationId) {
        this.loMin = loMin;
        this.loMax = loMax;
        this.laMin = laMin;
        this.laMax = laMax;
        this.pollingInterval = pollingInterval;
    }

    public OpenSkyStatesPipeline(Location location) {
        this.loMin = location.getLoMin();
        this.loMax = location.getLoMax();
        this.laMin = location.getLaMin();
        this.laMax = location.getLaMax();
        this.pollingInterval = location.getPollingInterval();
    }

    public Pipeline createPipeline(String locationId, String apiHost, String userEmail, String userPassword) {
        Pipeline pipeline = Pipeline.create();
        String sinkUrl = apiHost + "/flights";

        Sink<FlightResult> httpSink = SinkBuilder.<FlyingPigsApiClient>sinkBuilder(
                        "http-sink", ctx -> new FlyingPigsApiClient(apiHost, userEmail, userPassword))
                .receiveFn((FlyingPigsApiClient client, FlightResult item) -> {
                    client.post(sinkUrl, item.toJsonString());
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
                .groupingKey(message -> message.getIcao24().trim())
                .window(WindowDefinition.session(TimeUnit.MINUTES.toMillis(5)))
                .aggregate(AggregateOperations.toList())
                .filter((KeyedWindowResult<String, List<StateVector>> list) -> {
                    if (list.result().size() < 2) {
                        return false;
                    }
                    // Filter out flights that were on the ground only
                    boolean allIsOnGround = list.result().stream().allMatch(StateVector::isOnGround);

                    return !allIsOnGround;
                })
                .map((KeyedWindowResult<String, List<StateVector>> list) -> {
                    ComparatorEx<StateVector> comparator = ComparatorEx.comparing(StateVector::getLastContact);
                    StateVector startStateVector = list.result().stream().min(comparator).get();
                    StateVector endStateVector = list.result().stream().max(comparator).get();

                    return new FlightResult(locationId, startStateVector, endStateVector);
                })
                .mapUsingIMap("all-flights", FlightResult::getIcao24, (FlightResult flightResult, FlightsWithingTimeRange flightsWithingTimeRange) -> {
                    if (flightsWithingTimeRange == null) {
                        return flightResult;
                    }

                    flightResult.setEstArrivalAirport(flightsWithingTimeRange.getEstArrivalAirport());
                    flightResult.setEstDepartureAirport(flightsWithingTimeRange.getEstDepartureAirport());

                    return flightResult;
                })
                .writeTo(httpSink);

        return pipeline;
    }
}
