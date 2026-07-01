package com.serialtracker.backend.service;

import com.serialtracker.backend.entity.MovieList;
import com.serialtracker.backend.entity.MovieListItem;

import java.util.List;

public interface MovieListService {

    MovieList createList(String ownerUsername, String name, String description, boolean isPublic);

    MovieList renameList(Long listId, String actingUsername, String newName, String newDescription);

    MovieList setVisibility(Long listId, String actingUsername, boolean isPublic);

    void deleteList(Long listId, String actingUsername);

    MovieListItem addShow(Long listId, String actingUsername, int showId);

    void removeShow(Long listId, String actingUsername, int showId);

    void reorderShows(Long listId, String actingUsername, List<Integer> orderedShowIds);

    List<MovieList> getListsOwnedBy(String ownerUsername);

    // Newest public lists across all users, for the Lists page's
    // "Featured Lists" section.
    List<MovieList> getRecentPublicLists();

    // Every public list across all users, newest first - for the "View All"
    // page (no cap, unlike getRecentPublicLists).
    List<MovieList> getAllPublicLists();

    // Public lists with the most shows in them, for the Lists page's
    // "Popular This Week" section. There's no likes/view-tracking system
    // yet, so "popular" is approximated as "biggest". Capped at 8, same
    // reasoning as getRecentPublicLists.
    List<MovieList> getPopularPublicLists();

    MovieList getList(Long listId);

    List<MovieListItem> getItems(Long listId);
}
