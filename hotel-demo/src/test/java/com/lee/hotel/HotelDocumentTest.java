package com.lee.hotel;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.lee.hotel.handle.ElasticsearchHandle;
import com.lee.hotel.pojo.Hotel;
import com.lee.hotel.pojo.HotelDoc;
import com.lee.hotel.service.IHotelService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

/**
 * @author LiJing
 * @version 1.0
 */
@Slf4j
@SpringBootTest
public class HotelDocumentTest {
    @Autowired
    private ElasticsearchClient client;
    @Autowired
    private ElasticsearchHandle handle;
    @Autowired
    private IHotelService hotelService;

    //新增文档
    @Test
    void testAddDocument() {
        Hotel hotel = hotelService.getById(36934L);
        HotelDoc hotelDoc = new HotelDoc(hotel);

        IndexResponse indexResponse = handle.insertDocument("hotel", hotelDoc, hotelDoc.getId().toString());
        Result result = indexResponse.result();
        log.info("result：{}", result);
    }

    //查询文档
    @Test
    void testGetDocument() {
        GetResponse<HotelDoc> getResponse = handle.getDocument("hotel", "36934");
        if (getResponse.found()) {
            HotelDoc hotelDoc = getResponse.source();
            log.info("hotelDoc：{}", hotelDoc);
        } else {
            log.info("HotelDoc not found");
        }
    }

    //删除文档
    @Test
    void testDeleteDocument() {
        DeleteResponse deleteResponse = handle.deleteDocument("hotel", "36934");
        Result result = deleteResponse.result();
        log.info("result：{}", result);
    }

    //全量修改文档
    @Test
    void testUpdateDocument() throws IOException {
        Hotel hotel = hotelService.getById(36934L);
        HotelDoc hotelDoc = new HotelDoc(hotel);
        hotelDoc.setPrice(500);//336->500
        hotelDoc.setStarName("三钻");//二 -> 三

        UpdateResponse<HotelDoc> updateResponse = handle.updateDocument("hotel", hotelDoc, hotelDoc.getId().toString());
        Result result = updateResponse.result();
        log.info("result：{}", result);
    }

    //批量新增文档
    @Test
    void testAddMultiDocuments() throws IOException {
        List<Hotel> hotels = hotelService.list();
        BulkRequest.Builder br = new BulkRequest.Builder();

        for (Hotel hotel : hotels) {
            HotelDoc hotelDoc = new HotelDoc(hotel);
            br.operations(op -> op
                    .index(idx -> idx
                            .index("hotel")
                            .id(hotelDoc.getId().toString())
                            .document(hotelDoc)
                    )
            );
        }

        BulkResponse bulkResponse = client.bulk(br.build());

        // Log errors
        if (bulkResponse.errors()) {
            log.error("Bulk had errors");
            for (BulkResponseItem item : bulkResponse.items()) {
                if (item.error() != null) {
                    log.error(item.error().reason());
                }
            }
        }
    }

}
