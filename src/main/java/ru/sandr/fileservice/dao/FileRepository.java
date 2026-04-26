package ru.sandr.fileservice.dao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.sandr.fileservice.entity.FileMetadata;
import ru.sandr.fileservice.entity.FileStatus;

public interface FileRepository extends JpaRepository<FileMetadata, UUID> {

    List<FileMetadata> findByStatusAndCreatedAtBefore(FileStatus status, Instant createdAt);
}
