package ru.nsu.marketplace.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.springframework.stereotype.Service;
import ru.nsu.marketplace.exceptions.InvalidPhoneNumberException;

@Service
public class PhoneNumberService {

    private final PhoneNumberUtil phoneNumberUtil =
            PhoneNumberUtil.getInstance();

    public String normalize(String rawTelephone) {
        if (rawTelephone == null || rawTelephone.isBlank()) {
            throw new InvalidPhoneNumberException("Номер телефона обязателен");
        }

        try {
            Phonenumber.PhoneNumber phoneNumber =
                    phoneNumberUtil.parse(rawTelephone, null);

            if (!phoneNumberUtil.isValidNumber(phoneNumber)) {
                throw new InvalidPhoneNumberException("Невалидный номер телефона");
            }

            return phoneNumberUtil.format(
                    phoneNumber,
                    PhoneNumberUtil.PhoneNumberFormat.E164
            );
        } catch (NumberParseException exception) {
            throw new InvalidPhoneNumberException(
                    "Ошибка при проверке номера телефона",
                    exception
            );
        }
    }
}
