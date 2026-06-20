package com.ARVision.repository;

import com.ARVision.entity.ARModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ARModelRepository extends JpaRepository<ARModel, Long> {
    Optional<ARModel> findByProductProductId(Long productId);
    boolean existsByProductProductId(Long productId);
    void deleteByProductProductId(Long productId);
}