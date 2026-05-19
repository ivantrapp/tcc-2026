package com.cqrs.repository;

import com.cqrs.entity.CustomerViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CustomerViewRepository extends JpaRepository<CustomerViewEntity, UUID> {

    @Query(value = """
            SELECT * FROM customer_view
            WHERE account_manager_id = :managerId
              AND (:minBalance IS NULL OR total_balance >= :minBalance)
              AND (:maxDaysInactive IS NULL
                   OR last_transaction_at < NOW() - CAST(:maxDaysInactive || ' days' AS INTERVAL)
                   OR last_transaction_at IS NULL)
            ORDER BY
              CASE WHEN :orderBy = 'totalBalance'    THEN total_balance END DESC NULLS LAST,
              CASE WHEN :orderBy = 'lastTransaction' THEN last_transaction_at END DESC NULLS LAST,
              customer_name ASC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<CustomerViewEntity> findByManager(
            @Param("managerId") UUID managerId,
            @Param("minBalance") BigDecimal minBalance,
            @Param("maxDaysInactive") Integer maxDaysInactive,
            @Param("orderBy") String orderBy,
            @Param("size") int size,
            @Param("offset") int offset
    );

    @Query(value = """
            SELECT * FROM customer_view
            WHERE (:minBalance IS NULL OR total_balance >= :minBalance)
              AND (:maxDaysInactive IS NULL
                   OR last_transaction_at < NOW() - CAST(:maxDaysInactive || ' days' AS INTERVAL)
                   OR last_transaction_at IS NULL)
            ORDER BY
              CASE WHEN :orderBy = 'totalBalance'    THEN total_balance END DESC NULLS LAST,
              CASE WHEN :orderBy = 'lastTransaction' THEN last_transaction_at END DESC NULLS LAST,
              customer_name ASC
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<CustomerViewEntity> findAll(
            @Param("minBalance") BigDecimal minBalance,
            @Param("maxDaysInactive") Integer maxDaysInactive,
            @Param("orderBy") String orderBy,
            @Param("size") int size,
            @Param("offset") int offset
    );
}
