package com.example.primaverap6reader.service;

import com.example.primaverap6reader.model.Activity;
import com.example.primaverap6reader.model.Project;
import com.example.primaverap6reader.model.ResourceAssignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EarnedValueService {

    private final PrimaveraRestService primaveraService;

    /**
     * Calculate basic EVM metrics for a project
     * @param projectObjectId Project Object ID
     * @return Map containing EVM metrics
     */
    public Map<String, Double> calculateEVMMetrics(String projectObjectId) {
        Project project = primaveraService.getProjectById(projectObjectId);
        List<Activity> activities = primaveraService.getActivitiesForProject(projectObjectId);
        List<ResourceAssignment> assignments = primaveraService.getResourceAssignmentsForProject(projectObjectId);

        Map<String, Double> evmMetrics = new HashMap<>();

        // Calculate Budget at Completion (BAC)
        double bac = assignments.stream()
                .mapToDouble(a -> a.getPlannedCost() != null ? a.getPlannedCost() : 0)
                .sum();

        // Calculate Planned Value (PV)
        // For simplicity, using project timeline percentage
        double pv = calculatePlannedValue(project, activities, bac);

        // Calculate Actual Cost (AC)
        double ac = assignments.stream()
                .mapToDouble(a -> a.getActualCost() != null ? a.getActualCost() : 0)
                .sum();

        // Calculate Earned Value (EV)
        double ev = calculateEarnedValue(activities, bac);

        // Store basic metrics
        evmMetrics.put("BAC", bac);
        evmMetrics.put("PV", pv);
        evmMetrics.put("AC", ac);
        evmMetrics.put("EV", ev);

        // Calculate derived metrics
        if (pv > 0) {
            // Schedule Performance Index (SPI)
            evmMetrics.put("SPI", ev / pv);

            // Schedule Variance (SV)
            evmMetrics.put("SV", ev - pv);
        }

        if (ac > 0) {
            // Cost Performance Index (CPI)
            evmMetrics.put("CPI", ev / ac);

            // Cost Variance (CV)
            evmMetrics.put("CV", ev - ac);
        }

        if (ac > 0 && pv > 0) {
            // Estimate at Completion (EAC)
            evmMetrics.put("EAC", bac / (ev / ac));

            // Estimate to Complete (ETC)
            evmMetrics.put("ETC", (bac - ev) / (ev / ac));

            // Variance at Completion (VAC)
            evmMetrics.put("VAC", bac - (bac / (ev / ac)));
        }

        return evmMetrics;
    }

    private double calculatePlannedValue(Project project, List<Activity> activities, double bac) {
        if (project.getStartDate() == null || project.getFinishDate() == null) {
            return 0;
        }

        Date currentDate = new Date();
        long totalDuration = project.getFinishDate().getTime() - project.getStartDate().getTime();
        long elapsedDuration = Math.min(
                currentDate.getTime() - project.getStartDate().getTime(),
                totalDuration
        );

        if (totalDuration <= 0) {
            return 0;
        }

        double percentageComplete = (double) elapsedDuration / totalDuration;
        return bac * percentageComplete;
    }

    private double calculateEarnedValue(List<Activity> activities, double bac) {
        // Count completed activities
        long completedActivities = activities.stream()
                .filter(a -> "Completed".equals(a.getStatus()))
                .count();

        if (activities.isEmpty()) {
            return 0;
        }

        // Calculate EV based on percentage of completed activities
        double percentageComplete = (double) completedActivities / activities.size();
        return bac * percentageComplete;
    }
}