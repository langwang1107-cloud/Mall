package search.service;

import search.domain.po.ItemDoc;
import search.domain.query.ItemPageQuery;
import search.domain.vo.PageVO;

import java.util.List;
import java.util.Map;

public interface ISearchService {
    void saveItemById(Long itemId);

    void deleteItemById(Long itemId);

    PageVO<ItemDoc> search(ItemPageQuery query);

    Map<String, List<String>> getFilters(ItemPageQuery query);
}
