package com.ARVision.repository;

import com.ARVision.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);

    // New customers this month
    @Query("""
    SELECT COUNT(c) FROM Customer c
    WHERE EXTRACT(MONTH FROM c.memberSince) = EXTRACT(MONTH FROM CURRENT_DATE)
    AND EXTRACT(YEAR FROM c.memberSince) = EXTRACT(YEAR FROM CURRENT_DATE)
""")
    long countNewCustomersThisMonth();

    // New customers today
    @Query("SELECT COUNT(c) FROM Customer c " +
            "WHERE CAST(c.memberSince AS date) = CURRENT_DATE")
    long countNewCustomersToday();

}