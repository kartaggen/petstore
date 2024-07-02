package com.chtrembl.petstore.product.model;

import org.springframework.stereotype.Component;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Component
@Converter(autoApply = true)
public class StatusEnumConverter implements AttributeConverter<Product.StatusEnum, String> {

    @Override
    public String convertToDatabaseColumn(Product.StatusEnum status) {
        if (status == null) {
            return null;
        }
        return status.toString();
    }

    @Override
    public Product.StatusEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return Product.StatusEnum.fromValue(dbData);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}