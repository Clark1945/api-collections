package com.example.userapi.service;

import com.example.userapi.dto.request.CreateUserRequest;
import com.example.userapi.dto.request.UpdateUserRequest;
import com.example.userapi.entity.Role;
import com.example.userapi.entity.User;
import com.example.userapi.enums.AuditEventType;
import com.example.userapi.exception.DuplicateResourceException;
import com.example.userapi.exception.ResourceNotFoundException;
import com.example.userapi.repository.RoleRepository;
import com.example.userapi.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());

        User saved = userRepository.save(user);
        auditLogService.log(saved, AuditEventType.ACCOUNT_CREATED, null);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional
    public User updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (request.getEmail() != null) {
            userRepository.findByEmail(request.getEmail())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException("User", "email", request.getEmail());
                    });
            user.setEmail(request.getEmail());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        User updated = userRepository.save(user);
        auditLogService.log(updated, AuditEventType.ACCOUNT_UPDATED, null);
        return updated;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        userRepository.delete(user);
    }

    @Transactional
    public User assignRoles(Long userId, Set<Long> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
        if (roles.size() != roleIds.size()) {
            throw new ResourceNotFoundException("Role", "ids", roleIds);
        }

        user.setRoles(roles);
        User updated = userRepository.save(user);
        auditLogService.log(updated, AuditEventType.ROLE_CHANGE,
                "Roles assigned: " + roleIds);
        return updated;
    }
}
