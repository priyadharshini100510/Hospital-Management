package com.hospital.hms.service;

import com.hospital.hms.entity.Department;

import java.util.List;

public interface DepartmentService {
    List<Department> findAll();
    Department findById(Long id);
    Department create(Department department);
    Department update(Long id, Department department);
    void delete(Long id);
}
