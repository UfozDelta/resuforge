package com.resumepipeline.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumepipeline.bullet.Bullet;
import com.resumepipeline.bullet.BulletRepository;
import com.resumepipeline.jd.JdFetcher;
import com.resumepipeline.llm.LlmClient;
import com.resumepipeline.project.Project;
import com.resumepipeline.project.ProjectRepository;
import com.resumepipeline.render.PdfCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);
    private static final int MAX_TOTAL = 8;
    private static final int MAX_PER_PROJECT = 3;

    private final ApplicationRepository repo;
    private final BulletRepository bulletRepo;
    private final ProjectRepository projectRepo;
    private final JdFetcher jdFetcher;
    private final LlmClient llm;
    private final ApplicationRenderer renderer;
    private final PdfCompiler compiler;
    private final ObjectMapper mapper = new ObjectMapper();

    public ApplicationService(ApplicationRepository repo, BulletRepository bulletRepo,
                              ProjectRepository projectRepo, JdFetcher jdFetcher, LlmClient llm,
                              ApplicationRenderer renderer, PdfCompiler compiler) {
        this.repo = repo;
        this.bulletRepo = bulletRepo;
        this.projectRepo = projectRepo;
        this.jdFetcher = jdFetcher;
        this.llm = llm;
        this.renderer = renderer;
        this.compiler = compiler;
    }

    public List<Application> list(String outcome) {
        return outcome == null || outcome.isBlank()
                ? repo.findAllByOrderByCreatedAtDesc()
                : repo.findByOutcomeOrderByCreatedAtDesc(outcome);
    }

    public Application get(UUID id) {
        return repo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Application not found: " + id));
    }

    public Application updateOutcome(UUID id, String outcome) {
        Application a = get(id);
        a.setOutcome(outcome);
        return repo.save(a);
    }

    public Application create(String jdText, String jdUrl, String roleEmphasis) {
        if ((jdText == null || jdText.isBlank()) && (jdUrl == null || jdUrl.isBlank())) {
            throw new IllegalArgumentException("Provide jdText or jdUrl");
        }
        if (jdUrl != null && !jdUrl.isBlank() && (jdText == null || jdText.isBlank())) {
            jdText = jdFetcher.fetch(jdUrl);
        }

        // Stage 3: clean JD
        LlmClient.JdCleanResult clean = llm.cleanJd(jdText);

        // Stage 4: rank + cover
        List<Bullet> allBullets = bulletRepo.findAll();
        if (allBullets.isEmpty()) {
            throw new IllegalStateException("No bullets in the bank — generate or add some first.");
        }
        Map<UUID, Project> projectById = projectRepo.findAll().stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        List<LlmClient.BulletForMatch> bulletsForMatch = allBullets.stream()
                .map(b -> new LlmClient.BulletForMatch(
                        b.getId().toString(),
                        b.getText(),
                        Arrays.asList(b.getTags() == null ? new String[0] : b.getTags()),
                        projectById.containsKey(b.getProjectId()) ? projectById.get(b.getProjectId()).getName() : ""))
                .toList();

        LlmClient.MatchResult match = llm.match(new LlmClient.MatchRequest(
                clean.cleanJd(), clean.company(), clean.role(),
                clean.keywords(), roleEmphasis, bulletsForMatch));

        // Server-side selection: top 8 overall, cap 3 per project
        Map<UUID, Bullet> bulletById = allBullets.stream()
                .collect(Collectors.toMap(Bullet::getId, b -> b));
        List<LlmClient.RankedBullet> rankedSorted = match.rankedBullets().stream()
                .sorted(Comparator.comparingInt(LlmClient.RankedBullet::rank))
                .toList();

        LinkedHashMap<UUID, Integer> perProject = new LinkedHashMap<>();
        List<Bullet> selected = new ArrayList<>();
        for (LlmClient.RankedBullet rb : rankedSorted) {
            if (selected.size() >= MAX_TOTAL) break;
            UUID bid;
            try { bid = UUID.fromString(rb.bulletId()); } catch (Exception e) { continue; }
            Bullet b = bulletById.get(bid);
            if (b == null) continue;
            int count = perProject.getOrDefault(b.getProjectId(), 0);
            if (count >= MAX_PER_PROJECT) continue;
            perProject.put(b.getProjectId(), count + 1);
            selected.add(b);
        }

        // Stage 5: render
        String tex = renderer.render(selected, projectById);
        PdfCompiler.Result r = compiler.compile(tex);

        Application a = new Application();
        a.setJdText(jdText);
        a.setJdUrl(jdUrl);
        a.setRoleEmphasis(roleEmphasis);
        a.setCompany(clean.company());
        a.setRole(clean.role());
        a.setCoverLetter(match.coverLetter());
        a.setAtsMatched(match.atsMatched().toArray(new String[0]));
        a.setAtsMissing(match.atsMissing().toArray(new String[0]));
        a.setSelectedBulletIds(selected.stream().map(Bullet::getId).toArray(UUID[]::new));
        a.setTexBlob(tex.getBytes(StandardCharsets.UTF_8));
        try {
            a.setBulletRanking(mapper.writeValueAsString(rankedSorted));
        } catch (JsonProcessingException e) {
            a.setBulletRanking("[]");
        }
        if (r.success()) {
            a.setPdfBlob(r.pdf());
            a.setTectonicLog(r.log());
        } else {
            log.warn("tectonic failed: {}", r.error());
            a.setTectonicLog("FAILED: " + r.error() + "\n\n" + r.log());
        }
        return repo.save(a);
    }

    /** Override selection and re-render. Does NOT re-call the LLM. */
    public Application rerender(UUID applicationId, List<UUID> selectedBulletIds) {
        Application a = get(applicationId);
        Map<UUID, Bullet> bulletById = bulletRepo.findAllById(selectedBulletIds).stream()
                .collect(Collectors.toMap(Bullet::getId, b -> b));
        List<Bullet> selected = selectedBulletIds.stream()
                .map(bulletById::get).filter(Objects::nonNull).toList();
        Map<UUID, Project> projectById = projectRepo.findAll().stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        String tex = renderer.render(selected, projectById);
        PdfCompiler.Result r = compiler.compile(tex);

        a.setSelectedBulletIds(selected.stream().map(Bullet::getId).toArray(UUID[]::new));
        a.setTexBlob(tex.getBytes(StandardCharsets.UTF_8));
        if (r.success()) {
            a.setPdfBlob(r.pdf());
            a.setTectonicLog(r.log());
        } else {
            a.setTectonicLog("FAILED: " + r.error() + "\n\n" + r.log());
        }
        return repo.save(a);
    }
}
