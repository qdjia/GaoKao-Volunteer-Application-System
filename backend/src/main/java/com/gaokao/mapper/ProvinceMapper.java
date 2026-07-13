package com.gaokao.mapper;

import com.gaokao.entity.Province;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ProvinceMapper {

    @Select("SELECT * FROM province ORDER BY id")
    List<Province> findAll();

    @Select("SELECT * FROM province WHERE id = #{id}")
    Province findById(@Param("id") Long id);
}