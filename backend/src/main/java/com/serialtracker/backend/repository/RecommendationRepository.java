package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.Recommendation;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    // პროფილისთვის: ყველა მიღებული რეკომენდაცია ახლიდან ძველისკენ
    List<Recommendation> findByTargetUsernameOrderByCreatedAtDesc(String targetUsername);

    // ჰედერის ნოტიფიკაციებისთვის: მხოლოდ წაუკითხავი რექვესტები
    List<Recommendation> findByTargetUsernameAndIsReadFalse(String targetUsername);

    List<Recommendation> findBySenderUsername(String senderUsername);

    boolean existsBySenderUsernameAndTargetUsernameAndShowId(String senderUsername, String targetUsername, int showId);

    // 🟢 ჩაამატე ეს ორი მეთოდი:
    @Modifying
    @Transactional
    @Query("UPDATE Recommendation r SET r.targetUsername = :newUsername WHERE r.targetUsername = :oldUsername")
    void updateTargetUsername(@Param("oldUsername") String oldUsername, @Param("newUsername") String newUsername);

    @Modifying
    @Transactional
    @Query("UPDATE Recommendation r SET r.senderUsername = :newUsername WHERE r.senderUsername = :oldUsername")
    void updateSenderUsername(@Param("oldUsername") String oldUsername, @Param("newUsername") String newUsername);
}
