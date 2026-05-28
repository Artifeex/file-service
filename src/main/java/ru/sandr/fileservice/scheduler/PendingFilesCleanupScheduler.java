package ru.sandr.fileservice.scheduler;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.sandr.fileservice.service.FileService;

@Component
@RequiredArgsConstructor
public class PendingFilesCleanupScheduler {

    private static final Duration PENDING_TTL = Duration.ofHours(24);

    private final FileService fileService;

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupPendingFiles() {
        fileService.cleanupExpiredPendingFiles(PENDING_TTL);
    }
}
