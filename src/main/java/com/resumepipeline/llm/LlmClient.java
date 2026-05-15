package com.resumepipeline.llm;

import java.util.List;

/**
 * LLM abstraction. Three methods, one per pipeline LLM call.
 * Day 2 implements generateBullets; cleanJd/match come in Day 3.
 */
public interface LlmClient {

    BulletGenerationResult generateBullets(GenerateBulletsRequest req);

    JdCleanResult cleanJd(String rawJd);

    MatchResult match(MatchRequest req);

    // --- types ---

    enum SourceKind { PROJECT, EXPERIENCE }
    record GenerateBulletsRequest(
            SourceKind kind,
            String category,   // slug from CategoryLenses or "general"
            String projectName,
            String description,
            String repoContext,
            String title, String company, String location, String dates
    ) {}
    record BulletGenerationResult(List<GeneratedBullet> bullets) {}
    record GeneratedBullet(String text, List<String> tags) {}

    record JdCleanResult(String cleanJd, String company, String role, List<String> keywords) {}

    record MatchRequest(String cleanJd, String company, String role, List<String> keywords, String roleEmphasis, List<BulletForMatch> bullets) {}
    record BulletForMatch(String bulletId, String text, List<String> tags, String projectName) {}
    record MatchResult(List<RankedBullet> rankedBullets, String coverLetter, List<String> atsMatched, List<String> atsMissing) {}
    record RankedBullet(String bulletId, int rank, String why) {}
}
