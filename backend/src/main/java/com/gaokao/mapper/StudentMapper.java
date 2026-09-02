package com.gaokao.mapper;

import com.gaokao.entity.Student;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface StudentMapper {

    @Select("<script>" +
            "SELECT s.*, p.name as province_name, c.name as class_name FROM student s " +
            "LEFT JOIN province p ON s.province_id = p.id " +
            "LEFT JOIN class_info c ON s.class_id = c.id " +
            "<where>" +
            "<if test='name != null'> AND s.name LIKE CONCAT('%',#{name},'%')</if>" +
            "<if test='studentNo != null'> AND s.student_no = #{studentNo}</if>" +
            "<if test='classId != null'> AND s.class_id = #{classId}</if>" +
            "<if test='provinceId != null'> AND s.province_id = #{provinceId}</if>" +
            "</where>" +
            " ORDER BY s.total_score DESC" +
            "</script>")
    List<Student> findList(@Param("name") String name, @Param("studentNo") String studentNo,
                           @Param("classId") Long classId, @Param("provinceId") Long provinceId);

    @Select("SELECT s.*, p.name as province_name, c.name as class_name FROM student s " +
            "LEFT JOIN province p ON s.province_id = p.id " +
            "LEFT JOIN class_info c ON s.class_id = c.id WHERE s.id = #{id}")
    Student findById(@Param("id") Long id);

    @Select("SELECT s.*, p.name as province_name, c.name as class_name FROM student s " +
            "LEFT JOIN province p ON s.province_id = p.id " +
            "LEFT JOIN class_info c ON s.class_id = c.id WHERE s.student_no = #{studentNo}")
    Student findByStudentNo(@Param("studentNo") String studentNo);

    @Insert("INSERT INTO student(student_no, name, gender, id_card, total_score, chinese_score, math_score, foreign_language_score, province_id, class_id, subject_combo, phone) " +
            "VALUES(#{studentNo}, #{name}, #{gender}, #{idCard}, #{totalScore}, #{chineseScore}, #{mathScore}, #{foreignLanguageScore}, #{provinceId}, #{classId}, #{subjectCombo}, #{phone})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Student student);

    @Update("UPDATE student SET student_no=#{studentNo}, name=#{name}, gender=#{gender}, id_card=#{idCard}, " +
            "total_score=#{totalScore}, chinese_score=#{chineseScore}, math_score=#{mathScore}, foreign_language_score=#{foreignLanguageScore}, " +
            "province_id=#{provinceId}, class_id=#{classId}, subject_combo=#{subjectCombo}, " +
            "phone=#{phone}, status=#{status} WHERE id=#{id}")
    int update(Student student);

    @Delete("DELETE FROM student WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM student WHERE class_id = #{classId}")
    int countByClassId(@Param("classId") Long classId);

    @Select("SELECT s.*, p.name as province_name, c.name as class_name FROM student s " +
            "LEFT JOIN province p ON s.province_id = p.id " +
            "LEFT JOIN class_info c ON s.class_id = c.id " +
            "ORDER BY s.total_score DESC")
    List<Student> findAllOrderByScore();
}
