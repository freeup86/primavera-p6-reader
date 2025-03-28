package com.example.primaverap6reader.service;

import com.example.primaverap6reader.model.Activity;
import com.example.primaverap6reader.model.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectStatisticsService {

    private final PrimaveraRestService primaveraService;

    /**
     * Get count of projects by status
     * @return Map of status to count
     */
    @Cacheable("projectStatusStats")
    public Map<String, Integer> getProjectCountByStatus() {
        List<Project> projects = primaveraService.getAllProjects();
        Map<String, Integer> statusCounts = new HashMap<>();

        for (Project project : projects) {
            String status = project.getStatus() != null ? project.getStatus() : "Unknown";
            statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
        }

        return statusCounts;
    }

    /**
     * Get count of activities by type
     * @return Map of activity type to count
     */
    @Cacheable("activityTypeStats")
    public Map<String, Integer> getActivityCountByType() {
        List<Project> projects = primaveraService.getAllProjects();
        Map<String, Integer> typeCounts = new HashMap<>();

        for (Project project : projects) {
            List<Activity> activities = primaveraService.getActivitiesForProject(project.getObjectId());

            for (Activity activity : activities) {
                String type = activity.getType() != null ? activity.getType() : "Unknown";
                typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
            }
        }

        return typeCounts;
    }

    /**
     * Get count of activities by status
     * @return Map of activity status to count
     */
    @Cacheable("activityStatusStats")
    public Map<String, Integer> getActivityCountByStatus() {
        List<Project> projects = primaveraService.getAllProjects();
        Map<String, Integer> statusCounts = new HashMap<>();

        for (Project project : projects) {
            List<Activity> activities = primaveraService.getActivitiesForProject(project.getObjectId());

            for (Activity activity : activities) {
                String status = activity.getStatus() != null ? activity.getStatus() : "Unknown";
                statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
            }
        }

        return statusCounts;
    }

    /**
     * Get overdue projects (projects past finish date but not completed)
     * @return List of overdue projects
     */
    @Cacheable("overdueProjects")
    public List<Project> getOverdueProjects() {
        List<Project> projects = primaveraService.getAllProjects();
        Date today = new Date();

        return projects.stream()
                .filter(p -> p.getFinishDate() != null &&
                        p.getFinishDate().before(today) &&
                        !"Completed".equals(p.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Get upcoming projects (starting within next two weeks)
     * @return List of upcoming projects
     */
    @Cacheable("upcomingProjects")
    public List<Project> getUpcomingProjects() {
        List<Project> projects = primaveraService.getAllProjects();
        Date today = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.add(Calendar.DAY_OF_MONTH, 14);
        Date twoWeeksFromNow = calendar.getTime();

        return projects.stream()
                .filter(p -> p.getStartDate() != null &&
                        p.getStartDate().after(today) &&
                        p.getStartDate().before(twoWeeksFromNow))
                .collect(Collectors.toList());
    }

    /**
     * Get projects grouped by start month and year
     * @return Map of month-year string to count
     */
    @Cacheable("projectTimelineStats")
    public Map<String, Integer> getProjectsTimelineByMonth() {
        List<Project> projects = primaveraService.getAllProjects();
        Map<String, Integer> monthYearCounts = new TreeMap<>(); // TreeMap for natural ordering

        for (Project project : projects) {
            if (project.getStartDate() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(project.getStartDate());

                String monthYear = String.format("%d-%02d",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1); // +1 because Calendar months are 0-based

                monthYearCounts.put(monthYear, monthYearCounts.getOrDefault(monthYear, 0) + 1);
            }
        }

        return monthYearCounts;
    }

    /**
     * Get projects with most activities
     * @param limit Maximum number of projects to return
     * @return Map of project name to activity count
     */
    @Cacheable(value = "topProjectsStats", key = "#limit")
    public Map<String, Integer> getProjectsWithMostActivities(int limit) {
        List<Project> projects = primaveraService.getAllProjects();
        Map<String, Integer> projectActivityCounts = new HashMap<>();

        for (Project project : projects) {
            List<Activity> activities = primaveraService.getActivitiesForProject(project.getObjectId());
            projectActivityCounts.put(project.getName(), activities.size());
        }

        // Sort by activity count (descending) and limit results
        return projectActivityCounts.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new)); // LinkedHashMap to preserve order
    }

    /**
     * Get total activity duration by project
     * @return Map of project name to total duration in hours
     */
    @Cacheable("projectDurationStats")
    public Map<String, Double> getTotalDurationByProject() {
        List<Project> projects = primaveraService.getAllProjects();
        Map<String, Double> projectDurations = new HashMap<>();

        for (Project project : projects) {
            List<Activity> activities = primaveraService.getActivitiesForProject(project.getObjectId());

            double totalDuration = activities.stream()
                    .filter(a -> a.getDurationHours() != null)
                    .mapToDouble(Activity::getDurationHours)
                    .sum();

            projectDurations.put(project.getName(), totalDuration);
        }

        return projectDurations;
    }

    /**
     * Get projects summary statistics
     * @return Map with various summary metrics
     */
    @Cacheable("projectSummaryStats")
    public Map<String, Object> getProjectsSummaryStatistics() {
        List<Project> projects = primaveraService.getAllProjects();
        Map<String, Object> stats = new HashMap<>();

        // Total projects
        stats.put("totalProjects", projects.size());

        // Status counts
        Map<String, Long> statusCounts = projects.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getStatus() != null ? p.getStatus() : "Unknown",
                        Collectors.counting()));
        stats.put("statusCounts", statusCounts);

        // Calculate date ranges
        OptionalLong minStartDate = projects.stream()
                .filter(p -> p.getStartDate() != null)
                .mapToLong(p -> p.getStartDate().getTime())
                .min();

        OptionalLong maxFinishDate = projects.stream()
                .filter(p -> p.getFinishDate() != null)
                .mapToLong(p -> p.getFinishDate().getTime())
                .max();

        if (minStartDate.isPresent()) {
            stats.put("earliestStartDate", new Date(minStartDate.getAsLong()));
        }

        if (maxFinishDate.isPresent()) {
            stats.put("latestFinishDate", new Date(maxFinishDate.getAsLong()));
        }

        // Other useful statistics could be added here

        return stats;
    }
}