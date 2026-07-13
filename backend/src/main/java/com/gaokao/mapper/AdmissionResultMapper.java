package com.gaokao.mapper;

import com.gaokao.entity.AdmissionResult;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AdmissionResultMapper {

    @Select("<script>" +
            "SELECT ar.*, s.name as student_name, s.student_no as student_no, " +
            "u.name as university_name, m.name as major_name, c.name as class_name " +
            "FROM admission_result ar " +
            "LEFT JOIN student s ON ar.student_id = s.id " +
            "LEFT JOIN university u ON ar.university_id = u.id " +
            "LEFT JOIN major m ON ar.major_id = m.id " +
            "LEFT JOIN class_info c ON s.class_id = c.id " +
            "<where>" +
            "<if test='universityId != null'> AND ar.university_id = #{universityId}</if>" +
            "<if test='studentId != null'> AND ar.student_id = #{studentId}</if>" +
            "<if test='status != null'> AND ar.status = #{status}</if>" +
            "<if test='classId != null'> AND s.class_id = #{classId}</if>" +
            "</where>" +
            " ORDER BY s.total_score DESC" +
            "</script>")
    List<AdmissionResult> findList(@Param("universityId") Long universityId, @Param("studentId") Long studentId,
                                   @Param("status") String status, @Param("classId") Long classId);

    @Select("SELECT ar.*, s.name as student_name, s.student_no as student_no, " +
            "u.name as university_name, m.name as major_name " +
            "FROM admission_result ar " +
            "LEFT JOIN student s ON ar.student_id = s.id " +
            "LEFT JOIN university u ON ar.university_id = u.id " +
            "LEFT JOIN major m ON ar.major_id = m.id " +
            "WHERE ar.student_id = #{studentId}")
    AdmissionResult findByStudentId(@Param("studentId") Long studentId);

    @Insert("INSERT INTO admission_result(student_id, university_id, major_id, status, application_priority, is_adjusted, reason) " +
            "VALUES(#{studentId}, #{universityId}, #{majorId}, #{status}, #{applicationPriority}, #{isAdjusted}, #{reason})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AdmissionResult result);

    @Delete("DELETE FROM admission_result")
    int deleteAll();

    @Select("SELECT COUNT(*) FROM admission_result WHERE status = 'ADMITTED'")
    Long countAdmitted();

    @Select("SELECT COUNT(*) FROM admission_result WHERE status = 'UNADMITTED'")
    Long countUnadmitted();

    @Select("SELECT COUNT(*) FROM admission_result WHERE university_id = #{universityId} AND status = 'ADMITTED'")
    Long countByUniversityId(@Param("universityId") Long universityId);

    @Select("SELECT COUNT(*) FROM admission_result WHERE major_id = #{majorId} AND status = 'ADMITTED' AND is_adjusted = false")
    Long countAdmittedByMajorId(@Param("majorId") Long majorId);
}