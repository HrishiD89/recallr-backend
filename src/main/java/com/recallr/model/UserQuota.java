package com.recallr.model;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDate;

@Entity
@Table(name = "user_quotas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "quota_date"})
})
public class UserQuota {

    @Id
    @GeneratedValue(generator = "tsid")
    @GenericGenerator(name = "tsid", type = com.recallr.config.TsidGenerator.class)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "quota_date", nullable = false)
    private LocalDate quotaDate;

    @Column(name = "bookmark_saves_used", nullable = false)
    private int bookmarkSavesUsed = 0;

    @Column(name = "rag_queries_used", nullable = false)
    private int ragQueriesUsed = 0;

    public UserQuota() {}

    public UserQuota(User user, LocalDate quotaDate) {
        this.user = user;
        this.quotaDate = quotaDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getQuotaDate() {
        return quotaDate;
    }

    public void setQuotaDate(LocalDate quotaDate) {
        this.quotaDate = quotaDate;
    }

    public int getBookmarkSavesUsed() {
        return bookmarkSavesUsed;
    }

    public void setBookmarkSavesUsed(int bookmarkSavesUsed) {
        this.bookmarkSavesUsed = bookmarkSavesUsed;
    }

    public int getRagQueriesUsed() {
        return ragQueriesUsed;
    }

    public void setRagQueriesUsed(int ragQueriesUsed) {
        this.ragQueriesUsed = ragQueriesUsed;
    }
}
