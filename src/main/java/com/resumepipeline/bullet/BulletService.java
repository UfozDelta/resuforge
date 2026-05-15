package com.resumepipeline.bullet;

import com.resumepipeline.llm.CategoryLenses;
import com.resumepipeline.llm.LlmClient;
import com.resumepipeline.llm.RepoContextReader;
import com.resumepipeline.project.Project;
import com.resumepipeline.project.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BulletService {

    private static final Logger log = LoggerFactory.getLogger(BulletService.class);

    private final BulletRepository repo;
    private final ProjectService projectService;
    private final LlmClient llm;
    private final RepoContextReader repoContextReader;

    public BulletService(BulletRepository repo, ProjectService projectService,
                         LlmClient llm, RepoContextReader repoContextReader) {
        this.repo = repo;
        this.projectService = projectService;
        this.llm = llm;
        this.repoContextReader = repoContextReader;
    }

    public List<Bullet> listForProject(UUID projectId) {
        return repo.findByProjectIdOrderByCreatedAtAsc(projectId);
    }

    public Bullet create(UUID projectId, String text, String[] tags) {
        projectService.get(projectId);
        return repo.save(new Bullet(projectId, text, tags));
    }

    public Bullet update(UUID bulletId, String text, String[] tags) {
        Bullet b = repo.findById(bulletId).orElseThrow(() ->
                new IllegalArgumentException("Bullet not found: " + bulletId));
        if (text != null)  b.setText(text);
        if (tags != null)  b.setTags(tags);
        return repo.save(b);
    }

    public void delete(UUID bulletId) {
        repo.deleteById(bulletId);
    }

    /** Single un-categorized generation. Persists bullets with category="general". */
    public List<Bullet> generateForProject(UUID projectId) {
        return generateForProjectAndCategory(projectId, "general");
    }

    /** Generate bullets for one project and one category lens. */
    public List<Bullet> generateForProjectAndCategory(UUID projectId, String category) {
        Project p = projectService.get(projectId);
        String repoContext = repoContextReader.read(p.getSourcePath());

        LlmClient.SourceKind sk = p.getKind() == Project.Kind.EXPERIENCE
                ? LlmClient.SourceKind.EXPERIENCE
                : LlmClient.SourceKind.PROJECT;

        String cat = (category == null || category.isBlank()) ? "general" : category;

        LlmClient.BulletGenerationResult result = llm.generateBullets(
                new LlmClient.GenerateBulletsRequest(
                        sk, cat,
                        p.getName(), p.getDescription(), repoContext,
                        p.getTitle(), p.getCompany(), p.getLocation(), p.getDates()));

        return result.bullets().stream()
                .map(g -> repo.save(new Bullet(projectId, g.text(), g.tags().toArray(new String[0]), cat)))
                .toList();
    }

    /**
     * Run one LLM call per requested category, sequentially (respects Gemini Flash
     * free-tier RPM caps), and persist each result with its category tag.
     * Returns the combined list in the order the categories were requested.
     */
    public List<Bullet> generateBank(UUID projectId, List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("categories cannot be empty");
        }
        for (String c : categories) {
            if (!CategoryLenses.LENSES.containsKey(c)) {
                throw new IllegalArgumentException("Unknown category: " + c);
            }
        }
        List<Bullet> combined = new ArrayList<>();
        for (String c : categories) {
            log.info("Generating bank for project {} category {}", projectId, c);
            combined.addAll(generateForProjectAndCategory(projectId, c));
        }
        return combined;
    }
}
