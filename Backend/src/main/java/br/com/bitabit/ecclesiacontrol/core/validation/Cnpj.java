package br.com.bitabit.ecclesiacontrol.core.validation;

/**
 * CNPJ numérico ou alfanumérico (Receita Federal, a partir de julho/2026).
 * Os 12 primeiros caracteres podem ser letras ou dígitos; os 2 últimos (DV) são sempre dígitos.
 * O valor de cada caractere é (código ASCII - 48), então o cálculo vale para os dois formatos.
 */
public final class Cnpj {

    private static final int[] W1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] W2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private Cnpj() {}

    /** Remove máscara (. / -) e espaços e põe em maiúsculas. Nulo ou vazio vira null. */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.replaceAll("[\\s./-]", "").toUpperCase(java.util.Locale.ROOT);
        return s.isEmpty() ? null : s;
    }

    /** Espera o valor já normalizado. */
    public static boolean isValid(String cnpj) {
        if (cnpj == null || !cnpj.matches("[A-Z0-9]{12}[0-9]{2}")) return false;
        if (cnpj.chars().distinct().count() == 1) return false;   // 00000000000000, 11111111111111...
        char d1 = digit(cnpj, 12, W1);
        char d2 = digit(cnpj.substring(0, 12) + d1, 13, W2);
        return cnpj.charAt(12) == d1 && cnpj.charAt(13) == d2;
    }

    private static char digit(String s, int len, int[] weights) {
        int sum = 0;
        for (int i = 0; i < len; i++) sum += (s.charAt(i) - '0') * weights[i];
        int r = sum % 11;
        return (char) ('0' + (r < 2 ? 0 : 11 - r));
    }
}
