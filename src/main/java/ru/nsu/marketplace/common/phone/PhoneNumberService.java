package ru.nsu.marketplace.common.phone;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.springframework.stereotype.Service;

@Service
public class PhoneNumberService {

    private final PhoneNumberUtil phoneNumberUtil =
            PhoneNumberUtil.getInstance();

    public String normalize(String rawTelephone) {
        if (rawTelephone == null || rawTelephone.isBlank()) {
            throw new InvalidPhoneNumberException("Telephone number is required");
        }

        try {
            Phonenumber.PhoneNumber phoneNumber =
                    phoneNumberUtil.parse(rawTelephone, null);

            if (!phoneNumberUtil.isValidNumber(phoneNumber)) {
                throw new InvalidPhoneNumberException("Invalid telephone number");
            }

            return phoneNumberUtil.format(
                    phoneNumber,
                    PhoneNumberUtil.PhoneNumberFormat.E164
            );
        } catch (NumberParseException exception) {
            throw new InvalidPhoneNumberException(
                    "Could not validate telephone number",
                    exception
            );
        }
    }
}
