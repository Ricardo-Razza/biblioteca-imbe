package br.gov.imbe.biblioteca.leitor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.Period;

import br.gov.imbe.biblioteca.comum.config.RelogioConfig;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * Regras que dependem de mais de um campo, com o erro no campo certo:
 * menor de idade exige {@code responsavel}; nascimento antes de 1900 é erro de digitação.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LeitorValido.Validador.class)
@interface LeitorValido {

    String message() default "Dados do leitor inválidos.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<LeitorValido, LeitorRequisicao> {

        @Override
        public boolean isValid(LeitorRequisicao req, ConstraintValidatorContext ctx) {
            LocalDate nascimento = req.dataNascimento();
            if (nascimento == null) {
                return true;
            }
            ctx.disableDefaultConstraintViolation();
            if (nascimento.getYear() < 1900) {
                erro(ctx, "dataNascimento", "Data de nascimento improvável. Confira o ano.");
                return false;
            }
            boolean menor = Period.between(nascimento, LocalDate.now(RelogioConfig.FUSO)).getYears() < Leitor.MAIORIDADE;
            if (menor && (req.responsavel() == null || req.responsavel().isBlank())) {
                erro(ctx, "responsavel", "Leitor menor de idade: informe o responsável.");
                return false;
            }
            return true;
        }

        private static void erro(ConstraintValidatorContext ctx, String campo, String mensagem) {
            ctx.buildConstraintViolationWithTemplate(mensagem).addPropertyNode(campo).addConstraintViolation();
        }
    }
}
