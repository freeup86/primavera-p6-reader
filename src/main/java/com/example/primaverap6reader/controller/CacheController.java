package com.example.primaverap6reader.controller;

import com.example.primaverap6reader.service.PrimaveraRestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing cache operations via REST API
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheController {

    private final PrimaveraRestService primaveraService;

    /**
     * Refresh all caches
     * @return Success message
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshCache() {
        try {
            log.info("API request to refresh cache");
            primaveraService.refreshCache();
            return ResponseEntity.ok("Cache refreshed successfully");
        } catch (Exception e) {
            log.error("Error refreshing cache via API: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to refresh cache: " + e.getMessage());
        }
    }

    /**
     * Force login and refresh the session
     * @return Success message
     */
    @PostMapping("/force-login")
    public ResponseEntity<String> forceLogin() {
        try {
            log.info("API request to force login");
            boolean success = primaveraService.forceLogin();

            if (success) {
                return ResponseEntity.ok("Successfully forced login");
            } else {
                return ResponseEntity.badRequest().body("Force login attempt failed");
            }
        } catch (Exception e) {
            log.error("Error forcing login via API: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to force login: " + e.getMessage());
        }
    }
}