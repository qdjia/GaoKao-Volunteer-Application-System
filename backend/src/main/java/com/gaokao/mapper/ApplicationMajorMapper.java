package com.gaokao.mapper;

import com.gaokao.entity.ApplicationMajor;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ApplicationMajorMapper {

    @Select("SELECT am.*, m.name as major_name FROM application_major am " +
            "LEFT JOIN major m ON am.major_id = m.id " +
            "WHERE am.application_id = #{applicationId} ORDER BY am.priority")
    List<ApplicationMajor> findByApplicationId(@Param("applicationId") Long applicationId);

    @Insert("INSERT INTO application_major(application_id, major_id, priority) VALUES(#{applicationId}, #{majorId}, #{priority})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ApplicationMajor am);

    @Delete("DELETE FROM application_major WHERE application_id = #{applicationId}")
    int deleteByApplicationId(@Param("applicationId") Long applicationId);
}