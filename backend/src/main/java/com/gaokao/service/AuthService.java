package com.gaokao.service;

import com.gaokao.dto.RegisterRequest;
import com.gaokao.entity.Student;
import com.gaokao.entity.SysUser;
import com.gaokao.mapper.StudentMapper;
import com.gaokao.mapper.SysUserMapper;
import com.gaokao.util.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private StudentMapper studentMapper;

    public String login(String username, String password) {
        SysUser user = sysUserMapper.findByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            throw new RuntimeException("用户名或密码错误");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        AuthInterceptor.TOKEN_MAP.put(token, username + ":" + user.getRole() + ":" + user.getStudentId());
        return token;
    }

    @Transactional
    public SysUser register(RegisterRequest req) {
        if (!"STUDENT".equals(req.getRole()) && !"TEACHER".equals(req.getRole())) {
            throw new RuntimeException("角色只能选择学生或教师");
        }
        SysUser existing = sysUserMapper.findByUsername(req.getUsername());
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(req.getPassword());
        user.setRole(req.getRole());
        if ("STUDENT".equals(req.getRole())) {
            Student student = new Student();
            student.setStudentNo(req.getUsername());
            student.setName(req.getName());
            student.setGender(req.getGender());
            student.setProvinceId(req.getProvinceId());
            student.setClassId(req.getClassId());
            student.setSubjectCombo(req.getSubjectCombo());
            student.setPhone(req.getPhone());
            student.setStatus("ACTIVE");
            studentMapper.insert(student);
            user.setStudentId(student.getId());
        }
        sysUserMapper.insert(user);
        return user;
    }

    public SysUser getUserInfo(String username) {
        return sysUserMapper.findByUsername(username);
    }

    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        AuthInterceptor.TOKEN_MAP.remove(token);
    }
}