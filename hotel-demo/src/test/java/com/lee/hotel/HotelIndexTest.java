package com.lee.hotel;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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
public class HotelIndexTest {
    @Autowired
    private ElasticsearchClient client;
    @Autowired
    private ElasticsearchHandle handle;

    @Test
    public void testClient() {
        log.info("esClient：{}", client);
    }

    //判断索引是否存在
    @Test
    public void testHasIndex() {
        try {
            boolean res = handle.hasIndex("hotel");
            log.info("该索引是否存在：{}", res);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //创建索引
    @Test
    public void testCreateIndex() {
        try {
            boolean created = handle.createIndex("hotel", "hotel-index.json");
            log.info("索引是否创建成功：{}", created);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //删除索引
    @Test
    public void testDeleteIndex() {
        try {
            boolean deleted = handle.deleteIndex("hotel");
            log.info("索引是否删除成功：{}", deleted);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
