package com.gaokao.mapper;

import com.gaokao.entity.ClassInfo;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ClassInfoMapper {

    @Select("<script>" +
            "SELECT c.*, p.name as province_name, " +
            "(SELECT COUNT(*) FROM student s WHERE s.class_id = c.id) as student_count " +
            "FROM class_info c LEFT JOIN province p ON c.province_id = p.id " +
            "<where>" +
            "<if test='name != null'> AND c.name LIKE CONCAT('%',#{name},'%')</if>" +
            "</where>" +
            " ORDER BY c.id" +
            "</script>")
    List<ClassInfo> findList(@Param("name") String name);

    @Select("SELECT c.*, p.name as province_name, " +
            "(SELECT COUNT(*) FROM student s WHERE s.class_id = c.id) as student_count " +
            "FROM class_info c LEFT JOIN province p ON c.province_id = p.id WHERE c.id = #{id}")
    ClassInfo findById(@Param("id") Long id);

    @Insert("INSERT INTO class_info(name, grade, teacher, province_id) VALUES(#{name}, #{grade}, #{teacher}, #{provinceId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ClassInfo classInfo);

    @Update("UPDATE class_info SET name=#{name}, grade=#{grade}, teacher=#{teacher}, province_id=#{provinceId} WHERE id=#{id}")
    int update(ClassInfo classInfo);

    @Delete("DELETE FROM class_info WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT c.*, p.name as province_name FROM class_info c LEFT JOIN province p ON c.province_id = p.id ORDER BY c.id")
    List<ClassInfo> findAll();
}