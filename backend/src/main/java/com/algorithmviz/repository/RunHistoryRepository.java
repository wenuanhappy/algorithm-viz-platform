package com.algorithmviz.repository;

import com.algorithmviz.entity.RunHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RunHistoryRepository extends JpaRepository<RunHistory, Long> {
    List<RunHistory> findTop20ByOrderByCreatedAtDesc();
    List<RunHistory> findByCategoryOrderByCreatedAtDesc(String category);
}
