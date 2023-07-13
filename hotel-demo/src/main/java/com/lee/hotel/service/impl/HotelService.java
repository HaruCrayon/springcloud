package com.lee.hotel.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.json.JsonData;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lee.hotel.handle.ElasticsearchHandle;
import com.lee.hotel.mapper.HotelMapper;
import com.lee.hotel.pojo.Hotel;
import com.lee.hotel.pojo.HotelDoc;
import com.lee.hotel.pojo.PageResult;
import com.lee.hotel.pojo.RequestParams;
import com.lee.hotel.service.IHotelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {
    @Autowired
    private ElasticsearchClient client;
    @Autowired
    private ElasticsearchHandle handle;


    @Override
    public PageResult search(RequestParams params) {
        Integer page = params.getPage();
        Integer size = params.getSize();
        String location = params.getLocation();

        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();

        FunctionScoreQuery functionScoreQuery = buildBasicQuery(params);

        searchRequestBuilder.index("hotel")
                .query(q -> q.functionScore(functionScoreQuery))
                .from((page - 1) * size)
                .size(size);

        // 排序
        if (StringUtils.hasLength(location)) {
            GeoDistanceSort.Builder geoDistanceSortBuilder = new GeoDistanceSort.Builder();
            geoDistanceSortBuilder.field("location")
                    .location(l -> l.text(location))
                    .order(SortOrder.Asc)
                    .unit(DistanceUnit.Kilometers);
            searchRequestBuilder.sort(t -> t.geoDistance(geoDistanceSortBuilder.build()));
        }

        try {
            SearchResponse<HotelDoc> response = client.search(searchRequestBuilder.build(), HotelDoc.class);
            // 解析响应
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public Map<String, List<String>> filters(RequestParams params) {
        FunctionScoreQuery functionScoreQuery = buildBasicQuery(params);
        Map<String, Aggregation> aggregationMap = buildAggregation();

        SearchResponse<Void> response = null;
        try {
            response = client.search(b -> b
                            .index("hotel")
                            .query(q -> q.functionScore(functionScoreQuery))
                            .size(0)
                            .aggregations(aggregationMap),
                    Void.class
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, List<String>> result = new HashMap<>();

        Map<String, Aggregate> aggregations = response.aggregations();

        List<String> brandList = getAggByName(aggregations, "brandAgg");
        result.put("brand", brandList);
        List<String> cityList = getAggByName(aggregations, "cityAgg");
        result.put("city", cityList);
        List<String> starNameList = getAggByName(aggregations, "starNameAgg");
        result.put("starName", starNameList);

        return result;
    }


    @Override
    public List<String> getSuggestions(String prefix) {
        try {
            return handle.suggest(prefix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void insertById(Long id) {
        Hotel hotel = getById(id);
        HotelDoc hotelDoc = new HotelDoc(hotel);

        IndexResponse indexResponse = handle.insertDocument("hotel", hotelDoc, hotelDoc.getId().toString());
        Result result = indexResponse.result();
        log.info("result：{}", result);
    }


    @Override
    public void deleteById(Long id) {
        DeleteResponse deleteResponse = handle.deleteDocument("hotel", id.toString());
        Result result = deleteResponse.result();
        log.info("result：{}", result);
    }


    // 根据聚合名称获取聚合结果
    private List<String> getAggByName(Map<String, Aggregate> aggregations, String aggName) {
        List<StringTermsBucket> buckets = aggregations.get(aggName).sterms().buckets().array();

        List<String> keyList = new ArrayList<>();

        for (StringTermsBucket bucket : buckets) {
            String key = bucket.key().stringValue();
            keyList.add(key);
        }

        return keyList;
    }


    private Map<String, Aggregation> buildAggregation() {
        String[] filterFields = {"brand", "city", "starName"};
        Map<String, Aggregation> aggregationMap = new HashMap<>();
        for (String filterField : filterFields) {
            aggregationMap.put(filterField + "Agg",
                    new Aggregation.Builder().terms(t -> t.field(filterField).size(100)).build());
        }

        return aggregationMap;
    }


    private FunctionScoreQuery buildBasicQuery(RequestParams params) {
        String key = params.getKey();
        String city = params.getCity();
        String brand = params.getBrand();
        String starName = params.getStarName();
        Integer maxPrice = params.getMaxPrice();
        Integer minPrice = params.getMinPrice();

        // 1.原始查询，根据相关性算分
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // 关键字搜索 Search by "all"
        Query byAll;
        if (!StringUtils.hasLength(key)) {
            byAll = MatchAllQuery.of(m -> m)._toQuery();
        } else {
            byAll = MatchQuery.of(m -> m
                    .field("all")
                    .query(key)
            )._toQuery();
        }
        boolQueryBuilder.must(byAll);

        // Search by city
        if (StringUtils.hasLength(city)) {
            Query byCity = TermQuery.of(m -> m
                    .field("city")
                    .value(city)
            )._toQuery();
            boolQueryBuilder.filter(byCity);
        }

        // Search by brand
        if (StringUtils.hasLength(brand)) {
            Query byBrand = TermQuery.of(m -> m
                    .field("brand")
                    .value(brand)
            )._toQuery();
            boolQueryBuilder.filter(byBrand);
        }

        // Search by starName
        if (StringUtils.hasLength(starName)) {
            Query byStarName = TermQuery.of(m -> m
                    .field("starName")
                    .value(starName)
            )._toQuery();
            boolQueryBuilder.filter(byStarName);
        }

        // Search by price
        if (maxPrice != null && minPrice != null) {
            Query byPrice = RangeQuery.of(r -> r
                    .field("price")
                    .gte(JsonData.of(minPrice))
                    .lte(JsonData.of(maxPrice))
            )._toQuery();
            boolQueryBuilder.filter(byPrice);
        }

        Query query = boolQueryBuilder.build()._toQuery();

        // 2. function_score query 算分控制
        FunctionScoreQuery.Builder functionScoreQueryBuilder = new FunctionScoreQuery.Builder();

        functionScoreQueryBuilder.query(query)
                .functions(f -> f
                        .filter(q -> q
                                .term(t -> t
                                        .field("isAD")
                                        .value(true)
                                )
                        )
                        .weight(10.0)
                )
                .boostMode(FunctionBoostMode.Multiply);

        return functionScoreQueryBuilder.build();
    }


    private PageResult handleResponse(SearchResponse<HotelDoc> response) {
        //获取总条数
        TotalHits total = response.hits().total();
        long totalValue = total.value();

        List<HotelDoc> hotels = new ArrayList<>();
        List<Hit<HotelDoc>> hits = response.hits().hits();
        for (Hit<HotelDoc> hit : hits) {
            HotelDoc hotelDoc = hit.source();
            //获取排序值
            List<FieldValue> sortValues = hit.sort();
            if (sortValues.size() > 0) {
                FieldValue fieldValue = sortValues.get(0);
                Object sortValue = fieldValue._get();
                hotelDoc.setDistance(sortValue);
            }
            hotels.add(hotelDoc);
        }

        return new PageResult(totalValue, hotels);
    }
}
