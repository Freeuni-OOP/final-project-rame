package com.serialtracker.backend.service;

import com.serialtracker.backend.entity.MovieList;
import com.serialtracker.backend.entity.MovieListItem;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.repository.MovieListItemRepository;
import com.serialtracker.backend.repository.MovieListRepository;
import com.serialtracker.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class MovieListServiceImpl implements MovieListService {

    private final MovieListRepository listRepository;
    private final MovieListItemRepository itemRepository;
    private final UserRepository userRepository;

    public MovieListServiceImpl(MovieListRepository listRepository,
                                 MovieListItemRepository itemRepository,
                                 UserRepository userRepository) {
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @Override
    public MovieList createList(String ownerUsername, String name, String description, boolean isPublic) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("List name cannot be empty.");
        }

        User owner = getUserOrThrow(ownerUsername);
        MovieList list = new MovieList(owner.getId(), name.trim(), description, isPublic);
        return listRepository.save(list);
    }

    @Override
    public MovieList renameList(Long listId, String actingUsername, String newName, String newDescription) {
        MovieList list = getListOrThrow(listId);
        requireOwner(list, actingUsername);

        if (newName == null || newName.trim().isEmpty()) {
            throw new RuntimeException("List name cannot be empty.");
        }

        list.setName(newName.trim());
        list.setDescription(newDescription);
        return listRepository.save(list);
    }

    @Override
    public MovieList setVisibility(Long listId, String actingUsername, boolean isPublic) {
        MovieList list = getListOrThrow(listId);
        requireOwner(list, actingUsername);

        list.setPublic(isPublic);
        return listRepository.save(list);
    }

    @Override
    public void deleteList(Long listId, String actingUsername) {
        MovieList list = getListOrThrow(listId);
        requireOwner(list, actingUsername);

        // Delete the items first so we never leave orphaned rows pointing
        // at a list that no longer exists.
        itemRepository.deleteByListId(listId);
        listRepository.delete(list);
    }

    @Override
    public MovieListItem addShow(Long listId, String actingUsername, int showId) {
        MovieList list = getListOrThrow(listId);
        requireOwner(list, actingUsername);

        if (itemRepository.findByListIdAndShowId(listId, showId).isPresent()) {
            throw new RuntimeException("This show is already in the list.");
        }

        int nextPosition = itemRepository.countByListId(listId);
        MovieListItem item = new MovieListItem(listId, showId, nextPosition);
        return itemRepository.save(item);
    }

    @Override
    public void removeShow(Long listId, String actingUsername, int showId) {
        MovieList list = getListOrThrow(listId);
        requireOwner(list, actingUsername);

        MovieListItem item = itemRepository.findByListIdAndShowId(listId, showId)
                .orElseThrow(() -> new RuntimeException("This show is not in the list."));
        itemRepository.delete(item);
    }

    @Override
    public void reorderShows(Long listId, String actingUsername, List<Integer> orderedShowIds) {
        MovieList list = getListOrThrow(listId);
        requireOwner(list, actingUsername);

        List<MovieListItem> items = itemRepository.findByListIdOrderByPosition(listId);
        if (orderedShowIds.size() != items.size()) {
            throw new RuntimeException("The new order must include every show currently in the list, exactly once.");
        }

        Map<Integer, MovieListItem> byShowId = items.stream()
                .collect(Collectors.toMap(MovieListItem::getShowId, i -> i));

        for (int position = 0; position < orderedShowIds.size(); position++) {
            int showId = orderedShowIds.get(position);
            MovieListItem item = byShowId.get(showId);
            if (item == null) {
                throw new RuntimeException("Show " + showId + " is not in this list.");
            }
            item.setPosition(position);
        }

        itemRepository.saveAll(items);
    }

    @Override
    public List<MovieList> getListsOwnedBy(String ownerUsername) {
        User owner = getUserOrThrow(ownerUsername);
        return listRepository.findByOwnerId(owner.getId());
    }

    @Override
    public List<MovieList> getRecentPublicLists() {
        return listRepository.findTop8ByIsPublicTrueOrderByCreatedAtDesc();
    }

    @Override
    public List<MovieList> getAllPublicLists() {
        return listRepository.findByIsPublicTrueOrderByCreatedAtDesc();
    }

    @Override
    public List<MovieList> getPopularPublicLists() {
        return listRepository.findByIsPublicTrueOrderByCreatedAtDesc().stream()
                .sorted((a, b) -> itemRepository.countByListId(b.getId()) - itemRepository.countByListId(a.getId()))
                .limit(8)
                .toList();
    }

    @Override
    public MovieList getList(Long listId) {
        return getListOrThrow(listId);
    }

    @Override
    public List<MovieListItem> getItems(Long listId) {
        return itemRepository.findByListIdOrderByPosition(listId);
    }

    private User getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private MovieList getListOrThrow(Long listId) {
        return listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found."));
    }

    private void requireOwner(MovieList list, String actingUsername) {
        User actingUser = getUserOrThrow(actingUsername);
        if (!list.getOwnerId().equals(actingUser.getId())) {
            throw new RuntimeException("Only the owner of this list can do that.");
        }
    }
}
