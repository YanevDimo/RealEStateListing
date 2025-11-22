package app.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;


public class PaginationUtil {

    public static <T> Page<T> paginateList(List<T> list, Pageable pageable) {
        // Calculate where to start (which item index)
        int start = (int) pageable.getOffset();
        
        // Calculate where to end (which item index)
        // use Math.min to make sure don't go past the end of the list
        int end = Math.min(start + pageable.getPageSize(), list.size());
        
        // Make sure start is not beyond the list size
        if (start >= list.size()) {
            // If asking for a page that doesn't exist, return an empty page
            return new PageImpl<>(List.of(), pageable, list.size());
        }
        // Extract just the items for this page (a "sub-list")
        List<T> pageContent = list.subList(start, end);
        // The PageImpl constructor needs: the items, pagination info, and total count
        return new PageImpl<>(pageContent, pageable, list.size());
    }
}

