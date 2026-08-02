package com.recallr.domain.content;

import com.recallr.domain.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByUser(User user);
    Optional<Tag> findByNameAndUser(String name, User user);
    boolean existsByNameAndUser(String name, User user);
}
