package com.lee.hotel;

import com.lee.hotel.handle.ElasticsearchHandle;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

/**
 * @author LiJing
 * @version 1.0
 */
@Slf4j
@SpringBootTest
public class HotelSearchTest {
    @Autowired
    private ElasticsearchHandle handle;

    // 查询所有 match_all
    @Test
    public void testMatchAll() {
        try {
            handle.matchAllQuery("hotel");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 全文检索查询 match
    @Test
    public void testMatch() {
        try {
            handle.matchQuery("hotel", "name", "如家");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 复合查询 bool
    @Test
    public void testBool() {
        try {
            handle.boolQuery("hotel", "北京", 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 排序和分页
    @Test
    public void testPageAndSort() {
        try {
            handle.pageAndSort("hotel", 1, 5);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 高亮
    @Test
    public void testHighlight() {
        try {
            handle.highlight("hotel", "all", "如家");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 聚合
    @Test
    public void testBucketAggregation() {
        try {
            handle.bucketAggregation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 自动补全
    @Test
    public void testSuggest() {
        try {
            handle.suggest("s");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
