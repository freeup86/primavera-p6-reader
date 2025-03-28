package com.example.primaverap6reader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

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

    // This field might not be available from the API
    // But we'll keep it for application logic
    private Double durationHours;

    @JsonProperty("Type")
    private String type;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("WBSObjectId")
    private String wbsObjectId;

    @JsonProperty("WBSName")
    private String wbsName;

    // Calculate duration if it's not provided directly
    public Double getDurationHours() {
        if (durationHours != null) {
            return durationHours;
        }

        // Calculate duration from start and finish dates if both are available
        if (startDate != null && finishDate != null) {
            long diffInMillies = finishDate.getTime() - startDate.getTime();
            return (double) diffInMillies / (1000 * 60 * 60); // Convert to hours
        }

        return null;
    }
}