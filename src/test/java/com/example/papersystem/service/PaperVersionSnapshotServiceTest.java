package com.example.papersystem.service;

import com.example.papersystem.entity.ReferenceEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaperVersionSnapshotServiceTest {

    @Test
    void buildSnapshotSerializesReferenceAuditTimestamps() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        PaperVersionSnapshotService paperVersionSnapshotService = new PaperVersionSnapshotService(objectMapper);

        ReferenceEntry reference = new ReferenceEntry();
        reference.setId(1L);
        reference.setPaperId(2L);
        reference.setAuthors("Alice");
        reference.setTitle("Citation-safe persistence");
        reference.setJournal("Journal of Tests");
        reference.setYear("2026");
        reference.setPages("1-8");
        reference.setCitationNo(1);
        reference.setFormattedText("[1] Alice. Citation-safe persistence.");
        reference.setCreatedAt(LocalDateTime.of(2026, 7, 22, 12, 30, 45));
        reference.setUpdatedAt(LocalDateTime.of(2026, 7, 22, 12, 35, 0));

        String snapshot = paperVersionSnapshotService.buildSnapshot(
                "[{\"id\":\"sec-1\",\"title\":\"正文\",\"content\":\"<p>含引用</p>\"}]",
                List.of(reference));

        JsonNode root = objectMapper.readTree(snapshot);
        assertThat(root.path("sections")).hasSize(1);
        assertThat(root.path("references")).hasSize(1);
        assertThat(root.at("/references/0/createdAt").isMissingNode()).isFalse();
        assertThat(root.at("/references/0/updatedAt").isMissingNode()).isFalse();
    }
}