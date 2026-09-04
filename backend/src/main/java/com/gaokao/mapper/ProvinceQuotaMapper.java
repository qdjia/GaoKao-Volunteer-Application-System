package com.gaokao.mapper;

import com.gaokao.entity.ProvinceQuota;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ProvinceQuotaMapper {

    @Select("SELECT pq.*, m.name as major_name, p.name as province_name " +
            "FROM province_quota pq " +
            "LEFT JOIN major m ON pq.major_id = m.id " +
            "LEFT JOIN province p ON pq.province_id = p.id " +
            "WHERE pq.major_id = #{majorId} ORDER BY pq.province_id")
    List<ProvinceQuota> findByMajorId(@Param("majorId") Long majorId);

    @Select("SELECT pq.*, m.name as major_name, p.name as province_name " +
            "FROM province_quota pq " +
            "LEFT JOIN major m ON pq.major_id = m.id " +
            "LEFT JOIN province p ON pq.province_id = p.id " +
            "WHERE pq.major_id = #{majorId} AND pq.province_id = #{provinceId}")
    ProvinceQuota findByMajorAndProvince(@Param("majorId") Long majorId, @Param("provinceId") Long provinceId);

    @Insert("INSERT INTO province_quota(major_id, province_id, quota) " +
            "VALUES(#{majorId}, #{provinceId}, #{quota}) " +
            "ON CONFLICT (major_id, province_id) DO UPDATE SET quota = EXCLUDED.quota")
    int insertOrUpdate(ProvinceQuota quota);

    @Delete("DELETE FROM province_quota WHERE major_id = #{majorId} AND province_id = #{provinceId}")
    int deleteByMajorAndProvince(@Param("majorId") Long majorId, @Param("provinceId") Long provinceId);
}
