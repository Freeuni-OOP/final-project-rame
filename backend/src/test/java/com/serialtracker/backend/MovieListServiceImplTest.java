package com.serialtracker.backend;

import com.serialtracker.backend.entity.MovieList;
import com.serialtracker.backend.entity.MovieListItem;
import com.serialtracker.backend.entity.User;
import com.serialtracker.backend.repository.MovieListItemRepository;
import com.serialtracker.backend.repository.MovieListRepository;
import com.serialtracker.backend.repository.UserRepository;
import com.serialtracker.backend.service.MovieListServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovieListServiceImplTest {

    private MovieListRepository listRepository;
    private MovieListItemRepository itemRepository;
    private UserRepository userRepository;
    private MovieListServiceImpl movieListService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        listRepository = mock(MovieListRepository.class);
        itemRepository = mock(MovieListItemRepository.class);
        userRepository = mock(UserRepository.class);
        movieListService = new MovieListServiceImpl(listRepository, itemRepository, userRepository);

        alice = new User("alice", "alice@mail.com", "hashedpass");
        alice.setId(1L);
        bob = new User("bob", "bob@mail.com", "hashedpass");
        bob.setId(2L);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
    }

    @Test
    void createList_success() {
        when(listRepository.save(any(MovieList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieList result = movieListService.createList("alice", "Best Sci-Fi", "some shows", true);

        assertEquals(1L, result.getOwnerId());
        assertEquals("Best Sci-Fi", result.getName());
        verify(listRepository, times(1)).save(any(MovieList.class));
    }

    @Test
    void createList_throwsException_whenNameIsBlank() {
        assertThrows(RuntimeException.class, () -> movieListService.createList("alice", "   ", null, true));
        verify(listRepository, never()).save(any());
    }

    @Test
    void renameList_success() {
        MovieList list = new MovieList(1L, "Old Name", "old desc", true);
        list.setId(5L);
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));
        when(listRepository.save(any(MovieList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieList result = movieListService.renameList(5L, "alice", "New Name", "new desc");

        assertEquals("New Name", result.getName());
        assertEquals("new desc", result.getDescription());
    }

    @Test
    void renameList_throwsException_whenActingUserIsNotOwner() {
        MovieList list = new MovieList(1L, "Old Name", "old desc", true);
        list.setId(5L);
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));

        // bob is not the owner (alice, id 1, is) - should be rejected
        assertThrows(RuntimeException.class, () -> movieListService.renameList(5L, "bob", "New Name", "new desc"));
        verify(listRepository, never()).save(any());
    }

    @Test
    void setVisibility_success() {
        MovieList list = new MovieList(1L, "List", "desc", true);
        list.setId(5L);
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));
        when(listRepository.save(any(MovieList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieList result = movieListService.setVisibility(5L, "alice", false);

        assertFalse(result.isPublic());
    }

    @Test
    void setVisibility_throwsException_whenActingUserIsNotOwner() {
        MovieList list = new MovieList(1L, "List", "desc", true);
        list.setId(5L);
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));

        assertThrows(RuntimeException.class, () -> movieListService.setVisibility(5L, "bob", false));
        verify(listRepository, never()).save(any());
    }

    @Test
    void deleteList_success() {
        MovieList list = new MovieList(1L, "List", "desc", true);
        list.setId(5L);
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));

        movieListService.deleteList(5L, "alice");

        verify(itemRepository, times(1)).deleteByListId(5L);
        verify(listRepository, times(1)).delete(list);
    }

    @Test
    void deleteList_throwsException_whenActingUserIsNotOwner() {
        MovieList list = new MovieList(1L, "List", "desc", true);
        list.setId(5L);
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));

        assertThrows(RuntimeException.class, () -> movieListService.deleteList(5L, "bob"));
        verify(listRepository, never()).delete(any());
    }

    @Test
    void addShow_success_appendsAtNextPosition() {
        MovieList list = new MovieList(1L, "List", "desc", true);
        list.setId(5L);
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));
        when(itemRepository.findByListIdAndShowId(5L, 100)).thenReturn(Optional.empty());
        when(itemRepository.countByListId(5L)).thenReturn(2);
        when(itemRepository.save(any(MovieListItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieListItem result = movieListService.addShow(5L, "alice", 100);

        assertEquals(100, result.getShowId());
        assertEquals(2, result.getPosition());
    }

    @Test
    void addShow_throwsException_whenShowAlreadyInList() {
        MovieList list = new MovieList(1L, "List", "desc", true);
        list.setId(5L);
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));
        when(itemRepository.findByListIdAndShowId(5L, 100))
                .thenReturn(Optional.of(new MovieListItem(5L, 100, 0)));

        assertThrows(RuntimeException.class, () -> movieListService.addShow(5L, "alice", 100));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void addShow_throwsException_whenActingUserIsNotOwner() {
        MovieList list = new MovieList(1L, "List", "desc", true);
        list.setId(5L);
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));

        assertThrows(RuntimeException.class, () -> movieListService.addShow(5L, "bob", 100));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void removeShow_success() {
        MovieList list = new MovieList(1L, "List", "desc", true);
        list.setId(5L);
        MovieListItem item = new MovieListItem(5L, 100, 0);
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));
        when(itemRepository.findByListIdAndShowId(5L, 100)).thenReturn(Optional.of(item));

        movieListService.removeShow(5L, "alice", 100);

        verify(itemRepository, times(1)).delete(item);
    }

    @Test
    void removeShow_throwsException_whenShowNotInList() {
        MovieList list = new MovieList(1L, "List", "desc", true);
        list.setId(5L);
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));
        when(itemRepository.findByListIdAndShowId(5L, 100)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> movieListService.removeShow(5L, "alice", 100));
        verify(itemRepository, never()).delete(any());
    }

    @Test
    void reorderShows_success_rewritesPositions() {
        MovieList list = new MovieList(1L, "List", "desc", true);
        list.setId(5L);
        MovieListItem itemA = new MovieListItem(5L, 100, 0);
        MovieListItem itemB = new MovieListItem(5L, 200, 1);
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));
        when(itemRepository.findByListIdOrderByPosition(5L)).thenReturn(List.of(itemA, itemB));

        movieListService.reorderShows(5L, "alice", List.of(200, 100));

        assertEquals(1, itemA.getPosition());
        assertEquals(0, itemB.getPosition());
        verify(itemRepository, times(1)).saveAll(List.of(itemA, itemB));
    }

    @Test
    void reorderShows_throwsException_whenSizeMismatch() {
        MovieList list = new MovieList(1L, "List", "desc", true);
        list.setId(5L);
        MovieListItem itemA = new MovieListItem(5L, 100, 0);
        when(listRepository.findById(5L)).thenReturn(Optional.of(list));
        when(itemRepository.findByListIdOrderByPosition(5L)).thenReturn(List.of(itemA));

        assertThrows(RuntimeException.class,
                () -> movieListService.reorderShows(5L, "alice", List.of(100, 200)));
        verify(itemRepository, never()).saveAll(any());
    }

    @Test
    void getListsOwnedBy_returnsOnlyThatUsersLists() {
        MovieList aliceList = new MovieList(1L, "Alice's List", "desc", true);
        when(listRepository.findByOwnerId(1L)).thenReturn(List.of(aliceList));

        List<MovieList> result = movieListService.getListsOwnedBy("alice");

        assertEquals(1, result.size());
        assertEquals("Alice's List", result.get(0).getName());
    }
}
