package com.example.userapi.controller;

import com.example.userapi.dto.request.CreateUserRequest;
import com.example.userapi.dto.request.UpdateUserRequest;
import com.example.userapi.dto.response.AuditLogResponse;
import com.example.userapi.dto.response.UserResponse;
import com.example.userapi.entity.AuditLog;
import com.example.userapi.entity.User;
import com.example.userapi.service.AuditLogService;
import com.example.userapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "User management APIs")
public class UserController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public UserController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @GetMapping
    @Operation(summary = "Get all users (paginated)")
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        Page<UserResponse> users = userService.getAllUsers(pageable).map(UserResponse::from);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        User user = userService.updateUser(id, request);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Assign roles to a user")
    public ResponseEntity<UserResponse> assignRoles(
            @PathVariable Long id,
            @RequestBody Set<Long> roleIds) {
        User user = userService.assignRoles(id, roleIds);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @GetMapping("/{id}/audit-logs")
    @Operation(summary = "Get audit logs for a user (paginated)")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @PathVariable Long id, Pageable pageable) {
        Page<AuditLogResponse> logs = auditLogService
                .getAuditLogsByUserId(id, pageable)
                .map(AuditLogResponse::from);
        return ResponseEntity.ok(logs);
    }
}
