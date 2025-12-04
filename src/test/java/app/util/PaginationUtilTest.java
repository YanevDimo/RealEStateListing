package app.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class PaginationUtilTest {

    @Test
    void testPaginateList_FirstPage() {

        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        Pageable pageable = PageRequest.of(0, 2); // First page, 2 items per page

        // When
        Page<String> result = PaginationUtil.paginateList(list, pageable);

        // Then - verify results
        assertEquals(2, result.getContent().size(), "Should have 2 items on first page");
        assertEquals("A", result.getContent().get(0), "First item should be A");
        assertEquals("B", result.getContent().get(1), "Second item should be B");
        assertEquals(5, result.getTotalElements(), "Total elements should be 5");
        assertEquals(3, result.getTotalPages(), "Should have 3 pages total");
        assertTrue(result.hasContent(), "Page should have content");
        assertTrue(result.hasNext(), "Should have next page");
        assertFalse(result.hasPrevious(), "First page should not have previous");
    }

    @Test
    void testPaginateList_MiddlePage() {
        // Given
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        Pageable pageable = PageRequest.of(1, 2); // Second page (index 1), 2 items per page

        // When
        Page<String> result = PaginationUtil.paginateList(list, pageable);

        // Then
        assertEquals(2, result.getContent().size(), "Should have 2 items on middle page");
        assertEquals("C", result.getContent().get(0), "First item should be C");
        assertEquals("D", result.getContent().get(1), "Second item should be D");
        assertTrue(result.hasPrevious(), "Middle page should have previous");
        assertTrue(result.hasNext(), "Middle page should have next");
    }

    @Test
    void testPaginateList_LastPage() {
        // Given
        List<String> list = Arrays.asList("A", "B", "C", "D", "E");
        Pageable pageable = PageRequest.of(2, 2); // Third page (index 2), 2 items per page

        // When
        Page<String> result = PaginationUtil.paginateList(list, pageable);

        // Then
        assertEquals(1, result.getContent().size(), "Last page should have 1 item");
        assertEquals("E", result.getContent().get(0), "Last item should be E");
        assertTrue(result.hasPrevious(), "Last page should have previous");
        assertFalse(result.hasNext(), "Last page should not have next");
    }

    @Test
    void testPaginateList_EmptyList() {
        // Given
        List<String> list = Collections.emptyList();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<String> result = PaginationUtil.paginateList(list, pageable);

        // Then
        assertTrue(result.getContent().isEmpty(), "Empty list should return empty page");
        assertEquals(0, result.getTotalElements(), "Total elements should be 0");
        assertEquals(0, result.getTotalPages(), "Should have 0 pages");
        assertFalse(result.hasContent(), "Empty page should not have content");
    }

    @Test
    void testPaginateList_PageBeyondListSize() {
        // Given - request page 5 but list only has 3 items
        List<String> list = Arrays.asList("A", "B", "C");
        Pageable pageable = PageRequest.of(5, 10); // Page 5, 10 items per page

        // When
        Page<String> result = PaginationUtil.paginateList(list, pageable);

        // Then
        assertTrue(result.getContent().isEmpty(), "Page beyond list size should be empty");
        assertEquals(3, result.getTotalElements(), "Total elements should still be 3");
        assertFalse(result.hasContent(), "Should not have content");
    }

    @Test
    void testPaginateList_SingleItem() {
        // Given - list with one item
        List<String> list = Collections.singletonList("A");
        Pageable pageable = PageRequest.of(0, 1);

        // When
        Page<String> result = PaginationUtil.paginateList(list, pageable);

        // Then
        assertEquals(1, result.getContent().size(), "Should have 1 item");
        assertEquals("A", result.getContent().get(0), "Item should be A");
        assertEquals(1, result.getTotalElements(), "Total should be 1");
        assertEquals(1, result.getTotalPages(), "Should have 1 page");
    }
}








