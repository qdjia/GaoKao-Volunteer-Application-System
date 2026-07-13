package com.gaokao.mapper;

import com.gaokao.entity.MajorCourse;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MajorCourseMapper {

    @Select("SELECT * FROM major_course WHERE major_id = #{majorId}")
    List<MajorCourse> findByMajorId(@Param("majorId") Long majorId);

    @Insert("INSERT INTO major_course(major_id, name) VALUES(#{majorId}, #{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MajorCourse course);

    @Delete("DELETE FROM major_course WHERE major_id = #{majorId}")
    int deleteByMajorId(@Param("majorId") Long majorId);
}