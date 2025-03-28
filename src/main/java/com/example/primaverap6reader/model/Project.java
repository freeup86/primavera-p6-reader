package com.example.primaverap6reader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("ObjectId")
    private String objectId;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("StartDate")
    private Date startDate;

    @JsonProperty("FinishDate")
    private Date finishDate;

    @JsonProperty("DataDate")
    private Date dataDate;

    @JsonProperty("Description")
    private String description;
}