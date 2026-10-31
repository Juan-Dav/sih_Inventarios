package com.sih_inventarios_backend.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Serializa las fechas como instantes UTC explícitos.
 *
 * {@code LocalDateTime} no lleva zona, así que al enviarlo como
 * {@code 2026-10-30T15:11:00} el navegador lo interpreta como hora local y la
 * ficha mostraba una hora adelantada. Con la {@code Z} final el navegador sabe
 * que es UTC y lo convierte a la hora del usuario.
 *
 * Spring Boot 4 usa Jackson 3, cuyo mecanismo de personalización es
 * {@link JsonMapperBuilderCustomizer} y {@link ValueSerializer}.
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Bean
    public JsonMapperBuilderCustomizer fechasEnUtc() {
        return builder -> {
            SimpleModule modulo = new SimpleModule("fechas-utc");
            modulo.addSerializer(LocalDateTime.class, new ValueSerializer<LocalDateTime>() {
                @Override
                public void serialize(
                        LocalDateTime valor,
                        JsonGenerator generador,
                        SerializationContext contexto) throws JacksonException {
                    generador.writeString(valor.atOffset(ZoneOffset.UTC).format(ISO_UTC));
                }
            });
            builder.addModule(modulo);
        };
    }
}
