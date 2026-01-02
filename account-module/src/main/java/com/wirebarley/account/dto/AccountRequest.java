package com.wirebarley.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "계좌 등록 요청")
@Getter
@NoArgsConstructor
public class AccountRequest {

    @Schema(description = "계좌번호 (10~20자리 숫자)", example = "1234567890", required = true)
    @NotBlank(message = "계좌번호는 필수입니다.")
    @Pattern(regexp = "^[0-9]{10,20}$", message = "계좌번호는 10~20자리 숫자여야 합니다.")
    private String accountNumber;

    @Schema(description = "예금주명 (2~50자)", example = "홍길동", required = true)
    @NotBlank(message = "예금주명은 필수입니다.")
    @Size(min = 2, max = 50, message = "예금주명은 2~50자 사이여야 합니다.")
    private String accountHolder;

    @Builder
    public AccountRequest(String accountNumber, String accountHolder) {
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
    }
}
