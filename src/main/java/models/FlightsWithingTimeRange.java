package models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FlightsWithingTimeRange {
    private String icao24;

    private long firstSeen;

    private String estDepartureAirport;

    private long lastSeen;

    private String estArrivalAirport;

    private String callsign;

    private int estDepartureAirportHorizDistance;

    private int estDepartureAirportVertDistance;

    private int estArrivalAirportHorizDistance;

    private int estArrivalAirportVertDistance;

    private int departureAirportCandidatesCount;

    private int arrivalAirportCandidatesCount;
}
