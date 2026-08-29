package com.devanshbhosale.channelhub.service;

import com.devanshbhosale.channelhub.domain.OrderStatus;
import com.devanshbhosale.channelhub.domain.Reseller;
import com.devanshbhosale.channelhub.domain.SalesOrder;
import com.devanshbhosale.channelhub.dto.AccountRes;
import com.devanshbhosale.channelhub.dto.CreditRes;
import com.devanshbhosale.channelhub.exception.ApiException;
import com.devanshbhosale.channelhub.repository.ResellerRepository;
import com.devanshbhosale.channelhub.repository.SalesOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private final ResellerRepository resellerRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final OrderService orderService;

    public AccountService(ResellerRepository resellerRepository,
                          SalesOrderRepository salesOrderRepository,
                          OrderService orderService) {
        this.resellerRepository = resellerRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.orderService = orderService;
    }

    @Transactional(readOnly = true)
    public AccountRes load(Long userId) {
        Reseller reseller = resellerRepository.findByUserId(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reseller profile not found"));
        CreditRes credit = orderService.creditSummary(userId);
        List<SalesOrder> orders = salesOrderRepository.findByResellerIdOrderByCreatedAtDesc(reseller.getId());
        long pendingCount = orders.stream()
            .filter(order -> order.getStatus() == OrderStatus.PENDING)
            .count();
        return new AccountRes(reseller.getCompanyName(), reseller.getGstin(), reseller.getPhone(),
            credit, orders.size(), pendingCount);
    }
}
