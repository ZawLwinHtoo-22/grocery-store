package com.example.grocerystore.repository;

import com.example.grocerystore.model.CustomerOrder;
import com.example.grocerystore.model.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findWithItemsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("select o from CustomerOrder o where o.trackingCode = :trackingCode")
    Optional<CustomerOrder> findWithItemsByTrackingCode(@Param("trackingCode") String trackingCode);

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<CustomerOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    // Count orders created within a time range (used to generate daily sequential tracking numbers)
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Find orders by phone number for customer history lookup
    List<CustomerOrder> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);

    // Find orders between dates (inclusive start, exclusive end)
    List<CustomerOrder> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    // Sum totalAmount in range
    @Query("select sum(o.totalAmount) from CustomerOrder o where o.createdAt between :start and :end")
    BigDecimal sumTotalAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Top selling products in the date range (product id, product name, total quantity)
    @Query("select oi.product.id, oi.product.name, sum(oi.quantity) as qty from OrderItem oi where oi.order.createdAt between :start and :end group by oi.product.id, oi.product.name order by qty desc")
    List<Object[]> findTopSellingProducts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
