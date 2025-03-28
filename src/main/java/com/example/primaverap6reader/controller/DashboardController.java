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

import java.util.List;
import java.util.Map;

/**
 * Controller for dashboard views with project statistics
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final PrimaveraRestService primaveraService;
    private final ProjectStatisticsService statisticsService;

    /**
     * Dashboard page with project statistics and charts
     */
    @GetMapping({"", "/"})
    public String showDashboard(Model model) {
        try {
            log.info("Generating dashboard");

            // Get all projects
            List<Project> projects = primaveraService.getAllProjects();

            // Get statistics
            Map<String, Integer> projectCountByStatus = statisticsService.getProjectCountByStatus();
            Map<String, Integer> projectsTimelineByMonth = statisticsService.getProjectsTimelineByMonth();
            List<Project> overdueProjects = statisticsService.getOverdueProjects();
            List<Project> upcomingProjects = statisticsService.getUpcomingProjects();
            Map<String, Integer> topProjects = statisticsService.getProjectsWithMostActivities(5);
            Map<String, Double> projectDurations = statisticsService.getTotalDurationByProject();
            Map<String, Object> summaryStats = statisticsService.getProjectsSummaryStatistics();

            // Add data to the model
            model.addAttribute("projects", projects);
            model.addAttribute("projectCountByStatus", projectCountByStatus);
            model.addAttribute("projectsTimelineByMonth", projectsTimelineByMonth);
            model.addAttribute("overdueProjects", overdueProjects);
            model.addAttribute("upcomingProjects", upcomingProjects);
            model.addAttribute("topProjects", topProjects);
            model.addAttribute("projectDurations", projectDurations);
            model.addAttribute("summaryStats", summaryStats);

            return "dashboard";
        } catch (Exception e) {
            log.error("Error generating dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to generate dashboard: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Activity statistics dashboard
     */
    @GetMapping("/activities")
    public String showActivityDashboard(Model model) {
        try {
            log.info("Generating activity dashboard");

            // Get activity statistics
            Map<String, Integer> activityCountByType = statisticsService.getActivityCountByType();
            Map<String, Integer> activityCountByStatus = statisticsService.getActivityCountByStatus();

            // Add data to the model
            model.addAttribute("activityCountByType", activityCountByType);
            model.addAttribute("activityCountByStatus", activityCountByStatus);

            return "activity-dashboard";
        } catch (Exception e) {
            log.error("Error generating activity dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to generate activity dashboard: " + e.getMessage());
            return "error";
        }
    }
}