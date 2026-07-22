package com.example.papersystem.service;

import com.example.papersystem.entity.Paper;
import com.example.papersystem.entity.ReferenceEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaperVersionSnapshotService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String buildSnapshot(Paper paper, List<ReferenceEntry> references) {
        return buildSnapshot(paper == null ? null : paper.getContent(), references);
    }

    public String buildSnapshot(String content, List<ReferenceEntry> references) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("sections", parseSections(content));
        snapshot.put("references", normalizeReferences(references));
        try {
            return OBJECT_MAPPER.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("版本快照序列化失败", e);
        }
    }

    public Map<String, Object> parseSnapshot(String rawSnapshot) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("sections", List.of());
        snapshot.put("references", List.of());

        if (rawSnapshot == null || rawSnapshot.isBlank()) {
            return snapshot;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawSnapshot);
            if (root.isArray()) {
                snapshot.put("sections",
                        OBJECT_MAPPER.convertValue(root, new TypeReference<List<Map<String, Object>>>() {
                        }));
                return snapshot;
            }

            if (root.isObject()) {
                if (root.has("sections") && root.get("sections").isArray()) {
                    snapshot.put("sections", OBJECT_MAPPER.convertValue(root.get("sections"),
                            new TypeReference<List<Map<String, Object>>>() {
                            }));
                }
                if (root.has("references") && root.get("references").isArray()) {
                    snapshot.put("references", OBJECT_MAPPER.convertValue(root.get("references"),
                            new TypeReference<List<Map<String, Object>>>() {
                            }));
                }
                if (root.has("rawContent") && !root.get("rawContent").isNull()) {
                    snapshot.put("rawContent", root.get("rawContent").asText(""));
                }
                return snapshot;
            }
        } catch (JsonProcessingException e) {
            snapshot.put("rawContent", rawSnapshot);
        }

        return snapshot;
    }

    public List<Map<String, Object>> normalizeReferences(List<ReferenceEntry> references) {
        if (references == null || references.isEmpty()) {
            return List.of();
        }

        return references.stream().map(reference -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", reference.getId());
            item.put("authors", safe(reference.getAuthors()));
            item.put("title", safe(reference.getTitle()));
            item.put("journal", safe(reference.getJournal()));
            item.put("year", safe(reference.getYear()));
            item.put("pages", safe(reference.getPages()));
            item.put("citationNo", reference.getCitationNo());
            item.put("formattedText", safe(reference.getFormattedText()));
            item.put("createdAt", reference.getCreatedAt());
            item.put("updatedAt", reference.getUpdatedAt());
            return item;
        }).toList();
    }

    private List<Map<String, Object>> parseSections(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        try {
            return OBJECT_MAPPER.readValue(content, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}