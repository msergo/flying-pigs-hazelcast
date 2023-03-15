package models.dto;

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
//
//    public Location(String name, long loMin, long loMax, long laMin, long laMax, long pollingInterval) {
//        this.name = name;
//        this.loMin = loMin;
//        this.loMax = loMax;
//        this.laMin = laMin;
//        this.laMax = laMax;
//        this.pollingInterval = pollingInterval;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public long getLoMin() {
//        return loMin;
//    }
//
//    public long getLoMax() {
//        return loMax;
//    }
//
//    public long getLaMin() {
//        return laMin;
//    }
//
//    public long getLaMax() {
//        return laMax;
//    }
//
//    public long getPollingInterval() {
//        return pollingInterval;
//    }
}