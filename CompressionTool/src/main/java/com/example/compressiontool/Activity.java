package com.example.compressiontool;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationType operationType;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private long originalSize;

    @Column(nullable = false)
    private long resultSize;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // Constructors
    public Activity() {}

    public Activity(OperationType operationType, String fileName, long originalSize, long resultSize) {
        this.operationType = operationType;
        this.fileName = fileName;
        this.originalSize = originalSize;
        this.resultSize = resultSize;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OperationType getOperationType() { return operationType; }
    public void setOperationType(OperationType operationType) { this.operationType = operationType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getOriginalSize() { return originalSize; }
    public void setOriginalSize(long originalSize) { this.originalSize = originalSize; }

    public long getResultSize() { return resultSize; }
    public void setResultSize(long resultSize) { this.resultSize = resultSize; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
