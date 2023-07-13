package com.lee.hotel.handle;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.lee.hotel.exception.ExploException;
import com.lee.hotel.pojo.HotelDoc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author LiJing
 * @version 1.0
 * <p>
 * ElasticsearchHandle工具类
 * 主要是封装elasticsearch的索引、文档对应的一些增删改查方法
 */
@Slf4j
@Component
public class ElasticsearchHandle {

    @Autowired
    private ElasticsearchClient client;

    /**
     * 判断索引是否存在
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    public boolean hasIndex(String indexName) throws IOException {
        BooleanResponse response = client.indices().exists(d -> d.index(indexName));
        return response.value();
    }

    /**
     * 创建索引
     *
     * @param indexName
     * @param fileName
     * @return
     * @throws IOException
     */
    public boolean createIndex(String indexName, String fileName) throws IOException {
        InputStream input = this.getClass()
                .getResourceAsStream("/" + fileName);

        CreateIndexRequest req = CreateIndexRequest.of(b -> b
                .index(indexName)
                .withJson(input)
        );

        CreateIndexResponse response = client.indices().create(req);

        return response.acknowledged();
    }

    /**
     * 删除索引
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    public boolean deleteIndex(String indexName) throws IOException {
        DeleteIndexResponse response = client.indices().delete(d -> d.index(indexName));
        return response.acknowledged();
    }


    /**
     * 新增文档
     *
     * @param indexName
     * @param obj
     * @param id
     * @return
     */
    public IndexResponse insertDocument(String indexName, Object obj, String id) {
        IndexResponse indexResponse;
        try {
            indexResponse = client.index(i -> i
                    .index(indexName)
                    .id(id)
                    .document(obj));
        } catch (IOException e) {
            log.error("数据插入ES异常：{}", e.getMessage());
            throw new ExploException("ES新增数据失败");
        }
        return indexResponse;
    }

    /**
     * 查询文档
     *
     * @param indexName
     * @param id
     * @return
     */
    public GetResponse<HotelDoc> getDocument(String indexName, String id) {
        GetResponse<HotelDoc> getResponse;
        try {
            getResponse = client.get(g -> g
                            .index(indexName)
                            .id(id),
                    HotelDoc.class
            );
        } catch (IOException e) {
            log.error("查询ES异常：{}", e.getMessage());
            throw new ExploException("ES查询数据失败");
        }
        return getResponse;
    }

    /**
     * 删除文档
     *
     * @param indexName
     * @param id
     * @return
     */
    public DeleteResponse deleteDocument(String indexName, String id) {
        DeleteResponse deleteResponse;
        try {
            deleteResponse = client.delete(d -> d
                    .index(indexName)
                    .id(id)
            );
        } catch (IOException e) {
            log.error("删除ES数据异常：{}", e.getMessage());
            throw new ExploException("ES删除数据失败");
        }
        return deleteResponse;
    }

    /**
     * 全量修改文档
     *
     * @param indexName
     * @param obj
     * @param id
     * @return
     */
    public UpdateResponse<HotelDoc> updateDocument(String indexName, Object obj, String id) {
        UpdateResponse<HotelDoc> updateResponse;
        try {
            updateResponse = client.update(u -> u
                            .index(indexName)
                            .id(id)
                            .doc(obj),
                    HotelDoc.class
            );
        } catch (IOException e) {
            log.error("数据修改ES异常：{}", e.getMessage());
            throw new ExploException("ES修改数据失败");
        }
        return updateResponse;
    }

    /**
     * 查询所有 match_all
     *
     * @param indexName
     * @throws IOException
     */
    public void matchAllQuery(String indexName) throws IOException {
        SearchResponse<HotelDoc> response = client.search(s -> s
                        .index(indexName)
                        .query(q -> q
                                .matchAll(t -> t)
                        ),
                HotelDoc.class
        );

        handleResponse(response);
    }

    /**
     * 全文检索查询 match
     *
     * @param indexName
     * @param fieldName
     * @param searchText
     * @throws IOException
     */
    public void matchQuery(String indexName, String fieldName, String searchText) throws IOException {
        SearchResponse<HotelDoc> response = client.search(s -> s
                        .index(indexName)
                        .query(q -> q
                                .match(t -> t
                                        .field(fieldName)
                                        .query(searchText)
                                )
                        ),
                HotelDoc.class
        );

        handleResponse(response);
    }

    /**
     * 复合查询 bool
     *
     * @param indexName
     * @param cityName
     * @param minPrice
     * @throws IOException
     */
    public void boolQuery(String indexName, String cityName, Integer minPrice) throws IOException {
        // Search by city
        Query byCity = TermQuery.of(m -> m
                .field("city")
                .value(cityName)
        )._toQuery();

        // Search by price
        Query byPrice = RangeQuery.of(r -> r
                .field("price")
                .gte(JsonData.of(minPrice))
        )._toQuery();

        SearchResponse<HotelDoc> response = client.search(s -> s
                        .index(indexName)
                        .query(q -> q
                                .bool(b -> b
                                        .must(byCity)
                                        .filter(byPrice)
                                )
                        ),
                HotelDoc.class
        );

        handleResponse(response);
    }

    /**
     * 排序和分页
     *
     * @param indexName
     * @param page
     * @param size
     * @throws IOException
     */
    public void pageAndSort(String indexName, Integer page, Integer size) throws IOException {
        SearchResponse<HotelDoc> response = client.search(s -> s
                        .index(indexName)
                        .query(q -> q
                                .matchAll(t -> t)
                        )
                        .from((page - 1) * size)
                        .size(size)
                        .sort(t -> t
                                .field(f -> f
                                        .field("price")
                                        .order(SortOrder.Asc))
                        ),
                HotelDoc.class
        );

        handleResponse(response);
    }

    /**
     * 高亮
     *
     * @param indexName
     * @param fieldName
     * @param searchText
     * @throws IOException
     */
    public void highlight(String indexName, String fieldName, String searchText) throws IOException {
        SearchResponse<HotelDoc> response = client.search(s -> s
                        .index(indexName)
                        .query(q -> q
                                .match(t -> t
                                        .field(fieldName)
                                        .query(searchText)
                                )
                        )
                        .highlight(h -> h
                                .fields("name", f -> f
                                        .requireFieldMatch(false))),
                HotelDoc.class
        );

        handleResponse(response);
    }

    /**
     * 解析 SearchResponse
     *
     * @param response
     */
    private void handleResponse(SearchResponse<HotelDoc> response) {
        TotalHits total = response.hits().total();
        boolean isExactResult = total.relation() == TotalHitsRelation.Eq;

        if (isExactResult) {
            log.info("There are " + total.value() + " results");
        } else {
            log.info("There are more than " + total.value() + " results");
        }

        List<Hit<HotelDoc>> hits = response.hits().hits();
        for (Hit<HotelDoc> hit : hits) {
            HotelDoc hotelDoc = hit.source();
            // 获取高亮结果
            Map<String, List<String>> highlight = hit.highlight();
            if (!CollectionUtils.isEmpty(highlight)) {
                List<String> name = highlight.get("name");
                if (name != null) {
                    String s = name.get(0);
                    hotelDoc.setName(s);
                }
            }
            log.info("Found hotelDoc " + hotelDoc + ", score " + hit.score());
        }
    }

    /**
     * Bucket聚合
     *
     * @throws IOException
     */
    public void bucketAggregation() throws IOException {
        SearchResponse<Void> response = client.search(b -> b
                        .index("hotel")
                        .size(0)
                        .aggregations("brandAgg", a -> a
                                .terms(t -> t
                                        .field("brand")
                                        .size(20)
                                )
                        ),
                Void.class
        );

        List<StringTermsBucket> buckets = response.aggregations()
                .get("brandAgg")
                .sterms()
                .buckets().array();

        for (StringTermsBucket bucket : buckets) {
            log.info("There are " + bucket.docCount() +
                    " hotel brand " + bucket.key().stringValue());
        }
    }

    /**
     * 自动补全
     *
     * @param prefix
     * @throws IOException
     */
    public List<String> suggest(String prefix) throws IOException {
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();

        searchRequestBuilder.suggest(builder -> builder
                .suggesters("hotelSuggestion",
                        new FieldSuggester.Builder()
                                .prefix(prefix)
                                .completion(new CompletionSuggester.Builder()
                                        .field("suggestion")
                                        .skipDuplicates(true)
                                        .size(10)
                                        .build()
                                )
                                .build()
                )
        );

        SearchResponse<HotelDoc> response = client.search(searchRequestBuilder.build(), HotelDoc.class);

        Map<String, List<Suggestion<HotelDoc>>> suggest = response.suggest();
        List<Suggestion<HotelDoc>> hotelSuggestion = suggest.get("hotelSuggestion");
        List<CompletionSuggestOption<HotelDoc>> options = hotelSuggestion.get(0).completion().options();

        ArrayList<String> list = new ArrayList<>(options.size());
        for (CompletionSuggestOption<HotelDoc> option : options) {
            // 补全结果
            String text = option.text();
            list.add(text);
        }

        return list;
    }
}

