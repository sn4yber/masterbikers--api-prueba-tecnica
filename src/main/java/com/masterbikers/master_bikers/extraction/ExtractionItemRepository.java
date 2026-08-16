package com.masterbikers.master_bikers.extraction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExtractionItemRepository extends JpaRepository<ExtractionItem, UUID> {

	@Query("select item from ExtractionItem item where item.extractionJob.id = :jobId order by item.externalProductId asc")
	List<ExtractionItem> findAllByJobId(@Param("jobId") UUID jobId);

	@Query("select item.id from ExtractionItem item where item.extractionJob.id = :jobId order by item.externalProductId asc")
	List<UUID> findIdsByJobId(@Param("jobId") UUID jobId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select item from ExtractionItem item where item.id = :id")
	Optional<ExtractionItem> findByIdForUpdate(@Param("id") UUID id);

	long countByExtractionJobIdAndStatus(UUID jobId, ExtractionItemStatus status);
}
