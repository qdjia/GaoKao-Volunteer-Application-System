package com.gaokao.mapper;

import com.gaokao.entity.Major;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MajorMapper {

    @Select("<script>" +
            "SELECT m.*, d.name as department_name, u.id as university_id, u.name as university_name " +
            "FROM major m " +
            "LEFT JOIN department d ON m.department_id = d.id " +
            "LEFT JOIN university u ON d.university_id = u.id " +
            "<where>" +
            "<if test='departmentId != null'> AND m.department_id = #{departmentId}</if>" +
            "<if test='universityId != null'> AND d.university_id = #{universityId}</if>" +
            "<if test='name != null'> AND m.name LIKE CONCAT('%',#{name},'%')</if>" +
            "</where>" +
            " ORDER BY m.id" +
            "</script>")
    List<Major> findList(@Param("departmentId") Long departmentId, @Param("universityId") Long universityId,
                         @Param("name") String name);

    @Select("SELECT m.*, d.name as department_name, u.id as university_id, u.name as university_name " +
            "FROM major m LEFT JOIN department d ON m.department_id = d.id " +
            "LEFT JOIN university u ON d.university_id = u.id WHERE m.id = #{id}")
    Major findById(@Param("id") Long id);

    @Insert("INSERT INTO major(name, department_id, subject_req, total_quota) VALUES(#{name}, #{departmentId}, #{subjectReq}, #{totalQuota})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Major major);

    @Update("UPDATE major SET name=#{name}, department_id=#{departmentId}, subject_req=#{subjectReq}, total_quota=#{totalQuota} WHERE id=#{id}")
    int update(Major major);

    @Delete("DELETE FROM major WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT m.*, d.name as department_name, u.id as university_id, u.name as university_name " +
            "FROM major m LEFT JOIN department d ON m.department_id = d.id " +
            "LEFT JOIN university u ON d.university_id = u.id ORDER BY m.id")
    List<Major> findAll();
}