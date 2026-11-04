package ru.nsu.marketplace.common.phone;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    private final PhoneNumberProperties phoneNumberProperties;

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;

        try {
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil
                    .parse(value, null);

            if (!phoneNumberUtil.isValidNumber(phoneNumber)) return false;

            String region = phoneNumberUtil.getRegionCodeForNumber(phoneNumber);

            return phoneNumberProperties.supportedRegions().contains(region);
        } catch (NumberParseException exception) {
            return false;
        }

    }
}
