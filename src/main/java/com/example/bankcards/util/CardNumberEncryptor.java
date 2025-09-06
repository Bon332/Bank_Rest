package com.example.bankcards.util;

import com.example.bankcards.config.EncryptionProperties;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Convert(converter = CardNumberEncryptor.class)
@Component
@RequiredArgsConstructor
public class CardNumberEncryptor implements AttributeConverter<String, String> {

    private final EncryptionProperties properties;

    private Cipher initCipher(int mode) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(
                    properties.getAesKey().getBytes(), "AES"
            );
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(mode, secretKey);
            return cipher;
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка инициализации AES", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            Cipher cipher = initCipher(Cipher.ENCRYPT_MODE);
            byte[] encrypted = cipher.doFinal(attribute.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка шифрования", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            Cipher cipher = initCipher(Cipher.DECRYPT_MODE);
            byte[] decoded = Base64.getDecoder().decode(dbData);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка дешифрования", e);
        }
    }
}
