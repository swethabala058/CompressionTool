package com.example.compressiontool;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompressionStatsRepository extends JpaRepository<CompressionStats, Long> {
    // Custom queries can be added here if needed
}
