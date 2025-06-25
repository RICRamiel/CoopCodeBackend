package org.ricramiel.coopeditbackend.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class EnvUtil {
    private final Environment env;

    public boolean isProduction() {
        return Arrays.asList(env.getActiveProfiles()).contains("prod");
    }
}
