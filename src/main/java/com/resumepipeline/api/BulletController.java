package com.resumepipeline.api;

import com.resumepipeline.api.dto.BulletDtos.BulletResponse;
import com.resumepipeline.api.dto.BulletDtos.CreateBulletRequest;
import com.resumepipeline.api.dto.BulletDtos.UpdateBulletRequest;
import com.resumepipeline.bullet.BulletService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class BulletController {

    private final BulletService bullets;

    public BulletController(BulletService bullets) {
        this.bullets = bullets;
    }

    @GetMapping("/projects/{projectId}/bullets")
    public List<BulletResponse> listForProject(@PathVariable UUID projectId) {
        return bullets.listForProject(projectId).stream().map(BulletResponse::from).toList();
    }

    @PostMapping("/projects/{projectId}/bullets")
    public BulletResponse create(@PathVariable UUID projectId,
                                 @RequestBody @Valid CreateBulletRequest req) {
        String[] tags = req.tags() == null ? new String[0] : req.tags().toArray(new String[0]);
        return BulletResponse.from(bullets.create(projectId, req.text(), tags));
    }

    @PutMapping("/bullets/{id}")
    public BulletResponse update(@PathVariable UUID id,
                                 @RequestBody UpdateBulletRequest req) {
        String[] tags = req.tags() == null ? null : req.tags().toArray(new String[0]);
        return BulletResponse.from(bullets.update(id, req.text(), tags));
    }

    @DeleteMapping("/bullets/{id}")
    public void delete(@PathVariable UUID id) {
        bullets.delete(id);
    }
}
