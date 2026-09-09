package com.knowledge.common.converter;

import com.knowledge.api.common.PageVO;
import com.knowledge.common.model.PageBO;
import java.util.function.Function;

public final class PageConverter {

    private PageConverter() {
    }

    public static <S, T> PageVO<T> toVO(PageBO<S> source, Function<S, T> itemConverter) {
        return new PageVO<>(source.items().stream().map(itemConverter).toList(),
                source.pageNo(), source.pageSize(), source.total());
    }
}
