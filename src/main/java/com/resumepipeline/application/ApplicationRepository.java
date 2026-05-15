package com.resumepipeline.application;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    List<Application> findAllByOrderByCreatedAtDesc();
    List<Application> findByOutcomeOrderByCreatedAtDesc(String outcome);
}
