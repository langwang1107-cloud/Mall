package search.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import search.domain.po.ItemDoc;
import search.domain.query.ItemPageQuery;
import search.domain.vo.PageVO;
import search.service.ISearchService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ISearchService searchService;

    @GetMapping("/list")
    public PageVO<ItemDoc> search(ItemPageQuery query) {
        return searchService.search(query);
    }

    @PostMapping("/filters")
    public Map<String, List<String>> getFilters(@RequestBody ItemPageQuery query) {
        return searchService.getFilters(query);
    }
}
