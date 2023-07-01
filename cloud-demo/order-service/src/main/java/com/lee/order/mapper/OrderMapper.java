package com.lee.order.mapper;

import com.lee.order.pojo.Order;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderMapper {

    @Select("select * from tb_order where id = #{id}")
    Order findById(Long id);
}
