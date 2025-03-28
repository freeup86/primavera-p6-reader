package com.example.primaverap6reader.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    /**
     * Redirect root URL to projects list, preserving filter parameters
     * @return Redirect to projects list with parameters
     */
    @GetMapping("/")
    public String home(
            @RequestParam(required = false) String nameFilter,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false, defaultValue = "Name") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDirection,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        // Build redirect URL with all parameters
        StringBuilder redirectUrl = new StringBuilder("redirect:/projects?");

        if (nameFilter != null && !nameFilter.isEmpty()) {
            redirectUrl.append("nameFilter=").append(nameFilter).append("&");
        }
        if (statusFilter != null && !statusFilter.isEmpty()) {
            redirectUrl.append("statusFilter=").append(statusFilter).append("&");
        }

        redirectUrl.append("sortBy=").append(sortBy).append("&")
                .append("sortDirection=").append(sortDirection).append("&")
                .append("page=").append(page).append("&")
                .append("size=").append(size);

        return redirectUrl.toString();
    }
}