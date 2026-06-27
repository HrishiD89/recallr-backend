package com.recallr.service.quota;

import com.recallr.model.User;
import com.recallr.model.UserQuota;
import com.recallr.model.UserTier;
import com.recallr.repository.UserQuotaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
public class QuotaService {

    private static final int BOOKMARK_LIMIT_FREE = 20;
    private static final int RAG_LIMIT_FREE = 10;

    private final UserQuotaRepository quotaRepository;

    public QuotaService(UserQuotaRepository quotaRepository) {
        this.quotaRepository = quotaRepository;
    }

    @Transactional
    public UserQuota getOrCreateTodayQuota(User user) {
        LocalDate today = LocalDate.now();

        // 1. Try to find the quota
        return quotaRepository.findByUserAndQuotaDate(user, today)
                .orElseGet(() -> {
                    try {
                        // 2. If not found, create a new one
                        UserQuota newQuota = new UserQuota(user, today);
                        return quotaRepository.saveAndFlush(newQuota);
                    } catch (DataIntegrityViolationException ex) {
                        // Concurrency safeguard: if another thread created it concurrently,
                        // reload and return the existing row
                        return quotaRepository.findByUserAndQuotaDate(user, today)
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Could not resolve quota due to a database conflict."
                                ));
                    }
                });
    }

    @Transactional
    public void checkAndIncrementBookmarkLimit(User user) {
        if (user.getTier() == UserTier.PRO) {
            return; // Pro users have unlimited bookmark saves
        }

        UserQuota quota = getOrCreateTodayQuota(user);
        if (quota.getBookmarkSavesUsed() >= BOOKMARK_LIMIT_FREE) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Daily bookmark limit reached (" + BOOKMARK_LIMIT_FREE + "/day)"
            );
        }

        quota.setBookmarkSavesUsed(quota.getBookmarkSavesUsed() + 1);
        quotaRepository.save(quota);
    }

    @Transactional
    public void checkAndIncrementRagLimit(User user) {
        if (user.getTier() == UserTier.PRO) {
            return; // Pro users have unlimited RAG queries
        }

        UserQuota quota = getOrCreateTodayQuota(user);
        if (quota.getRagQueriesUsed() >= RAG_LIMIT_FREE) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Daily RAG query limit reached (" + RAG_LIMIT_FREE + "/day)"
            );
        }

        quota.setRagQueriesUsed(quota.getRagQueriesUsed() + 1);
        quotaRepository.save(quota);
    }
}