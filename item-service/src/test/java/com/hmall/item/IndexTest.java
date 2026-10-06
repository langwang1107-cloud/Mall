package com.hmall.item;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.utils.CollUtils;
import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.bouncycastle.cert.ocsp.Req;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.ParsedStats;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class IndexTest {
    @Autowired
    private IItemService itemService;


    private RestHighLevelClient client;
    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.150.101:9200")
        ));
    }
    @Test
    void testConnect() {
        System.out.println(client);
    }
    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }

    @Test
    @SneakyThrows
    void testCreateIndex(){
        CreateIndexRequest req = new CreateIndexRequest("items");
        String json = "{\n" +
                "  \"mappings\": {\n" +
                "    \"properties\": {\n" +
                "      \"id\": {\n" +
                "        \"type\": \"keyword\"\n" +
                "      },\n" +
                "      \"name\":{\n" +
                "        \"type\": \"text\",\n" +
                "        \"analyzer\": \"ik_max_word\"\n" +
                "      },\n" +
                "      \"price\":{\n" +
                "        \"type\": \"integer\"\n" +
                "      },\n" +
                "      \"stock\":{\n" +
                "        \"type\": \"integer\"\n" +
                "      },\n" +
                "      \"image\":{\n" +
                "        \"type\": \"keyword\",\n" +
                "        \"index\": false\n" +
                "      },\n" +
                "      \"category\":{\n" +
                "        \"type\": \"keyword\"\n" +
                "      },\n" +
                "      \"brand\":{\n" +
                "        \"type\": \"keyword\"\n" +
                "      },\n" +
                "      \"sold\":{\n" +
                "        \"type\": \"integer\"\n" +
                "      },\n" +
                "      \"commentCount\":{\n" +
                "        \"type\": \"integer\"\n" +
                "      },\n" +
                "      \"isAD\":{\n" +
                "        \"type\": \"boolean\"\n" +
                "      },\n" +
                "      \"updateTime\":{\n" +
                "        \"type\": \"date\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        req.source(json, XContentType.JSON);
        client.indices().create(req, RequestOptions.DEFAULT);
    }

    @Test
    @SneakyThrows
    public void testDeleteIndex(){
        DeleteIndexRequest request = new DeleteIndexRequest("items");
        client.indices().delete(request, RequestOptions.DEFAULT);
    }

    @Test
    @SneakyThrows
    public void testGetIndex(){
        GetIndexRequest request = new GetIndexRequest("items");
        GetIndexResponse response = client.indices().get(request, RequestOptions.DEFAULT);
        System.out.println("response = " + response);
    }


    @Test
    @SneakyThrows
    public void testAdd(){
        Item item = itemService.getById(625465);
        ItemDTO dto = BeanUtil.toBean(item, ItemDTO.class);
        IndexRequest request = new IndexRequest("items");
        request.id(dto.getId().toString());
        request.source(JSONUtil.toJsonStr(dto),XContentType.JSON);
        client.index(request,RequestOptions.DEFAULT);
    }

    @Test
    @SneakyThrows
    public void testGet(){
        GetRequest request = new GetRequest("items");
        request.id("625465");
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        String json = response.getSourceAsString();
        ItemDTO dto = JSONUtil.toBean(json, ItemDTO.class);
        System.out.println("dto = " + dto);
    }

    @Test
    @SneakyThrows
    public void testDelete(){
        DeleteRequest request = new DeleteRequest("items");
        request.id("625465");
        client.delete(request,RequestOptions.DEFAULT);
    }

    @Test
    @SneakyThrows
    public void testUpdate(){
        UpdateRequest request = new UpdateRequest();
        request.id("625465");
        request.index("items");
        Map<String, Object> map = new HashMap<>();
        map.put("sold",99);
        map.put("brand","哇哈哈AD钙奶");
        request.doc(map);
        client.update(request,RequestOptions.DEFAULT);
    }

    @Test
    @SneakyThrows
    public void testBulk(){
        //1.先查出来（分页）
        int pageNo = 1;
        int pageSize =10000;
        while(true){
            log.info("开始批量存储，页码：{}",pageNo);

            Page<Item> page = itemService.lambdaQuery().page(new Page<>(pageNo, pageSize));
            List<Item> records = page.getRecords();
            if(CollUtils.isEmpty(records)){
                log.info("没查到数据，结束，页码：{}",pageNo);
                return;
            }
            //代码走到这说明，有数据
            //2.一页一个bulk插入
            BulkRequest request = new BulkRequest();
            for (Item record : records) {
                ItemDTO dto = BeanUtil.toBean(record, ItemDTO.class);
                request.add(new IndexRequest("items")
                        .id(record.getId()
                                .toString()).source(JSONUtil.toJsonStr(dto),XContentType.JSON));
            }
            client.bulk(request,RequestOptions.DEFAULT);
            pageNo++;
        }
    }

    @Test
    @SneakyThrows
    public void testSimpleSearch(){
        SearchRequest request = new SearchRequest("items");
        MatchAllQueryBuilder builder = QueryBuilders.matchAllQuery();
        request.source().query(builder);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            ItemDTO dto = JSONUtil.toBean(json, ItemDTO.class);
            System.out.println("dto = " + dto);
        }
    }


    @Test
    @SneakyThrows
    public void testBoolSearch(){
        SearchRequest request = new SearchRequest("items");
        BoolQueryBuilder builder = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("name", "小米手机"))
                .must(QueryBuilders.termQuery("sold", 0))
                .filter(QueryBuilders.termQuery("brand", "小米"))
                .filter(QueryBuilders.rangeQuery("price").gte(90000).lte(159900));
        request.source().query(builder)
                .sort("price", SortOrder.DESC)
                .from(0)
                .size(10)
                .highlighter(SearchSourceBuilder.highlight().field("name").preTags("<em>").postTags("</em>"));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            HighlightField highlightField = hit.getHighlightFields().get("name");
            String highlight = highlightField.getFragments()[0].toString();
            ItemDTO dto = JSONUtil.toBean(json, ItemDTO.class);
            dto.setName(highlight);
            System.out.println("dto = " + dto);
        }
    }


    @Test
    @SneakyThrows
    public void testAgg(){
        SearchRequest request = new SearchRequest("items");
        BoolQueryBuilder builder = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("name", "小米手机"))
                .must(QueryBuilders.termQuery("sold", 0))
                .filter(QueryBuilders.rangeQuery("price").gte(90000).lte(159900));
        request.source().query(builder)
                .size(0).aggregation(AggregationBuilders.terms("kunkun").field("brand").size(20)
                        .subAggregation(AggregationBuilders.stats("wangyuan").field("price")));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        ParsedStringTerms kunkun = response.getAggregations().get("kunkun");
        for (Terms.Bucket bucket : kunkun.getBuckets()) {
            String key = bucket.getKeyAsString();
            long docCount = bucket.getDocCount();
            log.info("品牌：{}，数量：{}",key,docCount);
            ParsedStats wangyuan = bucket.getAggregations().get("wangyuan");
            log.info("max:{},min:{},avg:{},sum:{}",wangyuan.getMax(),wangyuan.getMin(),wangyuan.getAvg(),wangyuan.getSum());
        }
    }
}
