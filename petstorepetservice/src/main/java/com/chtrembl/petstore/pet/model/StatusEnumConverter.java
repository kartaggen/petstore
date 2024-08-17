package com.chtrembl.petstore.pet.model;

import org.springframework.stereotype.Component;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Component
@Converter(autoApply = true)
public class StatusEnumConverter implements AttributeConverter<Pet.StatusEnum, String> {

    @Override
    public String convertToDatabaseColumn(Pet.StatusEnum status) {
        if (status == null) {
            return null;
        }
        return status.toString();
    }

    @Override
    public Pet.StatusEnum convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return Pet.StatusEnum.fromValue(dbData);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}