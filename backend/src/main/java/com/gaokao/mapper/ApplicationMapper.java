package com.gaokao.mapper;

import com.gaokao.entity.Application;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ApplicationMapper {

    @Select("SELECT a.*, u.name as university_name, s.name as student_name " +
            "FROM application a " +
            "LEFT JOIN university u ON a.university_id = u.id " +
            "LEFT JOIN student s ON a.student_id = s.id " +
            "WHERE a.student_id = #{studentId} ORDER BY a.priority")
    List<Application> findByStudentId(@Param("studentId") Long studentId);

    @Select("SELECT a.*, u.name as university_name, s.name as student_name " +
            "FROM application a " +
            "LEFT JOIN university u ON a.university_id = u.id " +
            "LEFT JOIN student s ON a.student_id = s.id " +
            "WHERE a.student_id = #{studentId} AND a.status = #{status} ORDER BY a.priority")
    List<Application> findByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") String status);

    @Insert("INSERT INTO application(student_id, university_id, priority, accept_adjust, status) " +
            "VALUES(#{studentId}, #{universityId}, #{priority}, #{acceptAdjust}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Application application);

    @Delete("DELETE FROM application WHERE student_id = #{studentId}")
    int deleteByStudentId(@Param("studentId") Long studentId);

    @Update("UPDATE application SET status = #{status} WHERE student_id = #{studentId}")
    int updateStatusByStudentId(@Param("studentId") Long studentId, @Param("status") String status);
}