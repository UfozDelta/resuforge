package com.resumepipeline.api;

import com.resumepipeline.api.dto.ApplicationDtos.*;
import com.resumepipeline.application.Application;
import com.resumepipeline.application.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApplicationSummary> list(@RequestParam(required = false) String outcome) {
        return service.list(outcome).stream().map(ApplicationSummary::from).toList();
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable UUID id) {
        return ApplicationResponse.from(service.get(id));
    }

    @PostMapping
    public ApplicationResponse create(@RequestBody @Valid CreateApplicationRequest req) {
        Application a = service.create(req.jdText(), req.jdUrl(), req.roleEmphasis());
        return ApplicationResponse.from(a);
    }

    @PatchMapping("/{id}")
    public ApplicationResponse updateOutcome(@PathVariable UUID id,
                                             @RequestBody @Valid UpdateOutcomeRequest req) {
        return ApplicationResponse.from(service.updateOutcome(id, req.outcome()));
    }

    @PostMapping("/{id}/rerender")
    public ApplicationResponse rerender(@PathVariable UUID id, @RequestBody RerenderRequest req) {
        return ApplicationResponse.from(service.rerender(id, req.selectedBulletIds()));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        Application a = service.get(id);
        if (a.getPdfBlob() == null || a.getPdfBlob().length == 0) {
            return ResponseEntity.status(404).body("No PDF stored (compile failed?)".getBytes());
        }
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_PDF);
        String fname = "resume-" + (a.getCompany() == null ? "app" : a.getCompany().replaceAll("\\W+", "_")) + ".pdf";
        h.setContentDispositionFormData("inline", fname);
        return new ResponseEntity<>(a.getPdfBlob(), h, 200);
    }

    @GetMapping(value = "/{id}/cover-letter", produces = MediaType.TEXT_PLAIN_VALUE)
    public String coverLetter(@PathVariable UUID id) {
        return service.get(id).getCoverLetter();
    }
}
