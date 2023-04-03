package models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;

import java.sql.Date;
import java.text.SimpleDateFormat;
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

    public FlightResult(String locationId, String key, StateVector startStateVector, StateVector endStateVector) {
        this.key = key;
        this.locationId = locationId;

        this.start = new Date((long) (startStateVector.getLastContact() * 1000));
        this.end = new Date((long) (endStateVector.getLastContact() * 1000));
        this.hasLanded = startStateVector.isOnGround();
        this.startLat = startStateVector.getLatitude();
        this.startLon = startStateVector.getLongitude();
        this.endLat = endStateVector.getLatitude();
        this.endLon = endStateVector.getLongitude();
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
