package com.masterbikers.master_bikers.extraction;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExtractionJobRepository extends JpaRepository<ExtractionJob, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select job from ExtractionJob job where job.id = :id")
	Optional<ExtractionJob> findByIdForUpdate(@Param("id") UUID id);
}
