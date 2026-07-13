package com.gaokao.mapper;

import com.gaokao.entity.ScoreLine;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ScoreLineMapper {

    @Select("<script>" +
            "SELECT sl.*, p.name as province_name FROM score_line sl " +
            "LEFT JOIN province p ON sl.province_id = p.id " +
            "<where>" +
            "<if test='provinceId != null'> AND sl.province_id = #{provinceId}</if>" +
            "<if test='year != null'> AND sl.\"year\" = #{year}</if>" +
            "<if test='batch != null'> AND sl.batch = #{batch}</if>" +
            "<if test='subjectType != null'> AND sl.subject_type = #{subjectType}</if>" +
            "</where>" +
            " ORDER BY sl.\"year\" DESC, sl.province_id" +
            "</script>")
    List<ScoreLine> findList(@Param("provinceId") Long provinceId, @Param("year") Integer year,
                             @Param("batch") String batch, @Param("subjectType") String subjectType);

    @Insert("INSERT INTO score_line(province_id, \"year\", batch, subject_type, score) " +
            "VALUES(#{provinceId}, #{year}, #{batch}, #{subjectType}, #{score})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScoreLine scoreLine);

    @Update("UPDATE score_line SET province_id=#{provinceId}, \"year\"=#{year}, batch=#{batch}, " +
            "subject_type=#{subjectType}, score=#{score} WHERE id=#{id}")
    int update(ScoreLine scoreLine);

    @Delete("DELETE FROM score_line WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
