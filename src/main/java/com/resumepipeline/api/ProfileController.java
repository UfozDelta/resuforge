package com.resumepipeline.api;

import com.resumepipeline.profile.ProfileService;
import com.resumepipeline.profile.ProfileService.ProfileDto;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ProfileDto get() {
        return ProfileDto.from(service.get(), service);
    }

    @PutMapping
    public ProfileDto update(@RequestBody ProfileDto dto) {
        return ProfileDto.from(service.update(dto), service);
    }
}
