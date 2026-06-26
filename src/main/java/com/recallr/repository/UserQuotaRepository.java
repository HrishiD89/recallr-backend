package com.recallr.repository;

import com.recallr.model.User;
import com.recallr.model.UserQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserQuotaRepository extends JpaRepository<UserQuota, Long> {
    Optional<UserQuota> findByUserAndQuotaDate(User user, LocalDate quotaDate);
}
