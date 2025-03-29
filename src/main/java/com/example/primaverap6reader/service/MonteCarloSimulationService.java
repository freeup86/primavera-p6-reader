package com.example.primaverap6reader.service;

import com.example.primaverap6reader.model.Activity;
import com.example.primaverap6reader.model.Project;
import com.example.primaverap6reader.model.ResourceAssignment;
import com.example.primaverap6reader.model.SimulationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Service for performing Monte Carlo simulations on project schedule and budget data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonteCarloSimulationService {

    private final PrimaveraRestService primaveraService;

    /**
     * Perform schedule risk analysis using Monte Carlo simulation
     *
     * @param projectObjectId The project's object ID
     * @param iterations Number of simulation iterations to run
     * @param confidenceLevels List of confidence levels to calculate (e.g., 80, 90, 95)
     * @return SimulationResult containing the simulation outcomes
     */
    public SimulationResult performScheduleRiskAnalysis(
            String projectObjectId,
            int iterations,
            List<Integer> confidenceLevels) {

        log.info("Starting Monte Carlo simulation for project: {} with {} iterations",
                projectObjectId, iterations);

        try {
            // Get project data
            Project project = findProject(projectObjectId);
            List<Activity> activities = primaveraService.getActivitiesForProject(projectObjectId);

            if (activities.isEmpty()) {
                throw new IllegalStateException("No activities found for project");
            }

            log.info("Retrieved {} activities for project", activities.size());

            // Create simulation model
            Map<String, ActivityDurationModel> durationModels = createDurationModels(activities);

            // Run simulation
            List<Date> simulatedCompletionDates = new ArrayList<>(iterations);
            List<Double> simulatedTotalCosts = new ArrayList<>(iterations);

            for (int i = 0; i < iterations; i++) {
                SimulationIteration result = runSimulationIteration(project, activities, durationModels);
                simulatedCompletionDates.add(result.completionDate);
                simulatedTotalCosts.add(result.totalCost);
            }

            // Sort results for percentile calculations
            Collections.sort(simulatedCompletionDates);
            Collections.sort(simulatedTotalCosts);

            // Calculate confidence levels
            Map<Integer, Date> dateConfidenceResults = new HashMap<>();
            Map<Integer, Double> costConfidenceResults = new HashMap<>();

            for (int confidence : confidenceLevels) {
                int index = (int) Math.ceil(iterations * confidence / 100.0) - 1;
                index = Math.min(index, iterations - 1);

                dateConfidenceResults.put(confidence, simulatedCompletionDates.get(index));
                costConfidenceResults.put(confidence, simulatedTotalCosts.get(index));
            }

            // Calculate mean and standard deviation
            Date meanCompletionDate = calculateMeanDate(simulatedCompletionDates);
            double meanTotalCost = simulatedTotalCosts.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            // Build and return result
            SimulationResult result = new SimulationResult();
            result.setProjectId(projectObjectId);
            result.setProjectName(project.getName());
            result.setIterations(iterations);
            result.setMeanCompletionDate(meanCompletionDate);
            result.setMeanTotalCost(meanTotalCost);
            result.setDateConfidenceResults(dateConfidenceResults);
            result.setCostConfidenceResults(costConfidenceResults);
            result.setSimulatedCompletionDates(simulatedCompletionDates);
            result.setSimulatedTotalCosts(simulatedTotalCosts);

            log.info("Completed Monte Carlo simulation for project: {}", projectObjectId);
            log.info("Mean completion date: {}, Mean cost: ${}", meanCompletionDate, String.format("%.2f", meanTotalCost));

            return result;

        } catch (Exception e) {
            log.error("Error performing Monte Carlo simulation: {}", e.getMessage(), e);
            throw new RuntimeException("Error performing Monte Carlo simulation", e);
        }
    }

    /**
     * Forecast project completion date based on current progress
     *
     * @param projectObjectId The project's object ID
     * @return Map containing forecast dates at different confidence levels
     */
    public Map<String, Object> forecastProjectCompletion(String projectObjectId) {
        try {
            Project project = findProject(projectObjectId);
            List<Activity> activities = primaveraService.getActivitiesForProject(projectObjectId);

            // Calculate Schedule Performance Index (SPI)
            double spi = calculateSPI(activities);

            // Calculate remaining duration with SPI adjustment
            double remainingDuration = calculateRemainingDuration(activities);
            double adjustedRemainingDuration = remainingDuration / spi;

            // Calculate forecast completion date
            Date dataDate = project.getDataDate();
            if (dataDate == null) {
                dataDate = new Date(); // Use current date if no data date available
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dataDate);
            calendar.add(Calendar.DAY_OF_YEAR, (int) Math.ceil(adjustedRemainingDuration));

            Date forecastCompletionDate = calendar.getTime();

            // Calculate optimistic and pessimistic forecasts
            calendar.setTime(dataDate);
            calendar.add(Calendar.DAY_OF_YEAR, (int) Math.ceil(adjustedRemainingDuration * 0.8)); // 20% faster
            Date optimisticForecast = calendar.getTime();

            calendar.setTime(dataDate);
            calendar.add(Calendar.DAY_OF_YEAR, (int) Math.ceil(adjustedRemainingDuration * 1.2)); // 20% slower
            Date pessimisticForecast = calendar.getTime();

            // Prepare result
            Map<String, Object> result = new HashMap<>();
            result.put("projectId", projectObjectId);
            result.put("projectName", project.getName());
            result.put("spi", spi);
            result.put("remainingDuration", remainingDuration);
            result.put("adjustedRemainingDuration", adjustedRemainingDuration);
            result.put("dataDate", dataDate);
            result.put("forecastCompletionDate", forecastCompletionDate);
            result.put("optimisticForecast", optimisticForecast);
            result.put("pessimisticForecast", pessimisticForecast);

            log.info("Completion forecast for project {}: {} (SPI: {})",
                    project.getName(), forecastCompletionDate, String.format("%.2f", spi));

            return result;

        } catch (Exception e) {
            log.error("Error forecasting project completion: {}", e.getMessage(), e);
            throw new RuntimeException("Error forecasting project completion", e);
        }
    }

    /**
     * Forecast project budget with confidence intervals
     *
     * @param projectObjectId The project's object ID
     * @return Map containing budget forecast information
     */
    public Map<String, Object> forecastProjectBudget(String projectObjectId) {
        try {
            Project project = findProject(projectObjectId);
            List<Activity> activities = primaveraService.getActivitiesForProject(projectObjectId);

            // Get resource assignments to calculate costs
            List<ResourceAssignment> assignments = new ArrayList<>();
            for (Activity activity : activities) {
                assignments.addAll(primaveraService.getResourceAssignmentsForActivity(activity.getObjectId()));
            }

            // Calculate Cost Performance Index (CPI)
            double cpi = calculateCPI(assignments);

            // Calculate budget metrics
            double plannedCost = assignments.stream()
                    .mapToDouble(a -> a.getPlannedCost() != null ? a.getPlannedCost() : 0.0)
                    .sum();

            double actualCost = assignments.stream()
                    .mapToDouble(a -> a.getActualCost() != null ? a.getActualCost() : 0.0)
                    .sum();

            double remainingCost = assignments.stream()
                    .mapToDouble(a -> a.getRemainingCost() != null ? a.getRemainingCost() : 0.0)
                    .sum();

            // Calculate Estimate at Completion (EAC)
            double eac = actualCost + (remainingCost / cpi);

            // Calculate confidence intervals
            double eac80 = actualCost + (remainingCost / (cpi * 0.9)); // 80% confidence
            double eac90 = actualCost + (remainingCost / (cpi * 0.85)); // 90% confidence
            double eac95 = actualCost + (remainingCost / (cpi * 0.8)); // 95% confidence

            // Prepare result
            Map<String, Object> result = new HashMap<>();
            result.put("projectId", projectObjectId);
            result.put("projectName", project.getName());
            result.put("cpi", cpi);
            result.put("plannedCost", plannedCost);
            result.put("actualCost", actualCost);
            result.put("remainingCost", remainingCost);
            result.put("eac", eac);
            result.put("eac80", eac80);
            result.put("eac90", eac90);
            result.put("eac95", eac95);
            result.put("variance", plannedCost - eac);

            log.info("Budget forecast for project {}: ${} (CPI: {})",
                    project.getName(), String.format("%.2f", eac), String.format("%.2f", cpi));

            return result;

        } catch (Exception e) {
            log.error("Error forecasting project budget: {}", e.getMessage(), e);
            throw new RuntimeException("Error forecasting project budget", e);
        }
    }

    /**
     * Helper method to find a project by object ID
     */
    private Project findProject(String projectObjectId) {
        List<Project> projects = primaveraService.getAllProjects();
        return projects.stream()
                .filter(p -> projectObjectId.equals(p.getObjectId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectObjectId));
    }

    /**
     * Create duration distribution models for activities
     */
    private Map<String, ActivityDurationModel> createDurationModels(List<Activity> activities) {
        Map<String, ActivityDurationModel> models = new HashMap<>();

        for (Activity activity : activities) {
            Double durationHours = activity.getDurationHours();

            if (durationHours != null && durationHours > 0) {
                ActivityDurationModel model = new ActivityDurationModel();

                // Define triangular distribution parameters
                model.mostLikely = durationHours;
                model.optimistic = durationHours * 0.8; // 20% shorter
                model.pessimistic = durationHours * 1.3; // 30% longer

                models.put(activity.getObjectId(), model);
            }
        }

        return models;
    }

    /**
     * Run a single simulation iteration
     */
    private SimulationIteration runSimulationIteration(
            Project project,
            List<Activity> activities,
            Map<String, ActivityDurationModel> durationModels) {

        // Clone activities for this simulation iteration
        List<SimulatedActivity> simulatedActivities = activities.stream()
                .map(this::createSimulatedActivity)
                .collect(Collectors.toList());

        // Apply duration variations
        for (SimulatedActivity activity : simulatedActivities) {
            ActivityDurationModel model = durationModels.get(activity.objectId);

            if (model != null) {
                // Sample from triangular distribution
                double randomDuration = sampleTriangularDistribution(
                        model.optimistic, model.mostLikely, model.pessimistic);
                activity.duration = randomDuration;
            }
        }

        // Calculate simulated completion date
        Date startDate = project.getStartDate();
        double totalDuration = simulatedActivities.stream()
                .mapToDouble(a -> a.duration)
                .sum();

        // Convert duration from hours to days and add to start date
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.HOUR, (int) Math.ceil(totalDuration));

        Date completionDate = calendar.getTime();

        // Calculate simulated cost
        double totalCost = calculateSimulatedCost(simulatedActivities, activities);

        return new SimulationIteration(completionDate, totalCost);
    }

    /**
     * Sample from a triangular distribution
     */
    private double sampleTriangularDistribution(double min, double mode, double max) {
        double random = ThreadLocalRandom.current().nextDouble();
        double f = (mode - min) / (max - min);

        if (random < f) {
            return min + Math.sqrt(random * (max - min) * (mode - min));
        } else {
            return max - Math.sqrt((1 - random) * (max - min) * (max - mode));
        }
    }

    /**
     * Calculate mean date from a list of dates
     */
    private Date calculateMeanDate(List<Date> dates) {
        long sum = 0;
        for (Date date : dates) {
            sum += date.getTime();
        }

        long mean = sum / dates.size();
        return new Date(mean);
    }

    /**
     * Create a simulated activity from a regular activity
     */
    private SimulatedActivity createSimulatedActivity(Activity activity) {
        SimulatedActivity simulated = new SimulatedActivity();
        simulated.objectId = activity.getObjectId();
        simulated.name = activity.getName();
        simulated.duration = activity.getDurationHours() != null ? activity.getDurationHours() : 0;
        return simulated;
    }

    /**
     * Calculate simulated cost based on duration variations
     */
    private double calculateSimulatedCost(List<SimulatedActivity> simulatedActivities, List<Activity> originalActivities) {
        double totalCost = 0;

        // Create mapping from activity ID to original and simulated activities
        Map<String, Activity> originalMap = originalActivities.stream()
                .collect(Collectors.toMap(Activity::getObjectId, a -> a));

        Map<String, SimulatedActivity> simulatedMap = simulatedActivities.stream()
                .collect(Collectors.toMap(a -> a.objectId, a -> a));

        // Calculate cost impact based on duration changes
        for (String activityId : originalMap.keySet()) {
            Activity original = originalMap.get(activityId);
            SimulatedActivity simulated = simulatedMap.get(activityId);

            if (original != null && simulated != null && original.getDurationHours() != null && original.getDurationHours() > 0) {
                double durationRatio = simulated.duration / original.getDurationHours();

                // Get resource assignments for this activity
                try {
                    List<ResourceAssignment> assignments = primaveraService.getResourceAssignmentsForActivity(activityId);

                    // Calculate cost based on duration ratio
                    for (ResourceAssignment assignment : assignments) {
                        if (assignment.getPlannedCost() != null) {
                            double simulatedCost = assignment.getPlannedCost() * durationRatio;
                            totalCost += simulatedCost;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error getting resource assignments for activity {}: {}", activityId, e.getMessage());
                }
            }
        }

        return totalCost;
    }

    /**
     * Calculate Schedule Performance Index (SPI)
     */
    private double calculateSPI(List<Activity> activities) {
        double plannedValue = 0;
        double earnedValue = 0;

        for (Activity activity : activities) {
            if (activity.getDurationHours() != null && activity.getDurationHours() > 0) {
                double plannedDuration = activity.getDurationHours();
                plannedValue += plannedDuration;

                // Estimate earned value based on activity status
                if ("Completed".equals(activity.getStatus())) {
                    earnedValue += plannedDuration;
                } else if ("In Progress".equals(activity.getStatus())) {
                    // Assume 50% complete for activities in progress
                    earnedValue += plannedDuration * 0.5;
                }
            }
        }

        // Guard against division by zero
        if (plannedValue > 0) {
            return earnedValue / plannedValue;
        } else {
            return 1.0; // Default to neutral if no planned value
        }
    }

    /**
     * Calculate Cost Performance Index (CPI)
     */
    private double calculateCPI(List<ResourceAssignment> assignments) {
        double plannedCost = 0;
        double actualCost = 0;
        double earnedValue = 0;

        for (ResourceAssignment assignment : assignments) {
            if (assignment.getPlannedCost() != null) {
                plannedCost += assignment.getPlannedCost();
            }

            if (assignment.getActualCost() != null) {
                actualCost += assignment.getActualCost();
            }

            // Estimate earned value based on reported progress
            if (assignment.getPlannedCost() != null && assignment.getActualUnits() != null && assignment.getPlannedUnits() != null) {
                if (assignment.getPlannedUnits() > 0) {
                    double progressRatio = Math.min(assignment.getActualUnits() / assignment.getPlannedUnits(), 1.0);
                    earnedValue += assignment.getPlannedCost() * progressRatio;
                }
            }
        }

        // Guard against division by zero
        if (actualCost > 0) {
            return earnedValue / actualCost;
        } else {
            return 1.0; // Default to neutral if no actual cost
        }
    }

    /**
     * Calculate remaining duration of project
     */
    private double calculateRemainingDuration(List<Activity> activities) {
        return activities.stream()
                .filter(a -> !"Completed".equals(a.getStatus()))
                .mapToDouble(a -> {
                    if (a.getDurationHours() != null) {
                        if ("In Progress".equals(a.getStatus())) {
                            return a.getDurationHours() * 0.5; // Assume 50% remaining for in-progress
                        } else {
                            return a.getDurationHours(); // Full duration for not started
                        }
                    }
                    return 0;
                })
                .sum();
    }

    /**
     * Inner class for triangular distribution parameters
     */
    private static class ActivityDurationModel {
        double optimistic;
        double mostLikely;
        double pessimistic;
    }

    /**
     * Inner class for simulated activities
     */
    private static class SimulatedActivity {
        String objectId;
        String name;
        double duration;
    }

    /**
     * Inner class for simulation iteration results
     */
    private static class SimulationIteration {
        Date completionDate;
        double totalCost;

        SimulationIteration(Date completionDate, double totalCost) {
            this.completionDate = completionDate;
            this.totalCost = totalCost;
        }
    }
}