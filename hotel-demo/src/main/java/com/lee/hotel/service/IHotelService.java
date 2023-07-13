package com.lee.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lee.hotel.pojo.Hotel;
import com.lee.hotel.pojo.PageResult;
import com.lee.hotel.pojo.RequestParams;

import java.util.List;
import java.util.Map;

public interface IHotelService extends IService<Hotel> {
    /**
     * 搜索酒店数据
     *
     * @param params
     * @return
     */
    PageResult search(RequestParams params);


    /**
     * 获取品牌、城市、星级字段的聚合结果
     *
     * @return
     */
    Map<String, List<String>> filters(RequestParams params);


    /**
     * 实现搜索框自动补全
     *
     * @param prefix
     * @return
     */
    List<String> getSuggestions(String prefix);


    /**
     * 新增或修改酒店数据
     *
     * @param id
     */
    void insertById(Long id);


    /**
     * 删除酒店数据
     *
     * @param id
     */
    void deleteById(Long id);
}
