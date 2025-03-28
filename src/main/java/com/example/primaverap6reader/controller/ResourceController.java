package com.example.primaverap6reader.controller;

import com.example.primaverap6reader.model.Resource;
import com.example.primaverap6reader.service.PrimaveraRestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for resource-related views and operations
 */
@Controller
@RequestMapping("/resources")
@RequiredArgsConstructor
@Slf4j
public class ResourceController {

    private final PrimaveraRestService primaveraService;

    /**
     * Resource list page with filtering and sorting
     */
    @GetMapping({"", "/"})
    public String listResources(
            @RequestParam(required = false) String nameFilter,
            @RequestParam(required = false) String typeFilter,
            @RequestParam(defaultValue = "Name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            Model model) {

        try {
            log.info("Getting resources list with filters - name: {}, type: {}, sort: {}, direction: {}",
                    nameFilter, typeFilter, sortBy, sortDirection);

            // Get all resources
            List<Resource> resources = primaveraService.getAllResources();

            // Apply filters if provided
            if (nameFilter != null && !nameFilter.isEmpty()) {
                String lowerCaseFilter = nameFilter.toLowerCase();
                resources = resources.stream()
                        .filter(r -> r.getName() != null &&
                                r.getName().toLowerCase().contains(lowerCaseFilter))
                        .collect(Collectors.toList());
            }

            if (typeFilter != null && !typeFilter.isEmpty()) {
                resources = resources.stream()
                        .filter(r -> typeFilter.equals(r.getResourceType()))
                        .collect(Collectors.toList());
            }

            // Sort resources
            if ("Name".equals(sortBy)) {
                resources.sort((r1, r2) -> {
                    if (r1.getName() == null) return "asc".equals(sortDirection) ? -1 : 1;
                    if (r2.getName() == null) return "asc".equals(sortDirection) ? 1 : -1;
                    return "asc".equals(sortDirection) ?
                            r1.getName().compareTo(r2.getName()) :
                            r2.getName().compareTo(r1.getName());
                });
            } else if ("Id".equals(sortBy)) {
                resources.sort((r1, r2) -> {
                    if (r1.getId() == null) return "asc".equals(sortDirection) ? -1 : 1;
                    if (r2.getId() == null) return "asc".equals(sortDirection) ? 1 : -1;
                    return "asc".equals(sortDirection) ?
                            r1.getId().compareTo(r2.getId()) :
                            r2.getId().compareTo(r1.getId());
                });
            } else if ("Type".equals(sortBy)) {
                resources.sort((r1, r2) -> {
                    if (r1.getResourceType() == null) return "asc".equals(sortDirection) ? -1 : 1;
                    if (r2.getResourceType() == null) return "asc".equals(sortDirection) ? 1 : -1;
                    return "asc".equals(sortDirection) ?
                            r1.getResourceType().compareTo(r2.getResourceType()) :
                            r2.getResourceType().compareTo(r1.getResourceType());
                });
            }

            // Extract unique resource types for filter dropdown
            List<String> resourceTypes = resources.stream()
                    .map(Resource::getResourceType)
                    .filter(type -> type != null && !type.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

            // Add data to model
            model.addAttribute("resources", resources);
            model.addAttribute("resourceTypes", resourceTypes);
            model.addAttribute("nameFilter", nameFilter);
            model.addAttribute("typeFilter", typeFilter);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDirection", sortDirection);
            model.addAttribute("totalResources", resources.size());

            return "resources";
        } catch (Exception e) {
            log.error("Error retrieving resources: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to retrieve resources: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Resource details page with assignments
     */
    @GetMapping("/{resourceId}")
    public String resourceDetails(@PathVariable String resourceId, Model model) {
        try {
            log.info("Getting details for resource with ID: {}", resourceId);

            // Get all resources
            List<Resource> resources = primaveraService.getAllResources();

            // Find the specific resource
            Resource resource = resources.stream()
                    .filter(r -> resourceId.equals(r.getObjectId()) || resourceId.equals(r.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Resource not found"));

            // Get all projects to find assignments
            // (This is a placeholder - in a real implementation you might want to
            // fetch resource assignments for this resource across all projects)

            model.addAttribute("resource", resource);

            return "resource-details";
        } catch (Exception e) {
            log.error("Error retrieving resource details: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to retrieve resource details: " + e.getMessage());
            return "error";
        }
    }
}