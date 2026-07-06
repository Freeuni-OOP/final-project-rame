package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    // წამოიღებს კონკრეტული იუზერის აქტივობებს უახლესიდან უძველესისკენ (Desc)
    List<Activity> findByUsernameOrderByCreatedAtDesc(String username);
}