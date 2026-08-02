package com.recallr.domain.content;

import com.recallr.domain.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {
    List<Content> findByUserOrderByCreatedAtDesc(User user);
    Optional<Content> findByIdAndUser(Long id, User user);
    List<Content> findByUserAndTagsContaining(User user, Tag tag);
    Optional<Content> findByUrlAndUser(String url, User user);
    long countByUser(User user);

    @Modifying
    @Query("UPDATE Content c SET c.processingStatus = :processingStatus WHERE c.id = :contentId")
    void updateStatus(@Param("contentId") Long contentId,
                      @Param("processingStatus") ProcessingStatus processingStatus);
}
