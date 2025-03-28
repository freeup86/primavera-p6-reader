package com.example.primaverap6reader.service;

import com.example.primaverap6reader.model.Activity;
import com.example.primaverap6reader.model.Project;
import com.example.primaverap6reader.model.Resource;
import com.example.primaverap6reader.model.ResourceAssignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PrimaveraRestService {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String username;
    private final String password;
    private final String databaseName;
    private String cookies;

    public PrimaveraRestService(
            @Value("${primavera.api.baseUrl}") String baseUrl,
            @Value("${primavera.api.username}") String username,
            @Value("${primavera.api.password}") String password,
            @Value("${primavera.api.databaseName:}") String databaseName) {

        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
    }

    /**
     * Login using the method described in Oracle documentation
     * @return boolean indicating if login was successful
     */
    public boolean login() {
        try {
            log.info("Attempting login to Primavera P6...");

            // Step 1: Create the Base64 encoded credentials exactly as shown in Oracle doc
            String auth = username + ":" + password;
            String base64Auth = Base64.getEncoder().encodeToString(auth.getBytes());
            log.info("Base64 encoded credentials created: {}", base64Auth);

            // Step 2: Create the login URL with DatabaseName parameter if provided
            String loginUrl = baseUrl + "/login";
            if (databaseName != null && !databaseName.isEmpty()) {
                loginUrl += "?DatabaseName=" + databaseName;
            }

            // Using HttpHeaders and adding the AuthToken exactly as in the Oracle docs
            HttpHeaders headers = new HttpHeaders();
            headers.set("AuthToken", base64Auth);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

            log.info("Login headers: {}", headers);

            // Create the request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            // Send login request
            log.info("Sending login request to: {}", loginUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    loginUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class);

            log.info("Login response status: {}", response.getStatusCode());

            // Extract and save the cookies from response
            List<String> responseCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (responseCookies != null && !responseCookies.isEmpty()) {
                StringBuilder cookieBuilder = new StringBuilder();
                for (String cookie : responseCookies) {
                    log.info("Response cookie: {}", cookie);
                    cookieBuilder.append(cookie.split(";")[0]).append("; ");
                }
                cookies = cookieBuilder.toString().trim();
                log.info("Saved cookies for session: {}", cookies);
                return true;
            } else {
                log.warn("No cookies returned from login");
                return false;
            }

        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Creates headers with authentication cookies for API requests
     * @return HttpHeaders with proper authentication
     */
    private HttpHeaders createApiHeaders() {
        HttpHeaders headers = new HttpHeaders();

        // Add the session cookie if we have it
        if (cookies != null) {
            headers.set("Cookie", cookies);
        }

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    /**
     * Get all projects
     * @return List of Project objects
     */
    @Cacheable(value = "allProjects")
    public List<Project> getAllProjects() {
        // Check if logged in
        if (cookies == null) {
            log.info("No session cookies, attempting to login first");
            if (!login()) {
                log.error("Login failed, cannot proceed with fetching projects");
                throw new RuntimeException("Unable to login to Primavera P6");
            }
        }

        // Build URL with fields parameter to limit returned data
        String url = baseUrl + "/project?Fields=Name,ObjectId,Id,Status,StartDate,FinishDate,DataDate,Description";

        HttpEntity<String> entity = new HttpEntity<>(createApiHeaders());

        log.info("Fetching all projects from: {}", url);
        log.info("Using cookies: {}", cookies);

        try {
            ResponseEntity<Project[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Project[].class);

            if (response.getBody() != null) {
                log.info("Successfully retrieved {} projects", response.getBody().length);
                return Arrays.asList(response.getBody());
            } else {
                log.warn("No projects found or null response body");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching projects: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get projects with pagination
     * @param page Page number (zero-based)
     * @param size Page size
     * @return List of Project objects for the requested page
     */
    @Cacheable(value = "projectsPage", key = "{#page, #size}")
    public List<Project> getProjectsWithPagination(int page, int size) {
        // Check if logged in
        if (cookies == null) {
            log.info("No session cookies, attempting to login first");
            if (!login()) {
                log.error("Login failed, cannot proceed with fetching projects");
                throw new RuntimeException("Unable to login to Primavera P6");
            }
        }

        // Calculate offset
        int offset = page * size;

        // Build URL with pagination parameters
        String url = baseUrl + "/project?Fields=Name,ObjectId,Id,Status,StartDate,FinishDate,DataDate,Description" +
                "&Offset=" + offset + "&Limit=" + size;

        HttpEntity<String> entity = new HttpEntity<>(createApiHeaders());

        log.info("Fetching paginated projects from: {}", url);
        log.info("Page: {}, Size: {}, Offset: {}", page, size, offset);

        try {
            ResponseEntity<Project[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Project[].class);

            if (response.getBody() != null) {
                log.info("Successfully retrieved {} projects for page {}", response.getBody().length, page);
                return Arrays.asList(response.getBody());
            } else {
                log.warn("No projects found or null response body for page {}", page);
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching paginated projects: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get filtered and sorted projects with pagination
     * @param nameFilter Filter by project name (optional)
     * @param statusFilter Filter by project status (optional)
     * @param sortBy Field to sort by (optional, defaults to Name)
     * @param sortDirection Sort direction (asc/desc, defaults to asc)
     * @param page Page number (zero-based)
     * @param size Page size
     * @return List of filtered and sorted Project objects
     */
    @Cacheable(value = "filteredProjects", key = "{#nameFilter, #statusFilter, #sortBy, #sortDirection, #page, #size}")
    public List<Project> getFilteredProjects(
            String nameFilter,
            String statusFilter,
            String sortBy,
            String sortDirection,
            int page,
            int size) {

        // Check if logged in
        if (cookies == null) {
            log.info("No session cookies, attempting to login first");
            if (!login()) {
                log.error("Login failed, cannot proceed with fetching projects");
                throw new RuntimeException("Unable to login to Primavera P6");
            }
        }

        // Calculate offset
        int offset = page * size;

        // Build URL with filter, sort, and pagination parameters
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        urlBuilder.append("/project?Fields=Name,ObjectId,Id,Status,StartDate,FinishDate,DataDate,Description");

        // Add filter conditions
        StringBuilder filterBuilder = new StringBuilder();

        if (nameFilter != null && !nameFilter.isEmpty()) {
            filterBuilder.append("Name LIKE('%").append(nameFilter).append("%')");
        }

        if (statusFilter != null && !statusFilter.isEmpty()) {
            if (filterBuilder.length() > 0) {
                filterBuilder.append(" AND ");
            }
            filterBuilder.append("Status = '").append(statusFilter).append("'");
        }

        if (filterBuilder.length() > 0) {
            urlBuilder.append("&Filter=").append(filterBuilder);
        }

        // Add sorting
        if (sortBy != null && !sortBy.isEmpty()) {
            urlBuilder.append("&Sort=").append(sortBy);
            if ("desc".equalsIgnoreCase(sortDirection)) {
                urlBuilder.append(" DESC");
            } else {
                urlBuilder.append(" ASC");
            }
        }

        // Add pagination
        urlBuilder.append("&Offset=").append(offset).append("&Limit=").append(size);

        String url = urlBuilder.toString();

        HttpEntity<String> entity = new HttpEntity<>(createApiHeaders());

        log.info("Fetching filtered projects from: {}", url);

        try {
            ResponseEntity<Project[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Project[].class);

            if (response.getBody() != null) {
                log.info("Successfully retrieved {} filtered projects", response.getBody().length);
                return Arrays.asList(response.getBody());
            } else {
                log.warn("No filtered projects found or null response body");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching filtered projects: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get total count of projects (for pagination)
     * @return Total number of projects
     */
    @Cacheable(value = "projectCount")
    public int getTotalProjectCount() {
        // Some P6 REST APIs provide a Count endpoint or header
        // If not available, retrieve all projects and count them
        return getAllProjects().size();
    }

    /**
     * Get a specific project by ID
     * @param projectId Project ID
     * @return Project object
     */
    @Cacheable(value = "project", key = "#projectId")
    public Project getProjectById(String projectId) {
        // Check if logged in
        if (cookies == null) {
            log.info("No session cookies, attempting to login first");
            if (!login()) {
                log.error("Login failed, cannot proceed with fetching project");
                throw new RuntimeException("Unable to login to Primavera P6");
            }
        }

        try {
            String url = baseUrl + "/project/" + projectId;
            HttpEntity<String> entity = new HttpEntity<>(createApiHeaders());
            log.info("Fetching project with ID: {}", projectId);

            ResponseEntity<Project> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Project.class);

            log.info("Successfully retrieved project: {}", response.getBody().getName());
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            // Project not found by direct ID lookup, try finding it in all projects list
            log.warn("Project with ID {} not found via direct lookup, trying alternative approach", projectId);
            try {
                // Attempt to find in all projects list
                List<Project> allProjects = getAllProjects();
                Optional<Project> foundProject = allProjects.stream()
                        .filter(p -> projectId.equals(p.getId()) || projectId.equals(p.getObjectId()))
                        .findFirst();

                if (foundProject.isPresent()) {
                    log.info("Found project {} using alternative lookup", foundProject.get().getName());
                    return foundProject.get();
                } else {
                    log.error("Project with ID {} not found in all projects list", projectId);
                    throw new RuntimeException("Project not found: " + projectId);
                }
            } catch (Exception fallbackError) {
                log.error("Error in fallback project lookup: {}", fallbackError.getMessage(), fallbackError);
                throw new RuntimeException("Unable to find project: " + projectId, fallbackError);
            }
        } catch (Exception e) {
            log.error("Error fetching project with ID {}: {}", projectId, e.getMessage(), e);

            // Check if it might be a session issue and attempt re-login
            if (e.getMessage().contains("403") || e.getMessage().contains("401")) {
                log.info("Possible session expiration, attempting re-login");
                if (forceLogin()) {
                    // Retry after login
                    return getProjectById(projectId);
                }
            }

            throw e;
        }
    }

    /**
     * Get activities for a specific project
     * @param projectObjectId Project Object ID
     * @return List of Activity objects
     */
    @Cacheable(value = "projectActivities", key = "#projectObjectId")
    public List<Activity> getActivitiesForProject(String projectObjectId) {
        // Check if logged in
        if (cookies == null) {
            log.info("No session cookies, attempting to login first");
            if (!login()) {
                log.error("Login failed, cannot proceed with fetching activities");
                throw new RuntimeException("Unable to login to Primavera P6");
            }
        }

        // Use the filter parameter to get activities for a specific project
        // Remove Duration from the Fields list as it's not a valid field
        String url = baseUrl + "/activity?Filter=ProjectObjectId IN(" + projectObjectId +
                ")&Fields=Id,Name,ObjectId,Status,Type,WBSName,StartDate,FinishDate";

        HttpEntity<String> entity = new HttpEntity<>(createApiHeaders());

        log.info("Fetching activities for project ObjectId: {}", projectObjectId);

        try {
            ResponseEntity<Activity[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Activity[].class);

            if (response.getBody() != null) {
                log.info("Successfully retrieved {} activities", response.getBody().length);
                return Arrays.asList(response.getBody());
            } else {
                log.warn("No activities found or null response body");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching activities for project {}: {}", projectObjectId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get activities for a specific project with pagination
     * @param projectObjectId Project Object ID
     * @param page Page number (zero-based)
     * @param size Page size
     * @return List of Activity objects for the requested page
     */
    @Cacheable(value = "projectActivitiesPage", key = "{#projectObjectId, #page, #size}")
    public List<Activity> getActivitiesForProjectWithPagination(String projectObjectId, int page, int size) {
        // Check if logged in
        if (cookies == null) {
            log.info("No session cookies, attempting to login first");
            if (!login()) {
                log.error("Login failed, cannot proceed with fetching activities");
                throw new RuntimeException("Unable to login to Primavera P6");
            }
        }

        // Calculate offset
        int offset = page * size;

        // Use the filter parameter to get activities for a specific project with pagination
        // Remove Duration from the Fields list
        String url = baseUrl + "/activity?Filter=ProjectObjectId IN(" + projectObjectId +
                ")&Fields=Id,Name,ObjectId,Status,Type,WBSName,StartDate,FinishDate" +
                "&Offset=" + offset + "&Limit=" + size;

        HttpEntity<String> entity = new HttpEntity<>(createApiHeaders());

        log.info("Fetching paginated activities for project ObjectId: {}", projectObjectId);
        log.info("Page: {}, Size: {}, Offset: {}", page, size, offset);

        try {
            ResponseEntity<Activity[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Activity[].class);

            if (response.getBody() != null) {
                log.info("Successfully retrieved {} activities for page {}", response.getBody().length, page);
                return Arrays.asList(response.getBody());
            } else {
                log.warn("No activities found or null response body for page {}", page);
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching paginated activities for project {}: {}", projectObjectId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get filtered and sorted activities with pagination
     * @param projectObjectId Project Object ID
     * @param nameFilter Filter by activity name (optional)
     * @param typeFilter Filter by activity type (optional)
     * @param statusFilter Filter by activity status (optional)
     * @param sortBy Field to sort by (optional, defaults to Name)
     * @param sortDirection Sort direction (asc/desc, defaults to asc)
     * @param page Page number (zero-based)
     * @param size Page size
     * @return List of filtered and sorted Activity objects
     */
    @Cacheable(value = "filteredActivities",
            key = "{#projectObjectId, #nameFilter, #typeFilter, #statusFilter, #sortBy, #sortDirection, #page, #size}")
    public List<Activity> getFilteredActivities(
            String projectObjectId,
            String nameFilter,
            String typeFilter,
            String statusFilter,
            String sortBy,
            String sortDirection,
            int page,
            int size) {

        // Check if logged in
        if (cookies == null) {
            log.info("No session cookies, attempting to login first");
            if (!login()) {
                log.error("Login failed, cannot proceed with fetching activities");
                throw new RuntimeException("Unable to login to Primavera P6");
            }
        }

        try {
            // Calculate offset
            int offset = page * size;

            // Start with a simple filter just for the project ID
            StringBuilder urlBuilder = new StringBuilder(baseUrl);
            urlBuilder.append("/activity?Fields=Id,Name,ObjectId,Status,Type,WBSName,StartDate,FinishDate")
                    .append("&Filter=ProjectObjectId IN (").append(projectObjectId).append(")");

            // Add direct equality filters - these are safer and less likely to cause parsing issues
            if (typeFilter != null && !typeFilter.isEmpty()) {
                urlBuilder.append(" AND Type = '").append(typeFilter).append("'");
            }

            if (statusFilter != null && !statusFilter.isEmpty()) {
                urlBuilder.append(" AND Status = '").append(statusFilter).append("'");
            }

            // Add sorting
            if (sortBy != null && !sortBy.isEmpty()) {
                urlBuilder.append("&Sort=").append(sortBy);
                if ("desc".equalsIgnoreCase(sortDirection)) {
                    urlBuilder.append(" DESC");
                } else {
                    urlBuilder.append(" ASC");
                }
            }

            // Add pagination
            urlBuilder.append("&Offset=").append(offset).append("&Limit=").append(size);

            String url = urlBuilder.toString();

            HttpEntity<String> entity = new HttpEntity<>(createApiHeaders());

            log.info("Fetching filtered activities from: {}", url);

            ResponseEntity<Activity[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Activity[].class);

            if (response.getBody() != null) {
                List<Activity> activities = Arrays.asList(response.getBody());

                // If name filter is provided, filter the results in memory
                if (nameFilter != null && !nameFilter.isEmpty()) {
                    String lowerCaseFilter = nameFilter.toLowerCase();
                    activities = activities.stream()
                            .filter(activity -> activity.getName() != null &&
                                    activity.getName().toLowerCase().contains(lowerCaseFilter))
                            .collect(Collectors.toList());
                }

                log.info("Successfully retrieved {} filtered activities", activities.size());
                return activities;
            } else {
                log.warn("No filtered activities found or null response body");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching filtered activities: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get total count of activities for a project (for pagination)
     * @param projectObjectId Project Object ID
     * @return Total number of activities for the project
     */
    @Cacheable(value = "activityCount", key = "#projectObjectId")
    public int getTotalActivityCount(String projectObjectId) {
        // Some P6 REST APIs provide a Count endpoint or header
        // If not available, retrieve all activities and count them
        return getActivitiesForProject(projectObjectId).size();
    }

    /**
     * Refresh all caches
     */
    @CacheEvict(value = {
            "allProjects",
            "projectsPage",
            "filteredProjects",
            "projectCount",
            "project",
            "projectActivities",
            "projectActivitiesPage",
            "filteredActivities",
            "activityCount",
            "activityResourceAssignments",
            "projectResourceAssignments",
            "allResources"
    }, allEntries = true)
    public void refreshCache() {
        log.info("Refreshing all caches");
    }

    /**
     * Get resource assignments for a specific activity
     * @param activityObjectId Activity Object ID
     * @return List of resource assignments
     */
    @Cacheable(value = "activityResourceAssignments", key = "#activityObjectId")
    public List<ResourceAssignment> getResourceAssignmentsForActivity(String activityObjectId) {
        // Check if logged in
        if (cookies == null) {
            log.info("No session cookies, attempting to login first");
            if (!login()) {
                log.error("Login failed, cannot proceed with fetching resource assignments");
                throw new RuntimeException("Unable to login to Primavera P6");
            }
        }

        try {
            // Use the correct capitalization in the URL path
            String url = baseUrl + "/resourceAssignment?Fields=ObjectId,ActivityId,ActivityObjectId,ResourceId,ResourceObjectId,ResourceName,PlannedUnits,ActualUnits,RemainingUnits,PlannedCost,ActualCost,RemainingCost,PlannedStartDate,PlannedFinishDate,ActualStartDate,ActualFinishDate" +
                    "&Filter=ActivityObjectId = " + activityObjectId;

            HttpEntity<String> entity = new HttpEntity<>(createApiHeaders());

            log.info("Fetching resource assignments for activity: {}", activityObjectId);

            ResponseEntity<ResourceAssignment[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, ResourceAssignment[].class);

            if (response.getBody() != null) {
                log.info("Successfully retrieved {} resource assignments", response.getBody().length);
                return Arrays.asList(response.getBody());
            } else {
                log.warn("No resource assignments found or null response body");
                return Collections.emptyList();
            }
        } catch (HttpClientErrorException.NotFound e) {
            // 404 might indicate no resource assignments exist
            log.warn("No resource assignments found for activity {} (404 response)", activityObjectId);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching resource assignments: {}", e.getMessage(), e);

            // Try with a different filter format if it's a possible syntax issue
            try {
                log.info("Attempting alternative filter format for resource assignments");
                // Note the capital 'A' in the URL
                String altUrl = baseUrl + "/resourceAssignment?Fields=ObjectId,ActivityId,ActivityObjectId,ResourceId,ResourceObjectId,ResourceName,PlannedUnits,ActualUnits,RemainingUnits,PlannedCost,ActualCost,RemainingCost,PlannedStartDate,PlannedFinishDate,ActualStartDate,ActualFinishDate" +
                        "&Filter=ActivityObjectId IN (" + activityObjectId + ")";

                HttpEntity<String> entity = new HttpEntity<>(createApiHeaders());
                ResponseEntity<ResourceAssignment[]> response = restTemplate.exchange(
                        altUrl, HttpMethod.GET, entity, ResourceAssignment[].class);

                if (response.getBody() != null) {
                    log.info("Successfully retrieved {} resource assignments using alternative filter", response.getBody().length);
                    return Arrays.asList(response.getBody());
                }
            } catch (Exception altError) {
                log.error("Alternative filter approach also failed: {}", altError.getMessage());
            }

            // Return empty list instead of throwing exception
            return Collections.emptyList();
        }
    }

    /**
     * Get resource assignments for a project
     * @param projectObjectId Project Object ID
     * @return List of resource assignments
     */
    @Cacheable(value = "projectResourceAssignments", key = "#projectObjectId")
    public List<ResourceAssignment> getResourceAssignmentsForProject(String projectObjectId) {
        // Check if logged in
        if (cookies == null) {
            log.info("No session cookies, attempting to login first");
            if (!login()) {
                log.error("Login failed, cannot proceed with fetching resource assignments");
                throw new RuntimeException("Unable to login to Primavera P6");
            }
        }

        // First get all activities for the project
        List<Activity> activities = getActivitiesForProject(projectObjectId);

        if (activities.isEmpty()) {
            return Collections.emptyList();
        }

        // Create a filter with all activity object IDs
        StringBuilder filter = new StringBuilder("ActivityObjectId IN (");
        for (int i = 0; i < activities.size(); i++) {
            if (i > 0) {
                filter.append(", ");
            }
            filter.append(activities.get(i).getObjectId());
        }
        filter.append(")");

        // Note the capital 'A' in resourceAssignment
        String url = baseUrl + "/resourceAssignment?Fields=ObjectId,ActivityId,ActivityObjectId,ResourceId,ResourceObjectId,ResourceName,PlannedUnits,ActualUnits,RemainingUnits,PlannedCost,ActualCost,RemainingCost,PlannedStartDate,PlannedFinishDate,ActualStartDate,ActualFinishDate" +
                "&Filter=" + filter.toString();

        HttpEntity<String> entity = new HttpEntity<>(createApiHeaders());

        log.info("Fetching resource assignments for project activities: {}", projectObjectId);

        try {
            ResponseEntity<ResourceAssignment[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, ResourceAssignment[].class);

            if (response.getBody() != null) {
                log.info("Successfully retrieved {} resource assignments", response.getBody().length);
                return Arrays.asList(response.getBody());
            } else {
                log.warn("No resource assignments found or null response body");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching resource assignments: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Diagnostics for P6API
     */
    @GetMapping("/api/diagnostic/p6api")
    public ResponseEntity<String> diagnosticP6Api() {
        try {
            // Test the base API endpoints
            StringBuilder result = new StringBuilder();
            result.append("P6 API Diagnostic Report\n\n");

            // Add some header info about the connection
            result.append("Base URL: ").append(baseUrl).append("\n");
            result.append("Login successful: ").append(cookies != null).append("\n\n");

            // List available endpoints
            String[] endpoints = {
                    "/project",
                    "/activity",
                    "/resourceassignment",
                    "/resource"
            };

            for (String endpoint : endpoints) {
                try {
                    String url = baseUrl + endpoint + "?Limit=1"; // Just get one record

                    HttpEntity<String> entity = new HttpEntity<>(createApiHeaders());
                    ResponseEntity<String> response = restTemplate.exchange(
                            url, HttpMethod.GET, entity, String.class);

                    result.append("Endpoint: ").append(endpoint)
                            .append(" - Status: ").append(response.getStatusCode())
                            .append(" - Sample: ").append(response.getBody().substring(0,
                                    Math.min(response.getBody().length(), 100)))
                            .append("...\n");
                } catch (Exception e) {
                    result.append("Endpoint: ").append(endpoint)
                            .append(" - Error: ").append(e.getMessage())
                            .append("\n");
                }
            }

            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Diagnostic failed: " + e.getMessage());
        }
    }

    /**
     * Check for P6 API endpoints
     */
    @GetMapping("/api/diagnostic/p6-endpoints")
    public ResponseEntity<String> checkP6Endpoints() {
        try {
            // First ensure we're logged in
            if (cookies == null) {
                login();
            }

            StringBuilder results = new StringBuilder();
            results.append("Primavera P6 REST API Endpoint Check\n\n");

            // List of potential endpoints to check
            String[] endpointsToCheck = {
                    "/project",
                    "/activity",
                    "/resource",
                    "/resourceassignment",
                    "/assignment", // Some versions might use this instead
                    "/resources", // Check plural versions too
                    "/assignments"
            };

            for (String endpoint : endpointsToCheck) {
                try {
                    String url = baseUrl + endpoint;
                    HttpEntity<String> entity = new HttpEntity<>(createApiHeaders());

                    // Just do a HEAD request to check if endpoint exists
                    ResponseEntity<Void> response = restTemplate.exchange(
                            url, HttpMethod.HEAD, entity, Void.class);

                    results.append("✅ ").append(endpoint).append(": ")
                            .append(response.getStatusCode()).append("\n");
                } catch (Exception e) {
                    results.append("❌ ").append(endpoint).append(": ")
                            .append(e.getMessage()).append("\n");
                }
            }

            return ResponseEntity.ok(results.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Diagnostic failed: " + e.getMessage());
        }
    }

    /**
     * Get all resources
     * @return List of Resource objects
     */
    @Cacheable(value = "allResources")
    public List<Resource> getAllResources() {
        // Check if logged in
        if (cookies == null) {
            log.info("No session cookies, attempting to login first");
            if (!login()) {
                log.error("Login failed, cannot proceed with fetching resources");
                throw new RuntimeException("Unable to login to Primavera P6");
            }
        }

        String url = baseUrl + "/resource?Fields=ObjectId,Id,Name,ResourceType,EmailAddress,OfficePhone,OtherPhone,PricePerUnit,MaxUnitsPerTime,CalculateCostFromUnits,IsActive,CalendarName,Title,ResourceNotes";

        HttpEntity<String> entity = new HttpEntity<>(createApiHeaders());

        log.info("Fetching all resources from: {}", url);

        try {
            ResponseEntity<Resource[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Resource[].class);

            if (response.getBody() != null) {
                log.info("Successfully retrieved {} resources", response.getBody().length);
                return Arrays.asList(response.getBody());
            } else {
                log.warn("No resources found or null response body");
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching resources: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Manually force login even if we have cookies
     * @return boolean indicating if forced login was successful
     */
    public boolean forceLogin() {
        cookies = null;
        return login();
    }
}