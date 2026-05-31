package com.resumepipeline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
@SpringBootApplication
@EnableAsync
@org.springframework.boot.context.properties.ConfigurationPropertiesScan
public class ResumePipelineApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResumePipelineApplication.class, args);
    }
}
