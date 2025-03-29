package com.example.primaverap6reader.service;

import com.example.primaverap6reader.model.Activity;
import com.example.primaverap6reader.model.Project;
import com.example.primaverap6reader.model.Resource;
import com.example.primaverap6reader.model.ResourceAssignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceAnalyticsService {

    private final PrimaveraRestService primaveraService;

    /**
     * Calculate resource utilization by month
     * @return Map of resource name to monthly utilization
     */
    @Cacheable("resourceUtilizationByMonth")
    public Map<String, Map<String, Double>> calculateResourceUtilizationByMonth() {
        List<Resource> resources = primaveraService.getAllResources();
        List<Project> projects = primaveraService.getAllProjects();

        Map<String, Map<String, Double>> resourceUtilization = new HashMap<>();

        // Initialize result map for each resource
        resources.forEach(resource -> {
            resourceUtilization.put(resource.getName(), new HashMap<>());
        });

        // For each project, calculate resource allocation by month
        for (Project project : projects) {
            try {
                List<ResourceAssignment> assignments =
                        primaveraService.getResourceAssignmentsForProject(project.getObjectId());

                processAssignments(assignments, resourceUtilization);
            } catch (Exception e) {
                log.error("Error processing assignments for project {}: {}",
                        project.getName(), e.getMessage());
            }
        }

        return resourceUtilization;
    }

    private void processAssignments(List<ResourceAssignment> assignments,
                                    Map<String, Map<String, Double>> resourceUtilization) {
        for (ResourceAssignment assignment : assignments) {
            if (assignment.getResourceName() == null ||
                    assignment.getPlannedStartDate() == null ||
                    assignment.getPlannedFinishDate() == null ||
                    assignment.getPlannedUnits() == null) {
                continue;
            }

            // Get or create resource map
            Map<String, Double> monthlyUtilization =
                    resourceUtilization.getOrDefault(assignment.getResourceName(), new HashMap<>());

            // Calculate months covered by assignment
            List<String> months = getMonthsBetweenDates(
                    assignment.getPlannedStartDate(),
                    assignment.getPlannedFinishDate()
            );

            // Distribute units across months
            double unitsPerMonth = assignment.getPlannedUnits() / Math.max(1, months.size());

            for (String month : months) {
                double currentUtilization = monthlyUtilization.getOrDefault(month, 0.0);
                monthlyUtilization.put(month, currentUtilization + unitsPerMonth);
            }

            resourceUtilization.put(assignment.getResourceName(), monthlyUtilization);
        }
    }

    private List<String> getMonthsBetweenDates(Date startDate, Date endDate) {
        List<String> months = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(endDate);

        while (calendar.before(endCalendar) || calendar.equals(endCalendar)) {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            months.add(String.format("%d-%02d", year, month));

            calendar.add(Calendar.MONTH, 1);
        }

        return months;
    }

    /**
     * Find overallocated resources (>100% allocation)
     * @return List of overallocated resources with their allocation percentage
     */
    @Cacheable("overallocatedResources")
    public Map<String, Double> findOverallocatedResources() {
        Map<String, Double> resourceAllocation = primaveraService.calculateResourceAllocation();

        // Filter for resources with >100% allocation
        return resourceAllocation.entrySet().stream()
                .filter(entry -> entry.getValue() > 100)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Calculate resource costs by project
     * @return Map of project name to resource costs
     */
    @Cacheable("resourceCostsByProject")
    public Map<String, Map<String, Double>> calculateResourceCostsByProject() {
        List<Project> projects = primaveraService.getAllProjects();
        Map<String, Map<String, Double>> result = new HashMap<>();

        for (Project project : projects) {
            Map<String, Double> resourceCosts = new HashMap<>();

            try {
                List<ResourceAssignment> assignments =
                        primaveraService.getResourceAssignmentsForProject(project.getObjectId());

                // Group by resource and sum costs
                for (ResourceAssignment assignment : assignments) {
                    if (assignment.getResourceName() != null && assignment.getPlannedCost() != null) {
                        String resourceName = assignment.getResourceName();
                        double currentCost = resourceCosts.getOrDefault(resourceName, 0.0);
                        resourceCosts.put(resourceName, currentCost + assignment.getPlannedCost());
                    }
                }

                result.put(project.getName(), resourceCosts);
            } catch (Exception e) {
                log.error("Error calculating resource costs for project {}: {}",
                        project.getName(), e.getMessage());
            }
        }

        return result;
    }
}