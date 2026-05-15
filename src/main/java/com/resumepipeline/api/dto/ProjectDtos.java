package com.resumepipeline.api.dto;

import com.resumepipeline.project.Project;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public class ProjectDtos {

    public record CreateProjectRequest(
            Project.Kind kind,
            @NotBlank String name,
            @NotBlank String description,
            String sourcePath,
            String title, String company, String location, String dates
    ) {}

    public record UpdateProjectRequest(
            String name,
            String description,
            String sourcePath,
            String title, String company, String location, String dates
    ) {}

    public record ProjectResponse(
            UUID id,
            Project.Kind kind,
            String name,
            String description,
            String sourcePath,
            String title, String company, String location, String dates,
            Instant createdAt
    ) {
        public static ProjectResponse from(Project p) {
            return new ProjectResponse(
                    p.getId(), p.getKind(), p.getName(), p.getDescription(), p.getSourcePath(),
                    p.getTitle(), p.getCompany(), p.getLocation(), p.getDates(),
                    p.getCreatedAt());
        }
    }
}
