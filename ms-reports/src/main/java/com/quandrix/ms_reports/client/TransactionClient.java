package com.quandrix.ms_reports.client;

import com.quandrix.ms_reports.dto.TransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;


@FeignClient(name = "ms-transactions")
public interface TransactionClient {

    @GetMapping("/transactions/admin")
    List<TransactionResponse> getAll();

    @GetMapping("/transactions/range")
    List<TransactionResponse> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to);
}
