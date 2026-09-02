package com.gaokao.service;

import com.gaokao.dto.RegisterRequest;
import com.gaokao.entity.Student;
import com.gaokao.entity.SysUser;
import com.gaokao.mapper.StudentMapper;
import com.gaokao.mapper.SysUserMapper;
import com.gaokao.util.AuthInterceptor;
import com.gaokao.util.SubjectMatcher;
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
        if (!"ADMIN".equals(user.getRole()) && !"STUDENT".equals(user.getRole())) {
            throw new RuntimeException("当前系统仅支持管理员和考生登录");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        AuthInterceptor.TOKEN_MAP.put(token, username + ":" + user.getRole() + ":" + user.getStudentId());
        return token;
    }

    @Transactional
    public SysUser register(RegisterRequest req) {
        if (req.getRole() != null && !"STUDENT".equals(req.getRole())) {
            throw new RuntimeException("仅支持考生自助注册");
        }
        SysUser existing = sysUserMapper.findByUsername(req.getUsername());
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(req.getPassword());
        user.setRole("STUDENT");

        if (req.getName() == null || req.getName().isBlank()) {
            throw new RuntimeException("考生姓名不能为空");
        }
        if (req.getProvinceId() == null) {
            throw new RuntimeException("考生省份不能为空");
        }
        if (req.getSubjectCombo() == null || req.getSubjectCombo().isBlank()) {
            throw new RuntimeException("选科组合不能为空");
        }
        if (!SubjectMatcher.isValidCombination(req.getSubjectCombo())) {
            throw new RuntimeException("选科组合必须为物理或历史加化学、生物、政治、地理中的任意两门");
        }

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
