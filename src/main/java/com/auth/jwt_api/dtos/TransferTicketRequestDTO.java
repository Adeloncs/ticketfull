package com.auth.jwt_api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Transferência de ingresso: e-mail do usuário que passará a ser o detentor. */
public record TransferTicketRequestDTO(
        @NotBlank @Email String targetEmail) {
}
