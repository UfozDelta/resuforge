package com.resumepipeline.api;

import com.resumepipeline.api.dto.BulletDtos.BulletResponse;
import com.resumepipeline.api.dto.ProjectDtos.CreateProjectRequest;
import com.resumepipeline.api.dto.ProjectDtos.ProjectResponse;
import com.resumepipeline.api.dto.ProjectDtos.UpdateProjectRequest;
import com.resumepipeline.bullet.BulletService;
import com.resumepipeline.project.Project;
import com.resumepipeline.project.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projects;
    private final BulletService bullets;

    public ProjectController(ProjectService projects, BulletService bullets) {
        this.projects = projects;
        this.bullets = bullets;
    }

    @GetMapping
    public List<ProjectResponse> list(@RequestParam(required = false) Project.Kind kind) {
        var rows = kind == null ? projects.list() : projects.listByKind(kind);
        return rows.stream().map(ProjectResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable UUID id) {
        return ProjectResponse.from(projects.get(id));
    }

    @PostMapping
    public ProjectResponse create(@RequestBody @Valid CreateProjectRequest req) {
        Project p = projects.create(
                req.kind() == null ? Project.Kind.PROJECT : req.kind(),
                req.name(), req.description(), req.sourcePath(),
                req.title(), req.company(), req.location(), req.dates());
        return ProjectResponse.from(p);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable UUID id, @RequestBody UpdateProjectRequest req) {
        return ProjectResponse.from(projects.update(id,
                req.name(), req.description(), req.sourcePath(),
                req.title(), req.company(), req.location(), req.dates()));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        projects.delete(id);
    }

    @PostMapping("/{id}/bullets/generate")
    public List<BulletResponse> generateBullets(@PathVariable UUID id) {
        return bullets.generateForProject(id).stream().map(BulletResponse::from).toList();
    }

    public record GenerateBankRequest(List<String> categories) {}

    @PostMapping("/{id}/bullets/generate-bank")
    public List<BulletResponse> generateBank(@PathVariable UUID id, @RequestBody GenerateBankRequest req) {
        return bullets.generateBank(id, req.categories()).stream().map(BulletResponse::from).toList();
    }
}
