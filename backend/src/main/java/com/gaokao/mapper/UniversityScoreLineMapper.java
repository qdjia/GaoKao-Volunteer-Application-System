package com.gaokao.mapper;

import com.gaokao.entity.UniversityScoreLine;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UniversityScoreLineMapper {

    @Select("<script>" +
            "SELECT usl.*, u.name as university_name, m.name as major_name, p.name as province_name " +
            "FROM university_score_line usl " +
            "LEFT JOIN university u ON usl.university_id = u.id " +
            "LEFT JOIN major m ON usl.major_id = m.id " +
            "LEFT JOIN province p ON usl.province_id = p.id " +
            "<where>" +
            "<if test='universityId != null'> AND usl.university_id = #{universityId}</if>" +
            "<if test='provinceId != null'> AND usl.province_id = #{provinceId}</if>" +
            "<if test='year != null'> AND usl.\"year\" = #{year}</if>" +
            "</where>" +
            " ORDER BY usl.\"year\" DESC, usl.university_id" +
            "</script>")
    List<UniversityScoreLine> findList(@Param("universityId") Long universityId, @Param("provinceId") Long provinceId,
                                       @Param("year") Integer year);

    @Insert("INSERT INTO university_score_line(university_id, province_id, \"year\", major_id, min_score, avg_score) " +
            "VALUES(#{universityId}, #{provinceId}, #{year}, #{majorId}, #{minScore}, #{avgScore})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UniversityScoreLine usl);

    @Update("UPDATE university_score_line SET university_id=#{universityId}, province_id=#{provinceId}, " +
            "\"year\"=#{year}, major_id=#{majorId}, min_score=#{minScore}, avg_score=#{avgScore} WHERE id=#{id}")
    int update(UniversityScoreLine usl);

    @Delete("DELETE FROM university_score_line WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
