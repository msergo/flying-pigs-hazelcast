package pipelines;

import com.hazelcast.function.ComparatorEx;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.pipeline.*;
import models.FlightResult;
import models.Location;
import models.StateVector;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class OpenSkyPipeline {
    public static Pipeline createPipeline(Location location, StreamSource<StateVector> source, Sink<FlightResult> sink) {
        Pipeline pipeline = Pipeline.create();

        String locationId = location.getId();
        double loMin = location.getLoMin();
        double loMax = location.getLoMax();
        double laMin = location.getLaMin();
        double laMax = location.getLaMax();

        if (sink == null) {
            sink = Sinks.logger();
        }

        pipeline.readFrom(source)
                .withIngestionTimestamps()
                .filter((StateVector stateVector) -> {
                    try {
                        double longitude = stateVector.getLongitude();
                        double latitude = stateVector.getLatitude();

                        if (longitude == 0 || latitude == 0) {
                            return false;
                        }

                        return longitude >= loMin && longitude <= loMax &&
                                latitude >= laMin && latitude <= laMax;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .groupingKey((StateVector stateVector) -> stateVector.getIcao24().trim())
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
                .writeTo(sink);

        return pipeline;
    }
}
