/*
 * This file is part of CoralGate - https://github.com/GTeamX/CoralGate
 * Copyright (C) 2025 GTeamX (GTeam) and it's contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cloud.gteam.coralgate.packetevents.mappings.utils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class MinecraftEncryptionUtilMappings {

    public static byte[] decryptRSA(PrivateKey privateKey, byte[] data) {
        return decrypt("RSA/ECB/PKCS1Padding", privateKey, data);
    }

    public static byte[] encryptRSA(PublicKey publicKey, byte[] data) {
        return encrypt("RSA/ECB/PKCS1Padding", publicKey, data);
    }

    public static byte[] decrypt(Cipher cipher, byte[] data) {
        try {
            return cipher.doFinal(data);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            ((GeneralSecurityException)e).printStackTrace();
            return null;
        }
    }

    public static byte[] decrypt(String algorithm, PrivateKey privateKey, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(2, privateKey);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException |
                 NoSuchPaddingException ex) {
            ((GeneralSecurityException)ex).printStackTrace();
            return null;
        }
    }

    public static byte[] encrypt(String algorithm, PublicKey publicKey, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(1, publicKey);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException ex) {
            ((GeneralSecurityException)ex).printStackTrace();
            return null;
        }
    }

    public static byte[] encrypt(Cipher cipher, byte[] data) {
        return decrypt(cipher, data);
    }

    public static PublicKey publicKey(byte[] bytes) {
        try {
            EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(bytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(encodedKeySpec);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
