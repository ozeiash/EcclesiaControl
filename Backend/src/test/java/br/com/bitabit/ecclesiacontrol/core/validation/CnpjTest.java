package br.com.bitabit.ecclesiacontrol.core.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CnpjTest {

    @Test
    void acceptsNumericAndAlphanumericWithOrWithoutMask() {
        assertThat(Cnpj.isValid(Cnpj.normalize("11.222.333/0001-81"))).isTrue();
        assertThat(Cnpj.isValid(Cnpj.normalize("11222333000181"))).isTrue();
        assertThat(Cnpj.isValid(Cnpj.normalize("12.ABC.345/01DE-35"))).isTrue();   // exemplo oficial da Receita
        assertThat(Cnpj.isValid(Cnpj.normalize("12.abc.345/01de-35"))).isTrue();
    }

    @Test
    void rejectsWrongCheckDigitsRepeatedCharsAndBadShape() {
        assertThat(Cnpj.isValid(Cnpj.normalize("11.222.333/0001-82"))).isFalse();
        assertThat(Cnpj.isValid(Cnpj.normalize("00.000.000/0000-00"))).isFalse();
        assertThat(Cnpj.isValid(Cnpj.normalize("1122233300018"))).isFalse();       // 13 caracteres
        assertThat(Cnpj.isValid(Cnpj.normalize("12.ABC.345/01DE-3A"))).isFalse();   // DV com letra
    }

    @Test
    void normalizeStripsMaskAndTreatsBlankAsNull() {
        assertThat(Cnpj.normalize(" 11.222.333/0001-81 ")).isEqualTo("11222333000181");
        assertThat(Cnpj.normalize("   ")).isNull();
        assertThat(Cnpj.normalize(null)).isNull();
    }
}
