package ru.sandr.fileservice.file.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    List<FileEntity> findByStatusAndCreatedAtBefore(FileStatus status, Instant createdAt);
}
