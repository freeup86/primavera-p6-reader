package com.example.primaverap6reader.config;

import com.example.primaverap6reader.model.Activity;
import com.example.primaverap6reader.service.PrimaveraRestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ThymeleafConfig {

    @Autowired
    private PrimaveraRestService primaveraService;

    @Bean
    public ActivityNameHelper activityNameHelper() {
        return new ActivityNameHelper(primaveraService);
    }

    public static class ActivityNameHelper {
        private final PrimaveraRestService primaveraService;

        public ActivityNameHelper(PrimaveraRestService primaveraService) {
            this.primaveraService = primaveraService;
        }

        public String getActivityName(String activityObjectId, String projectObjectId) {
            try {
                List<Activity> activities = primaveraService.getActivitiesForProject(projectObjectId);
                return activities.stream()
                        .filter(a -> activityObjectId.equals(a.getObjectId()))
                        .map(Activity::getName)
                        .findFirst()
                        .orElse("Unknown Activity");
            } catch (Exception e) {
                return "Unknown Activity";
            }
        }
    }
}