/*
 * Copyright (C) 2014 Team GRIT
 * 
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package util.mailer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * @author <a href="mailto:fabian.marquart@uni-konstanz.de">Fabian Marquart</a>
 */

public class EncryptorDecryptor {

    private static final Path SECRETKEYDESTINATION = Paths.get("config",
            "secretKey.txt");

    private byte[] m_key;

    /**
     * Instantiates a new {@link EncryptorDecryptor}.
     * 
     * @throws IOException
     *             if the secretkey can not be read
     */
    public EncryptorDecryptor() throws IOException {
        m_key = Files.readAllBytes(SECRETKEYDESTINATION);
    }

    /**
     * Encrypts a given string.
     * 
     * @param stringToEncrypt
     *            : this is the string that shall be encrypted.
     * @return : the encrypted String or null in case of fail. : when
     *         encrypting fails.
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public String encrypt(String stringToEncrypt)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        // get cryptographic cipher with the requested transformation, AES
        // (advanced encryption standard) algorithm with ECB (electronic
        // code book) mode and PKCS5Padding (schema to pad cleartext to be
        // multiples of 8-byte blocks) padding.
        Cipher cipher;

        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

        // use the provided secret key for encryption using AES
        final SecretKeySpec secretKey = new SecretKeySpec(m_key, "AES");

        // initialize cryptographic cipher with encryption mode
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // encode the encrypted string in base 64
        final String encryptedString = Base64.encodeBase64String(cipher
                .doFinal(stringToEncrypt.getBytes()));
        return encryptedString;
    }

    /**
     * Decrypts a given string.
     * 
     * @param stringToDecrypt
     *            : this is the string that shall be decrypted.
     * @return : the decrypted string or null when failed. : when decrypting
     *         fails.
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public String decrypt(String stringToDecrypt)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        // get instance of cryptographic cipher using AES
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");

        // use the provided secret key for decryption using AES
        final SecretKeySpec secretKey = new SecretKeySpec(m_key, "AES");

        // initialize cryptographic cipher with decryption mode
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // decrypt the string encoded in base 64
        final String decryptedString = new String(cipher.doFinal(Base64
                .decodeBase64(stringToDecrypt)));
        return decryptedString;
    }
}