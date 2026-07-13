package com.gaokao.mapper;

import com.gaokao.entity.AdmissionLog;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AdmissionLogMapper {

    @Insert("INSERT INTO admission_log(student_id, university_id, major_id, action, detail) " +
            "VALUES(#{studentId}, #{universityId}, #{majorId}, #{action}, #{detail})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AdmissionLog log);

    @Select("SELECT al.*, s.name as student_name, u.name as university_name, m.name as major_name " +
            "FROM admission_log al " +
            "LEFT JOIN student s ON al.student_id = s.id " +
            "LEFT JOIN university u ON al.university_id = u.id " +
            "LEFT JOIN major m ON al.major_id = m.id " +
            "ORDER BY al.created_at DESC")
    List<AdmissionLog> findAll();

    @Delete("DELETE FROM admission_log")
    int deleteAll();
}