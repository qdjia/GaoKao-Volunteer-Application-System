package com.gaokao.mapper;

import com.gaokao.entity.SysUser;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SysUserMapper {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    SysUser findById(@Param("id") Long id);

    @Insert("INSERT INTO sys_user(username, password, role, student_id) VALUES(#{username}, #{password}, #{role}, #{studentId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);
}