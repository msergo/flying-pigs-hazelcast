package models;

import lombok.Data;

import java.util.List;

@Data
public class LocationData {
    private int total;
    private int skip;
    private int limit;
    private List<Location> data;
}
