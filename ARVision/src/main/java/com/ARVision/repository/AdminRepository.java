package com.ARVision.repository;

import com.ARVision.entity.Admin;
import com.ARVision.entity.Admin.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByEmail(String email);
    List<Admin> findByAdminrole(AdminRole adminRole);
}