package com.resumepipeline.project;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository repo;

    public ProjectService(ProjectRepository repo) {
        this.repo = repo;
    }

    public List<Project> list() {
        return repo.findAll();
    }

    public List<Project> listByKind(Project.Kind kind) {
        return repo.findByKindOrderByCreatedAtDesc(kind);
    }

    public Project get(UUID id) {
        return repo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Project not found: " + id));
    }

    public Project create(Project.Kind kind, String name, String description, String sourcePath,
                          String title, String company, String location, String dates) {
        return repo.save(new Project(kind, name, description, sourcePath, title, company, location, dates));
    }

    public Project update(UUID id, String name, String description, String sourcePath,
                          String title, String company, String location, String dates) {
        Project p = get(id);
        if (name != null)        p.setName(name);
        if (description != null) p.setDescription(description);
        p.setSourcePath(sourcePath);
        p.setTitle(title);
        p.setCompany(company);
        p.setLocation(location);
        p.setDates(dates);
        return repo.save(p);
    }

    public void delete(UUID id) {
        repo.deleteById(id);
    }
}
