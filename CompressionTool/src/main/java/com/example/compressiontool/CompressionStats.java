package com.example.compressiontool;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "compression_stats")
public class CompressionStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String operationType; // "COMPRESS" or "DECOMPRESS"

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private long originalSize;

    @Column
    private Long compressedSize; // null for decompress

    @Column(nullable = false)
    private long bytesSaved;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // Constructors
    public CompressionStats() {}

    public CompressionStats(String operationType, String fileName, long originalSize, Long compressedSize, long bytesSaved) {
        this.operationType = operationType;
        this.fileName = fileName;
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
        this.bytesSaved = bytesSaved;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public void setOriginalSize(long originalSize) {
        this.originalSize = originalSize;
    }

    public Long getCompressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(Long compressedSize) {
        this.compressedSize = compressedSize;
    }

    public long getBytesSaved() {
        return bytesSaved;
    }

    public void setBytesSaved(long bytesSaved) {
        this.bytesSaved = bytesSaved;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
