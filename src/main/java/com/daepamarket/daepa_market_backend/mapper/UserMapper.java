package com.daepamarket.daepa_market_backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Select("""
        SELECT u_id
        FROM user
        WHERE u_idx = #{userIdx}
        LIMIT 1
    """)
    String findLoginIdByIdx(@Param("userIdx") Long userIdx);
}