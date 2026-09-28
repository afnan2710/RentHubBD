package com.aoop.renthubbd.service;

import com.aoop.renthubbd.dto.AuditEntry;
import com.aoop.renthubbd.dto.AuditFilter;
import com.aoop.renthubbd.model.AuditAction;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AuditLogService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Queue<AuditEntry> buffer = new ConcurrentLinkedQueue<>();
    private final AtomicLong writes = new AtomicLong();
    private final AtomicLong received = new AtomicLong();

    @Value("${app.audit.log.dir}")
    private String auditDir;

    private Path logFile;

    @PostConstruct
    public void init() throws IOException {
        Path dir = Paths.get(auditDir);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        logFile = dir.resolve("admin-actions.log");
        if (!Files.exists(logFile)) Files.createFile(logFile);
    }

    public void record(String actor, AuditAction action, String detail) {
        received.incrementAndGet();
        buffer.offer(new AuditEntry(LocalDateTime.now(), actor, action, detail));
    }

    @Scheduled(fixedDelay = 5000)
    public void flush() {
        if (buffer.isEmpty()) return;

        List<AuditEntry> batch = new ArrayList<>();
        AuditEntry entry;
        while ((entry = buffer.poll()) != null) {
            batch.add(entry);
            if (batch.size() >= 200) break;
        }
        if (batch.isEmpty()) return;

        try (BufferedWriter w = Files.newBufferedWriter(
                logFile, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (AuditEntry e : batch) {
                w.write(e.getTimestamp().format(TS)
                        + " | " + e.getActor()
                        + " | " + (e.getAction() != null ? e.getAction().name() : "UNKNOWN")
                        + " | " + e.getDetail());
                w.newLine();
            }
            writes.addAndGet(batch.size());
        } catch (IOException ignored) {
        }
    }

    public List<AuditEntry> readAll() {
        if (!Files.exists(logFile)) return Collections.emptyList();
        List<AuditEntry> out = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            for (int i = lines.size() - 1; i >= 0; i--) {
                AuditEntry e = parse(lines.get(i));
                if (e != null) out.add(e);
            }
        } catch (IOException ignored) {
        }
        return out;
    }

    public List<AuditEntry> readFiltered(AuditFilter filter) {
        List<AuditEntry> all = readAll();
        List<AuditEntry> result = new ArrayList<>();
        String adminNeedle = filter.getAdmin() != null && !filter.getAdmin().isBlank()
                ? filter.getAdmin().toLowerCase() : null;
        AuditAction actionNeedle = filter.getAction();
        String keywordNeedle = filter.getKeyword() != null && !filter.getKeyword().isBlank()
                ? filter.getKeyword().toLowerCase() : null;

        for (AuditEntry e : all) {
            if (adminNeedle != null && (e.getActor() == null
                    || !e.getActor().toLowerCase().contains(adminNeedle))) continue;
            if (actionNeedle != null && e.getAction() != actionNeedle) continue;
            if (keywordNeedle != null
                    && (e.getDetail() == null
                    || !e.getDetail().toLowerCase().contains(keywordNeedle))) continue;
            result.add(e);
            if (result.size() >= filter.getLimit()) break;
        }
        return result;
    }

    public List<String> distinctActors() {
        List<String> actors = new ArrayList<>();
        for (AuditEntry e : readAll()) {
            if (e.getActor() != null && !actors.contains(e.getActor())) actors.add(e.getActor());
        }
        Collections.sort(actors);
        return actors;
    }

    public Map<String, Long> countByActor() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (AuditEntry e : readAll()) {
            if (e.getActor() == null) continue;
            map.merge(e.getActor(), 1L, Long::sum);
        }
        return map;
    }

    public Map<AuditAction, Long> countByAction() {
        Map<AuditAction, Long> map = new EnumMap<>(AuditAction.class);
        for (AuditEntry e : readAll()) {
            if (e.getAction() == null) continue;
            map.merge(e.getAction(), 1L, Long::sum);
        }
        return map;
    }

    public long countToday() {
        LocalDate today = LocalDate.now();
        long c = 0;
        for (AuditEntry e : readAll()) {
            if (e.getTimestamp() != null && e.getTimestamp().toLocalDate().equals(today)) c++;
        }
        return c;
    }

    public long countLastHour() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        long c = 0;
        for (AuditEntry e : readAll()) {
            if (e.getTimestamp() != null && e.getTimestamp().isAfter(cutoff)) c++;
        }
        return c;
    }

    public long getWriteCount() { return writes.get(); }
    public int getBufferedCount() { return buffer.size(); }
    public long getReceivedCount() { return received.get(); }
    public Path getLogFile() { return logFile; }

    private AuditEntry parse(String line) {
        String[] parts = line.split(" \\| ", 4);
        if (parts.length < 4) return null;

        AuditEntry e = new AuditEntry();
        try { e.setTimestamp(LocalDateTime.parse(parts[0], TS)); }
        catch (Exception ex) { e.setTimestamp(LocalDateTime.now()); }

        e.setActor(parts[1]);

        try { e.setAction(AuditAction.valueOf(parts[2])); }
        catch (Exception ex) { e.setAction(null); }

        e.setDetail(parts[3]);
        return e;
    }
}