package com.dbsync.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated Response
 *
 * @author DB Sync Platform
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> items;
    private Pagination pagination;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination {
        private Integer page;
        private Integer pageSize;
        private Long total;
        private Integer totalPages;

        public static Pagination of(int page, int pageSize, long total) {
            int totalPages = (int) Math.ceil((double) total / pageSize);
            return new Pagination(page, pageSize, total, totalPages);
        }
    }

    public static <T> PageResponse<T> of(List<T> items, int page, int pageSize, long total) {
        return new PageResponse<>(items, Pagination.of(page, pageSize, total));
    }
}
