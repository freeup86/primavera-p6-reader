package com.example.primaverap6reader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.example.primaverap6reader",
        "com.example.primaverap6reader.controller",
        "com.example.primaverap6reader.service",
        "com.example.primaverap6reader.config"
})
public class PrimaveraP6ReaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrimaveraP6ReaderApplication.class, args);
    }
}