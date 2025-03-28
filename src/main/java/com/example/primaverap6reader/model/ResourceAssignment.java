package com.example.primaverap6reader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceAssignment {

    @JsonProperty("ObjectId")
    private String objectId;

    @JsonProperty("ActivityId")
    private String activityId;

    @JsonProperty("ActivityObjectId")
    private String activityObjectId;

    @JsonProperty("ResourceId")
    private String resourceId;

    @JsonProperty("ResourceObjectId")
    private String resourceObjectId;

    @JsonProperty("ResourceName")
    private String resourceName;

    @JsonProperty("PlannedUnits")
    private Double plannedUnits;

    @JsonProperty("ActualUnits")
    private Double actualUnits;

    @JsonProperty("RemainingUnits")
    private Double remainingUnits;

    @JsonProperty("PlannedCost")
    private Double plannedCost;

    @JsonProperty("ActualCost")
    private Double actualCost;

    @JsonProperty("RemainingCost")
    private Double remainingCost;

    @JsonProperty("PlannedStartDate")
    private Date plannedStartDate;

    @JsonProperty("PlannedFinishDate")
    private Date plannedFinishDate;

    @JsonProperty("ActualStartDate")
    private Date actualStartDate;

    @JsonProperty("ActualFinishDate")
    private Date actualFinishDate;
}