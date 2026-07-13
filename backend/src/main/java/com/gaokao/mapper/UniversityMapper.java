package com.gaokao.mapper;

import com.gaokao.entity.University;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UniversityMapper {

    @Select("<script>" +
            "SELECT u.*, p.name as province_name FROM university u " +
            "LEFT JOIN province p ON u.province_id = p.id " +
            "<where>" +
            "<if test='name != null'> AND u.name LIKE CONCAT('%',#{name},'%')</if>" +
            "<if test='type != null'> AND u.type = #{type}</if>" +
            "<if test='provinceId != null'> AND u.province_id = #{provinceId}</if>" +
            "</where>" +
            " ORDER BY u.id" +
            "</script>")
    List<University> findList(@Param("name") String name, @Param("type") String type,
                              @Param("provinceId") Long provinceId);

    @Select("SELECT u.*, p.name as province_name FROM university u " +
            "LEFT JOIN province p ON u.province_id = p.id WHERE u.id = #{id}")
    University findById(@Param("id") Long id);

    @Insert("INSERT INTO university(name, type, province_id, batch) VALUES(#{name}, #{type}, #{provinceId}, #{batch})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(University university);

    @Update("UPDATE university SET name=#{name}, type=#{type}, province_id=#{provinceId}, batch=#{batch} WHERE id=#{id}")
    int update(University university);

    @Delete("DELETE FROM university WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT u.*, p.name as province_name FROM university u LEFT JOIN province p ON u.province_id = p.id ORDER BY u.id")
    List<University> findAll();
}