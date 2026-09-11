package com.cq.maintenance.common.response;

import java.util.List;

public record PageResult<T>(List<T> records, long total, long page, long size) {

    public PageResult {
        records = List.copyOf(records);
    }

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        return new PageResult<>(records, total, page, size);
    }
}

