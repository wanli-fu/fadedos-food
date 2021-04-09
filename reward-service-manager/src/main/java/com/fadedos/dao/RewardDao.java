package com.fadedos.dao;

import com.fadedos.po.RewardPO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface RewardDao {

    @Insert("INSERT INTO reward (order_id, amount, status, date) VALUES(#{orderId}, #{amount}, #{status}, #{date})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RewardPO rewardPO);
}
