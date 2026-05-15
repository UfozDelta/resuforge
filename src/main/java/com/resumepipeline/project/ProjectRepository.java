package com.resumepipeline.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByKindOrderByCreatedAtDesc(Project.Kind kind);
}
