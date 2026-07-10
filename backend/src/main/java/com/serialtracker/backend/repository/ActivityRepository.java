package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.Activity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByUsernameOrderByCreatedAtDesc(String username);

    // 🟢 ჩაამატე ეს მეთოდი:
    @Modifying
    @Transactional
    @Query("UPDATE Activity a SET a.username = :newUsername WHERE a.username = :oldUsername")
    void updateUsernameInActivity(@Param("oldUsername") String oldUsername, @Param("newUsername") String newUsername);

}