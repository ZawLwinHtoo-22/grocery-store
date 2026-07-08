package com.example.grocerystore.repository;

import com.example.grocerystore.model.CustomerOrder;
import com.example.grocerystore.model.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
