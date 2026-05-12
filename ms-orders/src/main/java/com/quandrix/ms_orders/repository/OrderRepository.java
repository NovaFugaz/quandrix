package com.quandrix.ms_orders.repository;

import com.quandrix.ms_orders.model.Order;
import com.quandrix.ms_orders.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerId(Long buyerId);
    List<Order> findBySellerId(Long sellerId);
    List<Order> findByBuyerIdAndStatus(Long buyerId, OrderStatus status);
    List<Order> findByStatus(OrderStatus status);
}