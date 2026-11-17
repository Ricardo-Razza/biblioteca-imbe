package br.gov.imbe.biblioteca.funcionario;

import java.security.SecureRandom;

import br.gov.imbe.biblioteca.comum.erro.RequisicaoInvalidaException;

/** Regras de senha dos usuários LOCAL. */
public final class Senhas {

    public static final int TAMANHO_MINIMO = 8;

    /** Sem caracteres que se confundem ao ditar ou ler (0/O, 1/l/I). */
    private static final String ALFABETO = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Senhas() {
    }

    /** Senha provisória de 10 caracteres, mostrada uma vez ao administrador. */
    public static String provisoria() {
        StringBuilder s = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            s.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return s.toString();
    }

    /** Nova senha: pelo menos 8 caracteres, com letra e número, e diferente do login. */
    public static void validarNova(String senha, String login) {
        if (senha == null || senha.length() < TAMANHO_MINIMO) {
            throw invalida("A senha precisa ter pelo menos " + TAMANHO_MINIMO + " caracteres.");
        }
        if (senha.length() > 72) {
            throw invalida("A senha pode ter no máximo 72 caracteres.");
        }
        if (!senha.chars().anyMatch(Character::isLetter) || !senha.chars().anyMatch(Character::isDigit)) {
            throw invalida("Use letras e números na senha.");
        }
        if (senha.equalsIgnoreCase(login)) {
            throw invalida("A senha não pode ser igual ao login.");
        }
    }

    private static RequisicaoInvalidaException invalida(String mensagem) {
        return new RequisicaoInvalidaException("Senha fraca", mensagem);
    }
}
