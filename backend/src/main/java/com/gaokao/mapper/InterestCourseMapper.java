package com.gaokao.mapper;

import com.gaokao.entity.InterestCourse;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface InterestCourseMapper {

    @Select("SELECT * FROM interest_course WHERE student_id = #{studentId}")
    List<InterestCourse> findByStudentId(@Param("studentId") Long studentId);

    @Insert("INSERT INTO interest_course(student_id, name) VALUES(#{studentId}, #{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InterestCourse course);

    @Delete("DELETE FROM interest_course WHERE student_id = #{studentId}")
    int deleteByStudentId(@Param("studentId") Long studentId);
}