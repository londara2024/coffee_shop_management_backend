package com.aura.coffeeshop.common.config;

import com.aura.coffeeshop.common.persistence.CodedEnum;
import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Makes the OpenAPI docs list {@link CodedEnum} values by their wire code ({@code brewing}, {@code Bar A})
 * instead of the Java constant name, matching what the API actually sends and accepts.
 */
@Component
public class CodedEnumModelConverter implements ModelConverter {

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        Schema schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
        JavaType javaType = Json.mapper().constructType(type.getType());
        if (schema != null && javaType != null && javaType.isEnumType()
                && CodedEnum.class.isAssignableFrom(javaType.getRawClass())) {
            List<String> codes = Arrays.stream(javaType.getRawClass().getEnumConstants())
                    .map(e -> ((CodedEnum) e).getCode())
                    .toList();
            schema.setEnum(new ArrayList<>(codes));
        }
        return schema;
    }
}
