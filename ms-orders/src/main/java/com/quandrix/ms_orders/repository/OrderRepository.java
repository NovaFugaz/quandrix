package com.quandrix.ms_orders.repository;

import com.quandrix.ms_orders.model.Order;
import com.quandrix.ms_orders.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerId(Long buyerId);
    List<Order> findBySellerId(Long sellerId);
    List<Order> findByBuyerIdAndStatus(Long buyerId, OrderStatus status);
    List<Order> findByStatus(OrderStatus status);
}