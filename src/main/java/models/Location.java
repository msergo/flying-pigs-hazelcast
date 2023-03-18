package models;

import lombok.Data;

@Data
public class Location {
    private String id;
    private String name;
    private double loMin;
    private double loMax;
    private double laMin;
    private double laMax;
    private long pollingInterval;
    private String createdAt;
    private String updatedAt;
}