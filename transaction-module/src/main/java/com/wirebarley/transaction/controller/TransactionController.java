package com.wirebarley.transaction.controller;

import com.wirebarley.transaction.dto.*;
import com.wirebarley.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "거래 API", description = "입금, 출금, 이체 및 거래내역 조회 API")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "입금", description = "특정 계좌에 금액을 입금합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "입금 성공",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음")
    })
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request) {
        TransactionResponse response = transactionService.deposit(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "출금", description = "특정 계좌에서 금액을 출금합니다. 일일 최대 1,000,000원까지 출금 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "출금 성공",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 잔액 부족 또는 일일 한도 초과"),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음")
    })
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawRequest request) {
        TransactionResponse response = transactionService.withdraw(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이체",
            description = "출금 계좌에서 다른 계좌로 금액을 이체합니다. 이체 금액의 1%가 수수료로 부과되며, 일일 최대 3,000,000원까지 이체 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이체 성공",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 잔액 부족 또는 일일 한도 초과 또는 동일 계좌 이체"),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음")
    })
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transactionService.transfer(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "거래내역 조회 (계좌 ID)", description = "계좌 ID로 해당 계좌의 거래내역을 조회합니다. 최신순으로 정렬됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음")
    })
    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(
            @Parameter(description = "계좌 ID", required = true, example = "1")
            @PathVariable Long accountId,
            @Parameter(description = "페이지 정보 (page, size, sort)")
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TransactionResponse> response = transactionService.getTransactionHistory(accountId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "거래내역 조회 (계좌번호)", description = "계좌번호로 해당 계좌의 거래내역을 조회합니다. 최신순으로 정렬됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음")
    })
    @GetMapping("/account/number/{accountNumber}")
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistoryByAccountNumber(
            @Parameter(description = "계좌번호", required = true, example = "1234567890")
            @PathVariable String accountNumber,
            @Parameter(description = "페이지 정보 (page, size, sort)")
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TransactionResponse> response = transactionService.getTransactionHistoryByAccountNumber(accountNumber, pageable);
        return ResponseEntity.ok(response);
    }
}
