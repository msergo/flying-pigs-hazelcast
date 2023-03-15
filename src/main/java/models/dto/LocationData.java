package models.dto;

import lombok.Data;
import models.dto.Location;

import java.util.List;

@Data
public class LocationData {
    private int total;
    private int skip;
    private int limit;
    private List<Location> data;
}
