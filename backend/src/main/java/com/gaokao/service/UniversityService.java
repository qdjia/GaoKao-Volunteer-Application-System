package com.gaokao.service;

import com.gaokao.entity.University;
import com.gaokao.entity.Department;
import com.gaokao.entity.Major;
import com.gaokao.entity.ProvinceQuota;
import com.gaokao.mapper.UniversityMapper;
import com.gaokao.mapper.DepartmentMapper;
import com.gaokao.mapper.MajorMapper;
import com.gaokao.mapper.ProvinceQuotaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UniversityService {

    @Autowired
    private UniversityMapper universityMapper;
    @Autowired
    private DepartmentMapper departmentMapper;
    @Autowired
    private MajorMapper majorMapper;
    @Autowired
    private ProvinceQuotaMapper provinceQuotaMapper;

    public List<University> findList(String name, String type, Long provinceId) {
        return universityMapper.findList(name, type, provinceId);
    }

    public University findById(Long id) {
        return universityMapper.findById(id);
    }

    @Transactional
    public void saveUniversity(University university) {
        if (university.getId() == null) {
            universityMapper.insert(university);
        } else {
            universityMapper.update(university);
        }
    }

    @Transactional
    public void deleteUniversity(Long id) {
        universityMapper.deleteById(id);
    }

    public List<Department> findDepartments(Long universityId) {
        return departmentMapper.findByUniversityId(universityId);
    }

    @Transactional
    public void saveDepartment(Department department) {
        if (department.getId() == null) {
            departmentMapper.insert(department);
        } else {
            departmentMapper.update(department);
        }
    }

    @Transactional
    public void deleteDepartment(Long id) {
        departmentMapper.deleteById(id);
    }

    public List<Major> findMajors(Long departmentId, Long universityId, String name) {
        return majorMapper.findList(departmentId, universityId, name);
    }

    public Major findMajorById(Long id) {
        return majorMapper.findById(id);
    }

    @Transactional
    public void saveMajor(Major major) {
        if (major.getId() == null) {
            majorMapper.insert(major);
        } else {
            majorMapper.update(major);
        }
    }

    @Transactional
    public void deleteMajor(Long id) {
        majorMapper.deleteById(id);
    }

    public List<ProvinceQuota> findQuotasByMajorId(Long majorId) {
        return provinceQuotaMapper.findByMajorId(majorId);
    }

    @Transactional
    public void saveQuota(ProvinceQuota quota) {
        ProvinceQuota existing = provinceQuotaMapper.findByMajorAndProvince(quota.getMajorId(), quota.getProvinceId());
        if (existing != null) {
            existing.setQuota(quota.getQuota());
            provinceQuotaMapper.insertOrUpdate(existing);
        } else {
            provinceQuotaMapper.insertOrUpdate(quota);
        }
    }
}