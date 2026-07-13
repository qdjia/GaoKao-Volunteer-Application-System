package com.gaokao.service;

import com.gaokao.entity.MajorCourse;
import com.gaokao.mapper.MajorCourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class MajorCourseService {

    @Autowired
    private MajorCourseMapper majorCourseMapper;

    public List<MajorCourse> findByMajorId(Long majorId) {
        return majorCourseMapper.findByMajorId(majorId);
    }

    @Transactional
    public void saveCourses(Long majorId, List<String> courseNames) {
        majorCourseMapper.deleteByMajorId(majorId);
        for (String name : courseNames) {
            MajorCourse course = new MajorCourse();
            course.setMajorId(majorId);
            course.setName(name);
            majorCourseMapper.insert(course);
        }
    }
}