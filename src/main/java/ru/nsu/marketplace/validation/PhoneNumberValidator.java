package ru.nsu.marketplace.validation;


import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import ru.nsu.marketplace.config.PhoneNumberProperties;

@RequiredArgsConstructor
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    private final PhoneNumberProperties properties;

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();


    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;

        try {
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil
                    .parse(value, null);

            if (!phoneNumberUtil.isValidNumber(phoneNumber)) return false;

            String region = phoneNumberUtil.getRegionCodeForNumber(phoneNumber);

            return properties.supportedRegions().contains(region);
        } catch (NumberParseException exception) {
            return false;
        }

    }
}
