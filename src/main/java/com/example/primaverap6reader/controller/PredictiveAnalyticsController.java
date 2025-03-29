package com.example.primaverap6reader.controller;

import com.example.primaverap6reader.model.Project;
import com.example.primaverap6reader.model.SimulationResult;
import com.example.primaverap6reader.service.MonteCarloSimulationService;
import com.example.primaverap6reader.service.PrimaveraRestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Controller for predictive analytics features
 */
@Controller
@RequestMapping("/predictive") // Changed from "/analytics" to "/predictive"
@RequiredArgsConstructor
@Slf4j
public class PredictiveAnalyticsController {

    private final PrimaveraRestService primaveraService;
    private final MonteCarloSimulationService simulationService;

    /**
     * Display main predictive analytics dashboard
     */
    @GetMapping({"", "/"})
    public String showAnalyticsDashboard(Model model) {
        try {
            log.info("Loading predictive analytics dashboard");

            // Get all projects for dropdown
            List<Project> projects = primaveraService.getAllProjects();
            model.addAttribute("projects", projects);

            return "predictive-analytics-dashboard";
        } catch (Exception e) {
            log.error("Error loading predictive analytics dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load predictive analytics dashboard: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Display Schedule Risk Analysis page
     */
    @GetMapping("/schedule-risk/{projectId}")
    public String showScheduleRiskAnalysis(
            @PathVariable String projectId,
            @RequestParam(required = false, defaultValue = "1000") int iterations,
            Model model) {

        try {
            log.info("Performing schedule risk analysis for project: {}", projectId);

            // Get project details
            Project project = findProject(projectId);
            model.addAttribute("project", project);

            // Default confidence levels to calculate
            List<Integer> confidenceLevels = Arrays.asList(50, 80, 90, 95);

            // Run Monte Carlo simulation
            SimulationResult simulationResult =
                    simulationService.performScheduleRiskAnalysis(projectId, iterations, confidenceLevels);

            model.addAttribute("simulationResult", simulationResult);
            model.addAttribute("iterations", iterations);
            model.addAttribute("confidenceLevels", confidenceLevels);

            return "schedule-risk-analysis";
        } catch (Exception e) {
            log.error("Error performing schedule risk analysis: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to perform schedule risk analysis: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Display Completion Forecast page
     */
    @GetMapping("/completion-forecast/{projectId}")
    public String showCompletionForecast(@PathVariable String projectId, Model model) {
        try {
            log.info("Calculating completion forecast for project: {}", projectId);

            // Get project details
            Project project = findProject(projectId);
            model.addAttribute("project", project);

            // Calculate forecast
            Map<String, Object> forecastResult = simulationService.forecastProjectCompletion(projectId);
            model.addAttribute("forecastResult", forecastResult);

            return "completion-forecast";
        } catch (Exception e) {
            log.error("Error calculating completion forecast: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to calculate completion forecast: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Display Budget Forecast page
     */
    @GetMapping("/budget-forecast/{projectId}")
    public String showBudgetForecast(@PathVariable String projectId, Model model) {
        try {
            log.info("Calculating budget forecast for project: {}", projectId);

            // Get project details
            Project project = findProject(projectId);
            model.addAttribute("project", project);

            // Calculate forecast
            Map<String, Object> forecastResult = simulationService.forecastProjectBudget(projectId);
            model.addAttribute("forecastResult", forecastResult);

            return "budget-forecast";
        } catch (Exception e) {
            log.error("Error calculating budget forecast: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to calculate budget forecast: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Run a Monte Carlo simulation and return the results via AJAX for chart updates
     */
    @PostMapping("/simulate/{projectId}")
    @ResponseBody
    public SimulationResult runSimulation(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "1000") int iterations) {

        try {
            log.info("Running Monte Carlo simulation for project: {} with {} iterations", projectId, iterations);

            List<Integer> confidenceLevels = Arrays.asList(50, 80, 90, 95);
            return simulationService.performScheduleRiskAnalysis(projectId, iterations, confidenceLevels);

        } catch (Exception e) {
            log.error("Error running simulation: {}", e.getMessage(), e);
            throw new RuntimeException("Error running simulation", e);
        }
    }

    /**
     * Helper method to find a project by ID
     */
    private Project findProject(String projectId) {
        List<Project> projects = primaveraService.getAllProjects();
        return projects.stream()
                .filter(p -> projectId.equals(p.getObjectId()) || projectId.equals(p.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
    }
}