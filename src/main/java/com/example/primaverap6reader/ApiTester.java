package com.example.primaverap6reader;

import com.example.primaverap6reader.model.Activity;
import com.example.primaverap6reader.model.Project;
import com.example.primaverap6reader.service.PrimaveraRestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiTester implements CommandLineRunner {

    private final PrimaveraRestService primaveraService;

    @Override
    public void run(String... args) {
        try {
            // Explicitly login first
            log.info("Attempting login to Primavera P6...");
            primaveraService.login();

            // Wait a short time to ensure login process completes
            Thread.sleep(1000);

            log.info("Fetching projects...");
            List<Project> projects = primaveraService.getAllProjects();
            log.info("Successfully retrieved {} projects:", projects.size());

            for (Project project : projects) {
                log.info("Project: {} (ID: {}, ObjectId: {}, Status: {})",
                        project.getName(),
                        project.getId(),
                        project.getObjectId(),
                        project.getStatus());
            }

            // If there are projects, test fetching activities for the first one
            if (!projects.isEmpty()) {
                String firstProjectObjectId = projects.get(0).getObjectId();
                log.info("Fetching activities for first project (ObjectId: {})", firstProjectObjectId);
                List<Activity> activities = primaveraService.getActivitiesForProject(firstProjectObjectId);

                log.info("Successfully retrieved {} activities:", activities.size());
                for (Activity activity : activities) {
                    log.info("Activity: {} (ID: {}, ObjectId: {})",
                            activity.getName(),
                            activity.getId(),
                            activity.getObjectId());
                }
            }

        } catch (Exception e) {
            log.error("Error connecting to Primavera P6 API: {}", e.getMessage(), e);
        }
    }
}