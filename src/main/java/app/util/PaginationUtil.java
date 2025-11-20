package app.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Utility class for pagination operations.
 * 
 * WHY THIS UTILITY CLASS?
 * Before: We had the same pagination code repeated 4 times in different services:
 *   int start = (int) pageable.getOffset();
 *   int end = Math.min(start + pageable.getPageSize(), list.size());
 *   List<Entity> pageContent = list.subList(start, end);
 *   return new PageImpl<>(pageContent, pageable, list.size());
 * 
 * After: We call this utility method instead, which:
 *   1. Makes the code shorter and easier to read
 *   2. If we need to change how pagination works, we only change it in one place
 *   3. Reduces the chance of bugs from having different versions of the same logic
 *   4. Makes it easier to test pagination logic separately
 * 
 * This is a beginner-friendly utility class that helps avoid code duplication.
 */
public class PaginationUtil {

    /**
     * Converts a list into a paginated Page object.
     * 
     * HOW IT WORKS:
     * 1. It calculates which items from the list should be on the current page
     * 2. It extracts just those items (a "sub-list")
     * 3. It wraps them in a Page object that Spring can use
     * 
     * EXAMPLE:
     * If you have a list of 100 items and want page 2 with 10 items per page:
     * - start = 10 (skip the first 10 items)
     * - end = 20 (take items 10-19)
     * - Returns a Page with items 10-19, but Spring knows there are 100 total items
     * 
     * @param <T> The type of items in the list (could be User, Agent, PropertyDto, etc.)
     * @param list The full list of items to paginate
     * @param pageable Contains information about which page to show and how many items per page
     * @return A Page object containing only the items for the current page
     */
    public static <T> Page<T> paginateList(List<T> list, Pageable pageable) {
        // Calculate where to start (which item index)
        int start = (int) pageable.getOffset();
        
        // Calculate where to end (which item index)
        // We use Math.min to make sure we don't go past the end of the list
        int end = Math.min(start + pageable.getPageSize(), list.size());
        
        // Make sure start is not beyond the list size
        if (start >= list.size()) {
            // If we're asking for a page that doesn't exist, return an empty page
            return new PageImpl<>(List.of(), pageable, list.size());
        }
        
        // Extract just the items for this page (a "sub-list")
        List<T> pageContent = list.subList(start, end);
        
        // Wrap it in a Page object that Spring understands
        // The PageImpl constructor needs: the items, pagination info, and total count
        return new PageImpl<>(pageContent, pageable, list.size());
    }
}

