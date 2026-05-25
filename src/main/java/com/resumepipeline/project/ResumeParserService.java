package com.resumepipeline.project;

import com.resumepipeline.api.dto.ResumeDtos.ParsedExperience;
import com.resumepipeline.api.dto.ResumeDtos.ParsedProject;
import com.resumepipeline.api.dto.ResumeDtos.ParseResumeResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class ResumeParserService {

    private static final Pattern EXPERIENCE_HEADER = Pattern.compile(
            "^(WORK\\s+EXPERIENCE|WORK\\s+HISTORY|PROFESSIONAL\\s+EXPERIENCE|" +
            "PROFESSIONAL\\s+BACKGROUND|RELEVANT\\s+EXPERIENCE|EMPLOYMENT|EXPERIENCE)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PROJECT_HEADER = Pattern.compile(
            "^(PROJECTS?|PERSONAL\\s+PROJECTS?|SIDE\\s+PROJECTS?|SELECTED\\s+PROJECTS?|" +
            "TECHNICAL\\s+PROJECTS?|KEY\\s+PROJECTS?)\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    // Matches: Jan 2020, 2020-01, 2020, May 2021 – Present, 01/2020, etc.
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\.?\\s+\\d{4}" +
            "|\\d{4}[\\s\\-–—/]+(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|present|current|now)" +
            "|\\d{1,2}/\\d{4}" +
            "|\\d{4}\\s*[-–—]\\s*(\\d{4}|present|current|now)" +
            "|\\b\\d{4}\\b"
    );

    public ParseResumeResponse parse(String text) {
        String[] lines = text.split("\\r?\\n");

        // Split into named sections
        Map<String, List<String>> sections = splitSections(lines);

        List<ParsedExperience> experiences = new ArrayList<>();
        List<ParsedProject> projects = new ArrayList<>();

        for (var entry : sections.entrySet()) {
            String header = entry.getKey();
            List<String> body = entry.getValue();
            if (EXPERIENCE_HEADER.matcher(header).matches()) {
                experiences.addAll(parseExperiences(body));
            } else if (PROJECT_HEADER.matcher(header).matches()) {
                projects.addAll(parseProjects(body));
            }
        }

        return new ParseResumeResponse(experiences, projects);
    }

    private Map<String, List<String>> splitSections(String[] lines) {
        // Preserves insertion order
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String currentHeader = null;
        List<String> currentBody = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (isSectionHeader(trimmed)) {
                if (currentHeader != null) {
                    sections.put(currentHeader, new ArrayList<>(currentBody));
                }
                currentHeader = trimmed.toUpperCase();
                currentBody.clear();
            } else if (currentHeader != null) {
                currentBody.add(trimmed);
            }
        }
        if (currentHeader != null && !currentBody.isEmpty()) {
            sections.put(currentHeader, currentBody);
        }
        return sections;
    }

    private boolean isSectionHeader(String line) {
        if (line.isBlank()) return false;
        return EXPERIENCE_HEADER.matcher(line).matches() || PROJECT_HEADER.matcher(line).matches();
    }

    private List<ParsedExperience> parseExperiences(List<String> lines) {
        List<ParsedExperience> result = new ArrayList<>();
        List<List<String>> blocks = splitIntoBlocks(lines);

        for (List<String> block : blocks) {
            if (block.isEmpty()) continue;

            String title = null, company = null, location = null, dates = null;
            List<String> descLines = new ArrayList<>();

            for (int i = 0; i < block.size(); i++) {
                String line = block.get(i);
                if (line.isBlank()) continue;

                if (i == 0) {
                    // First line: try to split "Title | Company" or "Title, Company" or "Title at Company"
                    if (line.contains(" | ")) {
                        String[] parts = line.split("\\s*\\|\\s*", 2);
                        title = parts[0].trim();
                        company = parts[1].trim();
                    } else if (line.contains(" at ")) {
                        String[] parts = line.split(" at ", 2);
                        title = parts[0].trim();
                        company = parts[1].trim();
                    } else {
                        title = line;
                    }
                } else if (i == 1 && company == null && !DATE_PATTERN.matcher(line).find()) {
                    company = line;
                } else if (DATE_PATTERN.matcher(line).find() && dates == null) {
                    // Try to extract location from the same line (before/after the date)
                    String datePart = extractDate(line);
                    String rest = line.replace(datePart, "").trim().replaceAll("^[,·|\\-–—]+|[,·|\\-–—]+$", "").trim();
                    dates = datePart;
                    if (!rest.isBlank()) location = rest;
                } else {
                    descLines.add(line);
                }
            }

            if (title == null || title.isBlank()) continue;

            String name = slugify(company != null ? company + "-" + title : title);
            String description = String.join("\n", descLines).strip();

            result.add(new ParsedExperience(name, title, company, location, dates, description));
        }

        return result;
    }

    private List<ParsedProject> parseProjects(List<String> lines) {
        List<ParsedProject> result = new ArrayList<>();
        List<List<String>> blocks = splitIntoBlocks(lines);

        for (List<String> block : blocks) {
            if (block.isEmpty()) continue;

            String name = null, dates = null;
            List<String> descLines = new ArrayList<>();

            for (int i = 0; i < block.size(); i++) {
                String line = block.get(i);
                if (line.isBlank()) continue;

                if (i == 0) {
                    if (DATE_PATTERN.matcher(line).find()) {
                        dates = extractDate(line);
                        name = line.replace(dates, "").trim().replaceAll("[|\\-–—]+$", "").trim();
                    } else {
                        name = line;
                    }
                } else if (DATE_PATTERN.matcher(line).find() && dates == null) {
                    dates = extractDate(line);
                } else {
                    descLines.add(line);
                }
            }

            if (name == null || name.isBlank()) continue;

            String description = String.join("\n", descLines).strip();
            result.add(new ParsedProject(name, description, dates));
        }

        return result;
    }

    // Split body lines into entry blocks by blank lines
    private List<List<String>> splitIntoBlocks(List<String> lines) {
        List<List<String>> blocks = new ArrayList<>();
        List<String> current = new ArrayList<>();

        for (String line : lines) {
            if (line.isBlank()) {
                if (!current.isEmpty()) {
                    blocks.add(new ArrayList<>(current));
                    current.clear();
                }
            } else {
                current.add(line);
            }
        }
        if (!current.isEmpty()) blocks.add(current);
        return blocks;
    }

    private String extractDate(String line) {
        var matcher = DATE_PATTERN.matcher(line);
        if (matcher.find()) {
            // Try to grab full date range (e.g., "Jan 2020 – Mar 2022")
            int start = matcher.start();
            // extend to end of line or next non-date character after a dash/dash
            String sub = line.substring(start);
            return sub.replaceAll("^([\\w\\s/–—\\-]+?)\\s*[|,·].*$", "$1").trim();
        }
        return "";
    }

    private String slugify(String s) {
        String slug = s.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.substring(0, Math.min(slug.length(), 60));
    }
}
