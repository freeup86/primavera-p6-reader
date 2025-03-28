package com.example.primaverap6reader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Activity {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("ObjectId")
    private String objectId;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("ProjectId")
    private String projectId;

    @JsonProperty("ProjectObjectId")
    private String projectObjectId;

    @JsonProperty("StartDate")
    private Date startDate;

    @JsonProperty("FinishDate")
    private Date finishDate;

    @JsonProperty("PlannedDuration")
    private Double plannedDuration;

    @JsonProperty("Type")
    private String type;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("WBSObjectId")
    private String wbsObjectId;

    @JsonProperty("WBSName")
    private String wbsName;

    /**
     * Calculate duration in hours
     * @return Duration in hours
     */
    public Double getDurationHours() {
        // First, check if planned duration is provided
        if (plannedDuration != null) {
            return plannedDuration;
        }

        // If no planned duration, calculate from start and finish dates
        if (startDate != null && finishDate != null) {
            long diffInMillis = finishDate.getTime() - startDate.getTime();
            // Convert milliseconds to hours
            return (double) TimeUnit.MILLISECONDS.toHours(diffInMillis);
        }

        // Return null if no duration can be calculated
        return null;
    }
}