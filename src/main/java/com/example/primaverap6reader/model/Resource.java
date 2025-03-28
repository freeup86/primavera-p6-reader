package com.example.primaverap6reader.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Resource {

    @JsonProperty("ObjectId")
    private String objectId;

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("ResourceType")
    private String resourceType;

    @JsonProperty("EmailAddress")
    private String emailAddress;

    @JsonProperty("PhoneNumber")
    private String phoneNumber;

    @JsonProperty("PricePerUnit")
    private Double pricePerUnit;

    @JsonProperty("MaxUnitsPerTime")
    private Double maxUnitsPerTime;

    @JsonProperty("CalculateCostFromUnits")
    private Boolean calculateCostFromUnits;

    @JsonProperty("Status")
    private String status;
}