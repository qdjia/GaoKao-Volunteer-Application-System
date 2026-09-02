package com.gaokao.service;

import com.gaokao.entity.Student;
import com.gaokao.entity.InterestCourse;
import com.gaokao.mapper.StudentMapper;
import com.gaokao.mapper.InterestCourseMapper;
import com.gaokao.util.SubjectMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private InterestCourseMapper interestCourseMapper;

    public List<Student> findList(String name, String studentNo, Long classId, Long provinceId) {
        return studentMapper.findList(name, studentNo, classId, provinceId);
    }

    public Student findById(Long id) {
        return studentMapper.findById(id);
    }

    @Transactional
    public void save(Student student) {
        if (!SubjectMatcher.isValidCombination(student.getSubjectCombo())) {
            throw new RuntimeException("选科组合必须为物理或历史加化学、生物、政治、地理中的任意两门");
        }
        if (student.getId() == null) {
            studentMapper.insert(student);
        } else {
            studentMapper.update(student);
        }
    }

    @Transactional
    public void deleteById(Long id) {
        studentMapper.deleteById(id);
    }

    public List<InterestCourse> getInterestCourses(Long studentId) {
        return interestCourseMapper.findByStudentId(studentId);
    }

    @Transactional
    public void saveInterestCourses(Long studentId, List<String> courseNames) {
        interestCourseMapper.deleteByStudentId(studentId);
        for (String name : courseNames) {
            InterestCourse course = new InterestCourse();
            course.setStudentId(studentId);
            course.setName(name);
            interestCourseMapper.insert(course);
        }
    }

    public List<Student> findAllOrderByScore() {
        return studentMapper.findAllOrderByScore();
    }
}
