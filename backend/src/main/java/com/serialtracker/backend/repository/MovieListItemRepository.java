package com.serialtracker.backend.repository;

import com.serialtracker.backend.entity.MovieListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieListItemRepository extends JpaRepository<MovieListItem, Long> {

    // All shows in one list, in the order the owner arranged them.
    List<MovieListItem> findByListIdOrderByPosition(Long listId);

    // Used to check "is this show already in this list?" before adding it,
    // and to find the row to delete when removing a show.
    Optional<MovieListItem> findByListIdAndShowId(Long listId, int showId);

    // Used when deleting an entire list, so its items don't become orphans.
    void deleteByListId(Long listId);

    // Used to know what position a newly-added show should get (append to end).
    int countByListId(Long listId);
}
