
package com.cyberrange.backend.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard pagination response.
 *
 * @param <T> Type of content in the page.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious
) {

    /**
     * Creates a pagination response from Spring Data Page.
     *
     * @param source Spring Data Page containing the data.
     * @param <T> Type of content in the page.
     * @return standardized pagination response.
     */
    public static <T> PageResponse<T> from(Page<T> source) {
        return new PageResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast(),
                source.hasNext(),
                source.hasPrevious()
        );
    }
}
