package com.example.primaverap6reader.service;

import com.example.primaverap6reader.model.Activity;
import com.example.primaverap6reader.model.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CriticalPathService {

    private final PrimaveraRestService primaveraService;

    /**
     * Identify the critical path activities for a project
     * @param projectObjectId Project Object ID
     * @return List of activities on the critical path
     */
    public List<Activity> identifyCriticalPath(String projectObjectId) {
        // Get all activities for the project
        List<Activity> activities = primaveraService.getActivitiesForProject(projectObjectId);

        // In a real implementation, you would analyze activity relationships
        // For this example, we'll identify activities with zero or minimal float
        // A more robust implementation would build a network diagram and calculate ES, EF, LS, LF

        Map<String, Activity> activityMap = activities.stream()
                .collect(Collectors.toMap(Activity::getObjectId, a -> a));

        // Placeholder for predecessor/successor relationships
        // In a real implementation, this would come from the P6 API
        Map<String, List<String>> successors = new HashMap<>();

        // Calculate early start/finish and late start/finish
        // For demo purposes, activities with durations in the top quartile
        // are considered critical

        List<Activity> sortedByDuration = activities.stream()
                .filter(a -> a.getDurationHours() != null)
                .sorted(Comparator.comparing(Activity::getDurationHours).reversed())
                .collect(Collectors.toList());

        if (sortedByDuration.isEmpty()) {
            return Collections.emptyList();
        }

        // Consider top 25% by duration as critical
        int criticalCount = Math.max(1, sortedByDuration.size() / 4);
        return sortedByDuration.subList(0, criticalCount);
    }

    /**
     * Calculate float time for each activity
     * @param projectObjectId Project Object ID
     * @return Map of activity ID to float hours
     */
    public Map<String, Double> calculateActivityFloat(String projectObjectId) {
        List<Activity> activities = primaveraService.getActivitiesForProject(projectObjectId);
        Map<String, Double> floatMap = new HashMap<>();

        // Simplified float calculation
        // In a real implementation, this would involve ES, EF, LS, LF calculations
        activities.forEach(activity -> {
            // Placeholder calculation - would use actual schedule data in production
            double floatTime = Math.random() * 40; // Random float for demonstration
            floatMap.put(activity.getId(), floatTime);
        });

        return floatMap;
    }
}