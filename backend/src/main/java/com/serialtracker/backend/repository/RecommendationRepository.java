package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    // პროფილისთვის: ყველა მიღებული რეკომენდაცია ახლიდან ძველისკენ
    List<Recommendation> findByTargetUsernameOrderByCreatedAtDesc(String targetUsername);

    // ჰედერის ნოტიფიკაციებისთვის: მხოლოდ წაუკითხავი რექვესტები
    List<Recommendation> findByTargetUsernameAndIsReadFalse(String targetUsername);
}