package com.example.primaverap6reader.controller;

import com.example.primaverap6reader.model.Activity;
import com.example.primaverap6reader.model.Project;
import com.example.primaverap6reader.model.ResourceAssignment;
import com.example.primaverap6reader.service.PrimaveraRestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for project-related views and operations
 */
@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final PrimaveraRestService primaveraService;

    /**
     * Project list page with pagination, filtering, and sorting
     */
    @GetMapping({"", "/"})
    public String listProjects(
            @RequestParam(required = false) String nameFilter,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(defaultValue = "Name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        try {
            log.info("Getting projects list with filters - name: {}, status: {}, sort: {}, direction: {}, page: {}, size: {}",
                    nameFilter, statusFilter, sortBy, sortDirection, page, size);

            List<Project> projects;

            // Check if any filters are applied
            boolean filtersApplied = (nameFilter != null && !nameFilter.isEmpty())
                    || (statusFilter != null && !statusFilter.isEmpty())
                    || !sortBy.equals("Name")
                    || !sortDirection.equals("asc");

            if (filtersApplied) {
                // Get filtered, sorted projects with pagination
                projects = primaveraService.getFilteredProjects(
                        nameFilter, statusFilter, sortBy, sortDirection, page, size);
            } else {
                // Get all projects with pagination
                projects = primaveraService.getProjectsWithPagination(page, size);
            }

            // Get total count for pagination
            int totalProjects = primaveraService.getTotalProjectCount();
            int totalPages = (int) Math.ceil((double) totalProjects / size);

            // Add all data to the model
            model.addAttribute("projects", projects);
            model.addAttribute("nameFilter", nameFilter);
            model.addAttribute("statusFilter", statusFilter);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDirection", sortDirection);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalProjects", totalProjects);

            return "projects";
        } catch (Exception e) {
            log.error("Error retrieving projects: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to retrieve projects: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Project details page with activities that supports pagination, filtering, and sorting
     */
    @GetMapping("/{objectId}")
    public String projectDetails(
            @PathVariable String objectId,
            @RequestParam(required = false) String nameFilter,
            @RequestParam(required = false) String typeFilter,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(defaultValue = "Name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Model model) {

        try {
            log.info("Getting details for project with ObjectId: {}", objectId);

            // Find the project
            List<Project> projects = primaveraService.getAllProjects();
            Project project = projects.stream()
                    .filter(p -> objectId.equals(p.getObjectId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Get activities with filtering, sorting, and pagination
            List<Activity> activities;
            boolean filtersApplied = (nameFilter != null && !nameFilter.isEmpty())
                    || (typeFilter != null && !typeFilter.isEmpty())
                    || (statusFilter != null && !statusFilter.isEmpty())
                    || !sortBy.equals("Name")
                    || !sortDirection.equals("asc");

            if (filtersApplied) {
                activities = primaveraService.getFilteredActivities(
                        objectId, nameFilter, typeFilter, statusFilter, sortBy, sortDirection, page, size);
            } else {
                activities = primaveraService.getActivitiesForProjectWithPagination(objectId, page, size);
            }

            // Get total activity count for pagination
            int totalActivities = primaveraService.getTotalActivityCount(objectId);
            int totalPages = (int) Math.ceil((double) totalActivities / size);

            // Add all data to the model
            model.addAttribute("project", project);
            model.addAttribute("activities", activities);
            model.addAttribute("nameFilter", nameFilter);
            model.addAttribute("typeFilter", typeFilter);
            model.addAttribute("statusFilter", statusFilter);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDirection", sortDirection);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalActivities", totalActivities);

            return "project-details";
        } catch (Exception e) {
            log.error("Error retrieving project details: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to retrieve project details: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Project timeline view for Gantt chart visualization
     */
    @GetMapping("/{objectId}/timeline")
    public String projectTimeline(@PathVariable String objectId, Model model) {
        try {
            log.info("Getting timeline for project with ObjectId: {}", objectId);

            // Find the project
            List<Project> projects = primaveraService.getAllProjects();
            Project project = projects.stream()
                    .filter(p -> objectId.equals(p.getObjectId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Get all activities for the project
            List<Activity> activities = primaveraService.getActivitiesForProject(objectId);

            model.addAttribute("project", project);
            model.addAttribute("activities", activities);

            return "project-timeline";
        } catch (Exception e) {
            log.error("Error generating project timeline: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to generate project timeline: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Refresh cache and redirect back
     */
    @PostMapping("/cache/refresh")
    public String refreshCache(@RequestHeader(value = "Referer", required = false) String referer) {
        try {
            log.info("Refreshing cache from web UI");
            primaveraService.refreshCache();

            // Redirect back to the previous page or home
            return "redirect:" + (referer != null ? referer : "/projects");
        } catch (Exception e) {
            log.error("Error refreshing cache: {}", e.getMessage(), e);
            return "redirect:/projects";
        }
    }

    /**
     * Activity details page with resource assignments
     */
    @GetMapping("/{projectId}/activities/{activityId}")
    public String activityDetails(
            @PathVariable String projectId,
            @PathVariable String activityId,
            Model model) {

        try {
            log.info("Getting details for activity {} in project {}", activityId, projectId);

            // Find the project
            Project project = primaveraService.getProjectById(projectId);

            // Find the activity
            List<Activity> activities = primaveraService.getActivitiesForProject(projectId);
            Activity activity = activities.stream()
                    .filter(a -> activityId.equals(a.getObjectId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Activity not found"));

            // Get resource assignments for the activity
            List<ResourceAssignment> resourceAssignments = primaveraService.getResourceAssignmentsForActivity(activityId);

            model.addAttribute("project", project);
            model.addAttribute("activity", activity);
            model.addAttribute("resourceAssignments", resourceAssignments);

            return "activity-details";
        } catch (Exception e) {
            log.error("Error retrieving activity details: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to retrieve activity details: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Project resources page
     */
    @GetMapping("/{projectId}/resources")
    public String projectResources(
            @PathVariable String projectId,
            Model model) {

        try {
            log.info("Getting resources for project {}", projectId);

            // Find the project
            Project project = primaveraService.getProjectById(projectId);

            // Get resource assignments for the project
            List<ResourceAssignment> resourceAssignments = primaveraService.getResourceAssignmentsForProject(projectId);

            // Group assignments by resource
            Map<String, List<ResourceAssignment>> assignmentsByResource = resourceAssignments.stream()
                    .collect(Collectors.groupingBy(ResourceAssignment::getResourceObjectId));

            model.addAttribute("project", project);
            model.addAttribute("resourceAssignments", resourceAssignments);
            model.addAttribute("assignmentsByResource", assignmentsByResource);

            return "project-resources";
        } catch (Exception e) {
            log.error("Error retrieving project resources: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to retrieve project resources: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Force login and redirect back
     */
    @PostMapping("/login/force")
    public String forceLogin(@RequestHeader(value = "Referer", required = false) String referer) {
        try {
            log.info("Forcing login from web UI");
            primaveraService.forceLogin();

            // Redirect back to the previous page or home
            return "redirect:" + (referer != null ? referer : "/projects");
        } catch (Exception e) {
            log.error("Error forcing login: {}", e.getMessage(), e);
            return "redirect:/projects";
        }
    }
}