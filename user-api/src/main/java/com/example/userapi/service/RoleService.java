package com.example.userapi.service;

import com.example.userapi.dto.request.CreateRoleRequest;
import com.example.userapi.entity.Role;
import com.example.userapi.exception.DuplicateResourceException;
import com.example.userapi.exception.ResourceNotFoundException;
import com.example.userapi.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional
    public Role createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Role", "name", request.getName());
        }
        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        return roleRepository.save(role);
    }

    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        roleRepository.delete(role);
    }
}
