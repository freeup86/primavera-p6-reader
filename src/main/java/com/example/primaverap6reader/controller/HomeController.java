package com.example.primaverap6reader.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for the home page that redirects to the projects list
 */
@Controller
public class HomeController {

    /**
     * Redirect root URL to projects list
     * @return Redirect to projects list
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/projects";
    }
}