package com.example.primaverap6reader.model;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Model for holding Monte Carlo simulation results
 */
@Data
public class SimulationResult {

    // Basic project info
    private String projectId;
    private String projectName;
    private int iterations;

    // Schedule simulation results
    private Date meanCompletionDate;
    private Map<Integer, Date> dateConfidenceResults; // Maps confidence levels to dates
    private List<Date> simulatedCompletionDates; // All simulated completion dates

    // Cost simulation results
    private double meanTotalCost;
    private Map<Integer, Double> costConfidenceResults; // Maps confidence levels to costs
    private List<Double> simulatedTotalCosts; // All simulated costs

    // Distribution properties
    private double dateStandardDeviation;
    private double costStandardDeviation;
}