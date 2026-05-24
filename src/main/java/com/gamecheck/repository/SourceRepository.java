package com.gamecheck.repository;

import com.gamecheck.model.Source;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepository extends JpaRepository<Source, Integer> {

    Optional<Source> findBySourceName(String sourceName);
}
