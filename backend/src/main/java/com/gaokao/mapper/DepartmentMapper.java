package com.gaokao.mapper;

import com.gaokao.entity.Department;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DepartmentMapper {

    @Select("SELECT d.*, u.name as university_name FROM department d " +
            "LEFT JOIN university u ON d.university_id = u.id " +
            "WHERE d.university_id = #{universityId} ORDER BY d.id")
    List<Department> findByUniversityId(@Param("universityId") Long universityId);

    @Select("SELECT d.*, u.name as university_name FROM department d " +
            "LEFT JOIN university u ON d.university_id = u.id WHERE d.id = #{id}")
    Department findById(@Param("id") Long id);

    @Insert("INSERT INTO department(name, university_id) VALUES(#{name}, #{universityId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Department department);

    @Update("UPDATE department SET name=#{name}, university_id=#{universityId} WHERE id=#{id}")
    int update(Department department);

    @Delete("DELETE FROM department WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT d.*, u.name as university_name FROM department d LEFT JOIN university u ON d.university_id = u.id ORDER BY d.id")
    List<Department> findAll();
}