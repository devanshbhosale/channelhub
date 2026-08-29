package com.devanshbhosale.channelhub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devanshbhosale.channelhub.domain.OrderStatus;
import com.devanshbhosale.channelhub.domain.SalesOrder;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    List<SalesOrder> findByResellerIdOrderByCreatedAtDesc(Long resellerId);

    List<SalesOrder> findByStatusOrderByCreatedAtAsc(OrderStatus status);

    List<SalesOrder> findByStatusIn(List<OrderStatus> statuses);

    List<SalesOrder> findByResellerIdAndStatusIn(Long resellerId, List<OrderStatus> statuses);
}
