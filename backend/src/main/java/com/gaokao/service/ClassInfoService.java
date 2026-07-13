package com.gaokao.service;

import com.gaokao.entity.ClassInfo;
import com.gaokao.mapper.ClassInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ClassInfoService {

    @Autowired
    private ClassInfoMapper classInfoMapper;

    public List<ClassInfo> findList(String name) {
        return classInfoMapper.findList(name);
    }

    public ClassInfo findById(Long id) {
        return classInfoMapper.findById(id);
    }

    @Transactional
    public void save(ClassInfo classInfo) {
        if (classInfo.getId() == null) {
            classInfoMapper.insert(classInfo);
        } else {
            classInfoMapper.update(classInfo);
        }
    }

    @Transactional
    public void deleteById(Long id) {
        classInfoMapper.deleteById(id);
    }

    public List<ClassInfo> findAll() {
        return classInfoMapper.findAll();
    }
}