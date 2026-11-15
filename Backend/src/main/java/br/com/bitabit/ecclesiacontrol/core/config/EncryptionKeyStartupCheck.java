package br.com.bitabit.ecclesiacontrol.core.config;

import br.com.bitabit.ecclesiacontrol.core.security.CryptUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

// core/config/EncryptionKeyStartupCheck.java
@Component
public class EncryptionKeyStartupCheck {
    @PostConstruct
        // jakarta.annotation.PostConstruct
    void check() { CryptUtils.assertConfigured(); }
}