package com.example.primaverap6reader.service;

import com.example.primaverap6reader.model.Activity;
import com.example.primaverap6reader.model.Project;
import com.example.primaverap6reader.model.ResourceAssignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectHealthService {

    private final PrimaveraRestService primaveraService;
    private final EarnedValueService earnedValueService;

    /**
     * Calculate health score for a project (0-100)
     * @param projectObjectId Project Object ID
     * @return Health score and component metrics
     */
    public Map<String, Object> calculateProjectHealth(String projectObjectId) {
        Project project = primaveraService.getProjectById(projectObjectId);
        List<Activity> activities = primaveraService.getActivitiesForProject(projectObjectId);

        Map<String, Object> healthMetrics = new HashMap<>();

        // Get EVM metrics
        Map<String, Double> evmMetrics = earnedValueService.calculateEVMMetrics(projectObjectId);

        // Calculate schedule health (0-25)
        double scheduleHealth = calculateScheduleHealth(project, activities, evmMetrics);

        // Calculate cost health (0-25)
        double costHealth = calculateCostHealth(evmMetrics);

        // Calculate resource health (0-25)
        double resourceHealth = calculateResourceHealth(projectObjectId);

        // Calculate risk health (0-25)
        double riskHealth = calculateRiskHealth(activities);

        // Calculate overall health score (0-100)
        double overallHealth = scheduleHealth + costHealth + resourceHealth + riskHealth;

        // Store metrics
        healthMetrics.put("overallHealth", overallHealth);
        healthMetrics.put("scheduleHealth", scheduleHealth);
        healthMetrics.put("costHealth", costHealth);
        healthMetrics.put("resourceHealth", resourceHealth);
        healthMetrics.put("riskHealth", riskHealth);
        healthMetrics.put("evmMetrics", evmMetrics);

        // Add interpretation
        String interpretation = interpretHealthScore(overallHealth);
        healthMetrics.put("interpretation", interpretation);

        return healthMetrics;
    }

    private double calculateScheduleHealth(Project project, List<Activity> activities,
                                           Map<String, Double> evmMetrics) {
        // Component 1: SPI (10 points)
        double spiScore = 0;
        if (evmMetrics.containsKey("SPI")) {
            double spi = evmMetrics.get("SPI");
            spiScore = Math.min(10, spi * 10);
        }

        // Component 2: Activity completion ratio (10 points)
        double completionRatio = 0;
        if (!activities.isEmpty()) {
            long completedActivities = activities.stream()
                    .filter(a -> "Completed".equals(a.getStatus()))
                    .count();
            completionRatio = (double) completedActivities / activities.size();
        }
        double completionScore = completionRatio * 10;

        // Component 3: On-time activities (5 points)
        double onTimeScore = 0;
        Date currentDate = new Date();
        if (!activities.isEmpty()) {
            long onTimeActivities = activities.stream()
                    .filter(a -> a.getFinishDate() == null ||
                            a.getFinishDate().after(currentDate) ||
                            "Completed".equals(a.getStatus()))
                    .count();
            onTimeScore = (double) onTimeActivities / activities.size() * 5;
        }

        return spiScore + completionScore + onTimeScore;
    }

    private double calculateCostHealth(Map<String, Double> evmMetrics) {
        // Component 1: CPI (15 points)
        double cpiScore = 0;
        if (evmMetrics.containsKey("CPI")) {
            double cpi = evmMetrics.get("CPI");
            cpiScore = Math.min(15, cpi * 15);
        }

        // Component 2: VAC as percentage of BAC (10 points)
        double vacScore = 0;
        if (evmMetrics.containsKey("VAC") && evmMetrics.containsKey("BAC") &&
                evmMetrics.get("BAC") > 0) {
            double vacRatio = evmMetrics.get("VAC") / evmMetrics.get("BAC");
            // Convert to a 0-10 score where 0 or positive VAC is 10 points
            vacScore = Math.max(0, Math.min(10, 10 * (1 + vacRatio)));
        }

        return cpiScore + vacScore;
    }

    private double calculateResourceHealth(String projectObjectId) {
        List<ResourceAssignment> assignments =
                primaveraService.getResourceAssignmentsForProject(projectObjectId);

        if (assignments.isEmpty()) {
            return 25; // Full points if no resources (simple project)
        }

        // Component 1: Resources with actual data (15 points)
        long assignmentsWithActuals = assignments.stream()
                .filter(a -> a.getActualUnits() != null && a.getActualUnits() > 0)
                .count();
        double actualsScore = (double) assignmentsWithActuals / assignments.size() * 15;

        // Component 2: Overallocation check (10 points)
        // Lower score if many resources are over-allocated
        // This is a placeholder - in a real implementation, you would check each resource's allocation
        double overallocationScore = 10;

        return actualsScore + overallocationScore;
    }

    private double calculateRiskHealth(List<Activity> activities) {
        if (activities.isEmpty()) {
            return 0;
        }

        // Component 1: Behind schedule activities (15 points)
        Date currentDate = new Date();
        long behindSchedule = activities.stream()
                .filter(a -> a.getFinishDate() != null &&
                        a.getFinishDate().before(currentDate) &&
                        !"Completed".equals(a.getStatus()))
                .count();

        double behindScheduleRatio = (double) behindSchedule / activities.size();
        double behindScheduleScore = (1 - behindScheduleRatio) * 15;

        // Component 2: Activities without dates (10 points)
        long withoutDates = activities.stream()
                .filter(a -> a.getStartDate() == null || a.getFinishDate() == null)
                .count();

        double withoutDatesRatio = (double) withoutDates / activities.size();
        double withoutDatesScore = (1 - withoutDatesRatio) * 10;

        return behindScheduleScore + withoutDatesScore;
    }

    private String interpretHealthScore(double score) {
        if (score >= 90) {
            return "Excellent - Project is on track with minimal issues";
        } else if (score >= 75) {
            return "Good - Project is generally on track with some minor issues";
        } else if (score >= 60) {
            return "Moderate - Project has significant issues that need attention";
        } else if (score >= 40) {
            return "Poor - Project has critical issues requiring immediate intervention";
        } else {
            return "Critical - Project is severely off track and needs major corrective action";
        }
    }

    /**
     * Calculate health scores for all projects
     * @return Map of project name to health score
     */
    public Map<String, Double> calculateAllProjectsHealth() {
        List<Project> projects = primaveraService.getAllProjects();
        Map<String, Double> projectHealthScores = new HashMap<>();

        for (Project project : projects) {
            try {
                Map<String, Object> healthMetrics =
                        calculateProjectHealth(project.getObjectId());
                projectHealthScores.put(
                        project.getName(),
                        (Double) healthMetrics.get("overallHealth")
                );
            } catch (Exception e) {
                log.error("Error calculating health for project {}: {}",
                        project.getName(), e.getMessage());
            }
        }

        return projectHealthScores;
    }
}