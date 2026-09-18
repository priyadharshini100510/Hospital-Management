package com.hospital.hms.service.impl;

import com.hospital.hms.entity.Department;
import com.hospital.hms.exception.DuplicateResourceException;
import com.hospital.hms.exception.ResourceNotFoundException;
import com.hospital.hms.repository.DepartmentRepository;
import com.hospital.hms.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }

    @Override
    @Transactional
    public Department create(Department department) {
        if (departmentRepository.existsByNameIgnoreCase(department.getName())) {
            throw new DuplicateResourceException("Department '" + department.getName() + "' already exists.");
        }
        return departmentRepository.save(department);
    }

    @Override
    @Transactional
    public Department update(Long id, Department department) {
        Department existing = findById(id);
        existing.setName(department.getName());
        existing.setDescription(department.getDescription());
        return departmentRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Department existing = findById(id);
        departmentRepository.delete(existing);
    }
}
