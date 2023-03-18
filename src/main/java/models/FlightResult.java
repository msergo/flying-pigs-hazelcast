package models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.datamodel.Tuple2;
import lombok.Data;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

@Data
public class FlightResult {
    private String key;
    private String locationId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date start;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date end;
    private boolean hasLanded;
    private double startLat;
    private double startLon;
    private double endLat;
    private double endLon;

    public FlightResult(KeyedWindowResult<String, Tuple2<List<StateVector>, List<StateVector>>> keyedWindowResult, String locationId) {
        StateVector startStateVector = keyedWindowResult.getValue().f0().get(0);
        StateVector endStateVector = keyedWindowResult.getValue().f1().get(0);

        this.key = keyedWindowResult.key();
        this.start = new Date(keyedWindowResult.start());
        this.end = new Date(keyedWindowResult.end());
        this.hasLanded = startStateVector.isOnGround();
        this.startLat = startStateVector.getLatitude();
        this.startLon = startStateVector.getLongitude();
        this.endLat = endStateVector.getLatitude();
        this.endLon = endStateVector.getLongitude();
        this.locationId = locationId;
    }

    public String toJsonString() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        mapper.setDateFormat(dateFormat);
        return mapper.writeValueAsString(this);
    }
}
