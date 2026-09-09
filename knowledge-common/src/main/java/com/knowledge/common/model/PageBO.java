package com.knowledge.common.model;

import java.util.List;

public record PageBO<T>(List<T> items, int pageNo, int pageSize, long total) {

    public PageBO {
        items = List.copyOf(items);
    }
}
