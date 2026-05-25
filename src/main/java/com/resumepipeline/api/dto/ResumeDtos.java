package com.resumepipeline.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class ResumeDtos {

    public record ParseResumeRequest(@NotBlank String text) {}

    public record ParsedExperience(
            String name,
            String title,
            String company,
            String location,
            String dates,
            String description
    ) {}

    public record ParsedProject(
            String name,
            String description,
            String dates
    ) {}

    public record ParseResumeResponse(
            List<ParsedExperience> experiences,
            List<ParsedProject> projects
    ) {}

    public record BulkCreateProjectsRequest(
            List<ProjectDtos.CreateProjectRequest> items
    ) {}
}
