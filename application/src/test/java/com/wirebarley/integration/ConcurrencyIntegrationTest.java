package com.wirebarley.integration;

import com.wirebarley.account.dto.AccountRequest;
import com.wirebarley.account.dto.AccountResponse;
import com.wirebarley.account.service.AccountService;
import com.wirebarley.transaction.dto.DepositRequest;
import com.wirebarley.transaction.dto.TransferRequest;
import com.wirebarley.transaction.dto.WithdrawRequest;
import com.wirebarley.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("동시성 통합 테스트")
class ConcurrencyIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    private String accountNumber1;
    private String accountNumber2;

    @BeforeEach
    void setUp() {
        // 테스트용 계좌 생성
        accountNumber1 = "1111111111";
        accountNumber2 = "2222222222";

        AccountResponse account1 = accountService.createAccount(AccountRequest.builder()
                .accountNumber(accountNumber1)
                .accountHolder("테스트사용자1")
                .build());

        AccountResponse account2 = accountService.createAccount(AccountRequest.builder()
                .accountNumber(accountNumber2)
                .accountHolder("테스트사용자2")
                .build());

        // 초기 잔액 입금
        transactionService.deposit(DepositRequest.builder()
                .accountNumber(accountNumber1)
                .amount(new BigDecimal("1000000"))
                .build());

        transactionService.deposit(DepositRequest.builder()
                .accountNumber(accountNumber2)
                .amount(new BigDecimal("1000000"))
                .build());
    }

    @Test
    @DisplayName("동시 입금: 10개 스레드에서 동시에 입금 시 정합성 유지")
    void concurrentDeposit_ShouldMaintainDataIntegrity() throws InterruptedException {
        // given
        int threadCount = 10;
        BigDecimal depositAmount = new BigDecimal("10000");
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    transactionService.deposit(DepositRequest.builder()
                            .accountNumber(accountNumber1)
                            .amount(depositAmount)
                            .build());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        AccountResponse account = accountService.getAccountByNumber(accountNumber1);
        BigDecimal expectedBalance = new BigDecimal("1000000")
                .add(depositAmount.multiply(BigDecimal.valueOf(successCount.get())));

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(account.getBalance()).isEqualByComparingTo(expectedBalance);
    }

    @Test
    @DisplayName("동시 출금: 잔액 부족 시 일부 요청만 성공")
    void concurrentWithdraw_ShouldRejectWhenInsufficientBalance() throws InterruptedException {
        // given
        int threadCount = 15;
        BigDecimal withdrawAmount = new BigDecimal("100000");
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 15개 스레드에서 각각 100,000원 출금 시도 (총 1,500,000원, 잔액은 1,000,000원)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    transactionService.withdraw(WithdrawRequest.builder()
                            .accountNumber(accountNumber1)
                            .amount(withdrawAmount)
                            .build());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 10개만 성공해야 함 (1,000,000 / 100,000 = 10)
        AccountResponse account = accountService.getAccountByNumber(accountNumber1);

        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(5);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("동시 이체: A→B, B→A 교차 이체 시 데드락 없이 처리")
    void concurrentTransfer_ShouldNotCauseDeadlock() throws InterruptedException {
        // given
        int threadCount = 20;
        BigDecimal transferAmount = new BigDecimal("10000");
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        // when: 10개는 A→B, 10개는 B→A로 동시 이체
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    if (index % 2 == 0) {
                        transactionService.transfer(TransferRequest.builder()
                                .fromAccountNumber(accountNumber1)
                                .toAccountNumber(accountNumber2)
                                .amount(transferAmount)
                                .build());
                    } else {
                        transactionService.transfer(TransferRequest.builder()
                                .fromAccountNumber(accountNumber2)
                                .toAccountNumber(accountNumber1)
                                .amount(transferAmount)
                                .build());
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptions.add(e);
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 데드락 없이 완료되어야 함
        assertThat(completed).isTrue();

        // 총 잔액은 유지되어야 함 (수수료 제외)
        AccountResponse account1 = accountService.getAccountByNumber(accountNumber1);
        AccountResponse account2 = accountService.getAccountByNumber(accountNumber2);

        BigDecimal totalBalance = account1.getBalance().add(account2.getBalance());
        BigDecimal feePerTransfer = transferAmount.multiply(new BigDecimal("0.01"));
        BigDecimal totalFee = feePerTransfer.multiply(BigDecimal.valueOf(successCount.get()));
        BigDecimal expectedTotal = new BigDecimal("2000000").subtract(totalFee);

        assertThat(totalBalance).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @DisplayName("동시 혼합 거래: 입금, 출금, 이체가 동시에 발생할 때 정합성 유지")
    void concurrentMixedTransactions_ShouldMaintainDataIntegrity() throws InterruptedException, ExecutionException {
        // given
        int operationsPerType = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(15);
        List<Future<?>> futures = new ArrayList<>();
        AtomicInteger depositSuccess = new AtomicInteger(0);
        AtomicInteger withdrawSuccess = new AtomicInteger(0);
        AtomicInteger transferSuccess = new AtomicInteger(0);

        // when
        // 입금 5회
        for (int i = 0; i < operationsPerType; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    transactionService.deposit(DepositRequest.builder()
                            .accountNumber(accountNumber1)
                            .amount(new BigDecimal("10000"))
                            .build());
                    depositSuccess.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                }
            }));
        }

        // 출금 5회
        for (int i = 0; i < operationsPerType; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    transactionService.withdraw(WithdrawRequest.builder()
                            .accountNumber(accountNumber1)
                            .amount(new BigDecimal("10000"))
                            .build());
                    withdrawSuccess.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                }
            }));
        }

        // 이체 5회
        for (int i = 0; i < operationsPerType; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    transactionService.transfer(TransferRequest.builder()
                            .fromAccountNumber(accountNumber1)
                            .toAccountNumber(accountNumber2)
                            .amount(new BigDecimal("10000"))
                            .build());
                    transferSuccess.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                }
            }));
        }

        // 모든 작업 완료 대기
        for (Future<?> future : futures) {
            future.get();
        }
        executorService.shutdown();

        // then
        AccountResponse account1 = accountService.getAccountByNumber(accountNumber1);
        AccountResponse account2 = accountService.getAccountByNumber(accountNumber2);

        // 계산: 초기잔액 + 입금 - 출금 - 이체금액 - 이체수수료
        BigDecimal account1Expected = new BigDecimal("1000000")
                .add(new BigDecimal("10000").multiply(BigDecimal.valueOf(depositSuccess.get())))
                .subtract(new BigDecimal("10000").multiply(BigDecimal.valueOf(withdrawSuccess.get())))
                .subtract(new BigDecimal("10100").multiply(BigDecimal.valueOf(transferSuccess.get())));

        BigDecimal account2Expected = new BigDecimal("1000000")
                .add(new BigDecimal("10000").multiply(BigDecimal.valueOf(transferSuccess.get())));

        assertThat(account1.getBalance()).isEqualByComparingTo(account1Expected);
        assertThat(account2.getBalance()).isEqualByComparingTo(account2Expected);
    }

    @Test
    @DisplayName("일일 한도 동시 검증: 동시 출금 시 일일 한도 정확히 적용")
    void concurrentWithdraw_ShouldRespectDailyLimit() throws InterruptedException {
        // given: 잔액을 충분히 증가
        transactionService.deposit(DepositRequest.builder()
                .accountNumber(accountNumber1)
                .amount(new BigDecimal("9000000"))
                .build());

        int threadCount = 20;
        BigDecimal withdrawAmount = new BigDecimal("100000");
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger limitExceededCount = new AtomicInteger(0);

        // when: 20개 스레드에서 각각 100,000원 출금 시도 (총 2,000,000원, 일일 한도 1,000,000원)
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    transactionService.withdraw(WithdrawRequest.builder()
                            .accountNumber(accountNumber1)
                            .amount(withdrawAmount)
                            .build());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    if (e.getMessage().contains("한도")) {
                        limitExceededCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 일일 한도(1,000,000원)로 인해 최대 10건만 성공
        assertThat(successCount.get()).isLessThanOrEqualTo(10);
        assertThat(limitExceededCount.get()).isGreaterThanOrEqualTo(10);
    }
}
