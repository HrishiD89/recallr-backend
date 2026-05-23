package com.recallr.repository;

import com.recallr.model.Content;
import com.recallr.model.Tag;
import com.recallr.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {
    List<Content> findByUserOrderByCreatedAtDesc(User user);
    Optional<Content> findByIdAndUser(Long id, User user);
    List<Content> findByUserAndTagsContaining(User user, Tag tag);
}
