package com.example.primaverap6reader.controller;

import com.example.primaverap6reader.model.Project;
import com.example.primaverap6reader.service.PrimaveraRestService;
import com.example.primaverap6reader.service.ProjectStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final PrimaveraRestService primaveraService;
    private final ProjectStatisticsService statisticsService;

    @GetMapping({"", "/"})
    public String showEnhancedDashboard(Model model) {
        try {
            log.info("Generating enhanced dashboard");

            // Fetch all projects
            List<Project> projects = primaveraService.getAllProjects();

            // Project Status Distribution
            Map<String, Integer> projectStatusDistribution = calculateProjectStatusDistribution(projects);
            model.addAttribute("projectStatusDistribution", projectStatusDistribution);

            // Project Duration Distribution
            Map<String, Integer> projectDurationDistribution = calculateProjectDurationDistribution(projects);
            model.addAttribute("projectDurationDistribution", projectDurationDistribution);

            // Activity Type Distribution
            Map<String, Integer> activityTypeDistribution = statisticsService.getActivityCountByType();
            model.addAttribute("activityTypeDistribution", activityTypeDistribution);

            // Project Progress Overview
            List<Map<String, Object>> projectProgressData = calculateProjectProgressData(projects);
            model.addAttribute("projectProgressData", projectProgressData);

            // Resource Allocation
            Map<String, Double> resourceAllocation = calculateResourceAllocation();
            model.addAttribute("resourceAllocation", resourceAllocation);

            return "enhanced-dashboard";
        } catch (Exception e) {
            log.error("Error generating enhanced dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to generate dashboard: " + e.getMessage());
            return "error";
        }
    }

    private Map<String, Integer> calculateProjectStatusDistribution(List<Project> projects) {
        return projects.stream()
                .collect(Collectors.groupingBy(
                        project -> project.getStatus() != null ? project.getStatus() : "Unknown",
                        Collectors.summingInt(p -> 1)
                ));
    }

    private Map<String, Integer> calculateProjectDurationDistribution(List<Project> projects) {
        return projects.stream()
                .filter(p -> p.getStartDate() != null && p.getFinishDate() != null)
                .map(p -> {
                    long durationDays = (p.getFinishDate().getTime() - p.getStartDate().getTime()) / (24 * 60 * 60 * 1000);
                    if (durationDays <= 30) return "0-1 Month";
                    if (durationDays <= 90) return "1-3 Months";
                    if (durationDays <= 180) return "3-6 Months";
                    if (durationDays <= 365) return "6-12 Months";
                    return "12+ Months";
                })
                .collect(Collectors.groupingBy(
                        duration -> duration,
                        Collectors.summingInt(p -> 1)
                ));
    }

    private List<Map<String, Object>> calculateProjectProgressData(List<Project> projects) {
        return projects.stream()
                .map(project -> {
                    Map<String, Object> projectProgress = new HashMap<>();
                    projectProgress.put("name", project.getName());

                    // Placeholder logic for progress calculation
                    // In a real scenario, you'd calculate this based on activity statuses
                    projectProgress.put("notStarted", 30);
                    projectProgress.put("inProgress", 50);
                    projectProgress.put("completed", 20);

                    return projectProgress;
                })
                .limit(5) // Limit to top 5 projects
                .collect(Collectors.toList());
    }

    private Map<String, Double> calculateResourceAllocation() {
        try {
            // Use PrimaveraRestService to get actual resource allocation
            Map<String, Double> resourceAllocation = primaveraService.calculateResourceAllocation();

            log.info("Dashboard Resource Allocation Before Sorting: {}", resourceAllocation);

            // Sort resources by allocation percentage in descending order
            Map<String, Double> sortedResourceAllocation = resourceAllocation.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(10) // Limit to top 10 resources
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));

            log.info("Dashboard Resource Allocation After Sorting: {}", sortedResourceAllocation);

            return sortedResourceAllocation;
        } catch (Exception e) {
            log.error("Error in resource allocation calculation: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

}