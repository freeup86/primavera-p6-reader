package com.example.primaverap6reader.controller;

import com.example.primaverap6reader.service.PrimaveraRestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CacheController {

    private final PrimaveraRestService primaveraService;

    /**
     * Refresh cache method with multiple mappings to handle different page scenarios
     * @param referer The page from which the refresh was initiated
     * @return Redirect path
     */
    @PostMapping({
            "/cache/refresh",
            "/projects/cache/refresh",
            "/dashboard/cache/refresh",
            "/refresh",
            "/projects/refresh",
            "/dashboard/refresh"
    })
    public String refreshCache(@RequestHeader(value = "Referer", required = false) String referer) {
        try {
            log.info("Refreshing cache from web UI. Referer: {}", referer);

            // Perform cache refresh
            primaveraService.refreshCache();

            // Determine safe redirect path
            String redirectPath = determineRedirectPath(referer);

            log.info("Redirecting to: {}", redirectPath);
            return "redirect:" + redirectPath;
        } catch (Exception e) {
            log.error("Error refreshing cache: {}", e.getMessage(), e);
            return "redirect:/projects";
        }
    }

    /**
     * Safely determine redirect path
     * @param referer Original referer URL
     * @return Safe redirect path
     */
    private String determineRedirectPath(String referer) {
        if (referer == null || referer.isEmpty()) {
            return "/projects";
        }

        // List of allowed base paths
        String[] allowedPaths = {
                "/projects",
                "/dashboard",
                "/resources"
        };

        try {
            // Remove protocol and domain
            String path = referer.replaceFirst("^https?://[^/]+", "");

            // Check if path starts with any allowed path
            for (String allowedPath : allowedPaths) {
                if (path.startsWith(allowedPath)) {
                    return path;
                }
            }
        } catch (Exception e) {
            log.warn("Error processing referer path: {}", referer);
        }

        return "/projects";
    }
}