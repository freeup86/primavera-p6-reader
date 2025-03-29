package com.example.primaverap6reader.controller;

import com.example.primaverap6reader.model.Activity;
import com.example.primaverap6reader.model.Project;
import com.example.primaverap6reader.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final PrimaveraRestService primaveraService;
    private final CriticalPathService criticalPathService;
    private final EarnedValueService earnedValueService;
    private final ResourceAnalyticsService resourceAnalyticsService;
    private final ProjectHealthService projectHealthService;

    /**
     * Main analytics dashboard with project health overview
     */
    @GetMapping({"", "/"})
    public String showAnalyticsDashboard(Model model) {
        try {
            log.info("Generating analytics dashboard");

            // Get all projects
            List<Project> projects = primaveraService.getAllProjects();
            model.addAttribute("projects", projects);

            // Calculate project health scores
            Map<String, Double> projectHealthScores = projectHealthService.calculateAllProjectsHealth();
            model.addAttribute("projectHealthScores", projectHealthScores);

            // Get resource utilization summary
            Map<String, Double> resourceAllocation = primaveraService.calculateResourceAllocation();
            model.addAttribute("resourceAllocation", resourceAllocation);

            // Find overallocated resources
            Map<String, Double> overallocatedResources = resourceAnalyticsService.findOverallocatedResources();
            model.addAttribute("overallocatedResources", overallocatedResources);

            return "analytics-dashboard";
        } catch (Exception e) {
            log.error("Error generating analytics dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to generate analytics dashboard: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Project-specific analytics
     */
    @GetMapping("/project/{objectId}")
    public String showProjectAnalytics(@PathVariable String objectId, Model model) {
        try {
            log.info("Generating project analytics for project {}", objectId);

            // Get project
            Project project = primaveraService.getProjectById(objectId);
            model.addAttribute("project", project);

            // Get critical path
            List<Activity> criticalPath = criticalPathService.identifyCriticalPath(objectId);
            model.addAttribute("criticalPath", criticalPath);

            // Get activity float
            Map<String, Double> activityFloat = criticalPathService.calculateActivityFloat(objectId);
            model.addAttribute("activityFloat", activityFloat);

            // Get EVM metrics
            Map<String, Double> evmMetrics = earnedValueService.calculateEVMMetrics(objectId);
            model.addAttribute("evmMetrics", evmMetrics);

            // Get project health metrics
            Map<String, Object> healthMetrics = projectHealthService.calculateProjectHealth(objectId);
            model.addAttribute("healthMetrics", healthMetrics);

            return "project-analytics";
        } catch (Exception e) {
            log.error("Error generating project analytics: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to generate project analytics: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Resource analytics dashboard
     */
    @GetMapping("/resources")
    public String showResourceAnalytics(Model model) {
        try {
            log.info("Generating resource analytics dashboard");

            // Get resource utilization by month
            Map<String, Map<String, Double>> utilization =
                    resourceAnalyticsService.calculateResourceUtilizationByMonth();
            model.addAttribute("resourceUtilizationByMonth", utilization);

            // Get resource costs by project
            Map<String, Map<String, Double>> costs =
                    resourceAnalyticsService.calculateResourceCostsByProject();
            model.addAttribute("resourceCostsByProject", costs);

            // Find overallocated resources
            Map<String, Double> overallocated = resourceAnalyticsService.findOverallocatedResources();
            model.addAttribute("overallocatedResources", overallocated);

            return "resource-analytics";
        } catch (Exception e) {
            log.error("Error generating resource analytics: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to generate resource analytics: " + e.getMessage());
            return "error";
        }
    }
}