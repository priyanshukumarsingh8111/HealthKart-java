package com.healthkart;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JSON-file store (data/db.json). Unlike the Node version, every read/write goes through
 * one lock, so concurrent requests can no longer overwrite each other (slot double-booking).
 * The file is re-read on each call, so manual edits to db.json are picked up live.
 */
@Component
public class JsonStore {
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path file;

    public JsonStore(@Value("${healthkart.db-file:data/db.json}") String dbFile) {
        Path p = Path.of(dbFile);
        if (!Files.exists(p) && Files.exists(Path.of("healthkart-java/" + dbFile))) {
            p = Path.of("healthkart-java/" + dbFile);
        }
        this.file = p;
    }

    /** Read-only access. */
    public synchronized <T> T read(Function<Map<String, Object>, T> fn) {
        return fn.apply(load());
    }

    /** Read-modify-write, atomically. If fn throws, nothing is saved. */
    public synchronized <T> T write(Function<Map<String, Object>, T> fn) {
        Map<String, Object> db = load();
        T result = fn.apply(db);
        save(db);
        return result;
    }

    private Map<String, Object> load() {
        try {
            if (!Files.exists(file)) return emptyDb();
            String content = Files.readString(file);
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }
            return mapper.readValue(content, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            System.err.println("Error reading " + file + ": " + e.getMessage());
            return emptyDb();
        }
    }

    private Map<String, Object> emptyDb() {
        Map<String, Object> d = new LinkedHashMap<>();
        for (String k : new String[]{"doctors", "medicines", "appointments", "prescriptions", "reminders", "notifications"})
            d.put(k, new ArrayList<>());
        d.put("bookedSlots", new LinkedHashMap<>());
        return d;
    }

    private void save(Map<String, Object> db) {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Files.writeString(file, mapper.writeValueAsString(db));
        } catch (IOException e) {
            System.err.println("Error writing " + file + ": " + e.getMessage());
        }
    }
}
