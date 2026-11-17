package br.gov.imbe.biblioteca.leitor;

import java.time.LocalDate;

import org.hibernate.validator.constraints.br.CPF;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * Cadastro ou alteração de leitor.
 *
 * @param cpf   com ou sem pontuação; gravado só com os 11 dígitos
 * @param ativo só na alteração; ausente mantém
 */
@LeitorValido
public record LeitorRequisicao(
        @NotBlank(message = "Informe o nome.") @Size(max = 200, message = "Use no máximo 200 caracteres.") String nome,
        @NotBlank(message = "Informe o CPF.") @CPF(message = "CPF inválido.") String cpf,
        @NotNull(message = "Informe a data de nascimento.") @Past(message = "A data de nascimento precisa ser passada.")
        LocalDate dataNascimento,
        @Size(max = 20, message = "Use no máximo 20 caracteres.") String telefone,
        @Email(message = "E-mail inválido.") @Size(max = 150, message = "Use no máximo 150 caracteres.") String email,
        @Size(max = 255, message = "Use no máximo 255 caracteres.") String endereco,
        @Size(max = 200, message = "Use no máximo 200 caracteres.") String responsavel,
        Boolean ativo) {

    static String soDigitos(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }
}
