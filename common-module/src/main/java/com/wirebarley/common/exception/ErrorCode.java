package com.wirebarley.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 계좌
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "계좌를 찾을 수 없습니다."),
    DUPLICATE_ACCOUNT_NUMBER(HttpStatus.CONFLICT, "A002", "이미 존재하는 계좌번호입니다."),
    ACCOUNT_HAS_BALANCE(HttpStatus.BAD_REQUEST, "A003", "잔액이 있는 계좌는 삭제할 수 없습니다."),

    // 이체
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "T001", "잔액이 부족합니다."),
    DAILY_WITHDRAWAL_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "T002", "일일 출금 한도를 초과했습니다."),
    DAILY_TRANSFER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "T003", "일일 이체 한도를 초과했습니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "T004", "유효하지 않은 금액입니다."),
    SAME_ACCOUNT_TRANSFER(HttpStatus.BAD_REQUEST, "T005", "동일 계좌로 이체할 수 없습니다."),

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
