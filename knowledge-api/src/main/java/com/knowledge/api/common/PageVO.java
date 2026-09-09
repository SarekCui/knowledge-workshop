package com.knowledge.api.common;

import java.util.List;

public record PageVO<T>(List<T> items, int pageNo, int pageSize, long total) {

    public PageVO {
        items = List.copyOf(items);
    }
}
