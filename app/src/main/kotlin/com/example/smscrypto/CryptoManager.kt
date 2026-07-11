package com.example.smscrypto

import android.util.Base64
import java.security.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

object CryptoManager {
    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private const val PUBLIC_PEM_HEADER = "-----BEGIN PUBLIC KEY-----"
    private const val PUBLIC_PEM_FOOTER = "-----END PUBLIC KEY-----"
    private const val PRIVATE_PEM_HEADER = "-----BEGIN PRIVATE KEY-----"
    private const val PRIVATE_PEM_FOOTER = "-----END PRIVATE KEY-----"

    data class KeyPairPEM(val privateKeyPem: String, val publicKeyPem: String)

    fun generateKeyPair(keySize: Int): KeyPairPEM {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(keySize)
        val pair = generator.generateKeyPair()
        val privatePem = toPem(pair.private.encoded, PRIVATE_PEM_HEADER, PRIVATE_PEM_FOOTER)
        val publicPem = toPem(pair.public.encoded, PUBLIC_PEM_HEADER, PUBLIC_PEM_FOOTER)
        return KeyPairPEM(privatePem, publicPem)
    }

    fun pemToBase64(pem: String): String {
        return pem
            .replace(PUBLIC_PEM_HEADER, "")
            .replace(PUBLIC_PEM_FOOTER, "")
            .replace(PRIVATE_PEM_HEADER, "")
            .replace(PRIVATE_PEM_FOOTER, "")
            .replace("\\s".toRegex(), "")
    }

    private fun toPem(der: ByteArray, header: String, footer: String): String {
        val base64 = Base64.encodeToString(der, Base64.DEFAULT)
        val sb = StringBuilder(header).append('\n')
        var start = 0
        while (start < base64.length) {
            val end = minOf(start + 64, base64.length)
            sb.append(base64.substring(start, end)).append('\n')
            start = end
        }
        sb.append(footer)
        return sb.toString()
    }

    fun encryptMessage(plainText: String, recipientPublicKeyPem: String): ByteArray {
        val pubKeyB64 = pemToBase64(recipientPublicKeyPem)
        val pubKeyBytes = Base64.decode(pubKeyB64, Base64.DEFAULT)
        val pubKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(pubKeyBytes))

        val aesKey = generateAesKey()
        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey)
        val encryptedMessage = aesCipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val iv = aesCipher.iv

        val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        rsaCipher.init(Cipher.ENCRYPT_MODE, pubKey)
        val encryptedAesKey = rsaCipher.doFinal(aesKey.encoded)

        val keySizeId: Byte = when (encryptedAesKey.size) {
            256 -> 0
            512 -> 1
            else -> throw IllegalArgumentException("Unexpected encrypted AES key length: ${encryptedAesKey.size}")
        }

        return ByteArray(1 + encryptedAesKey.size + iv.size + encryptedMessage.size).apply {
            this[0] = keySizeId
            System.arraycopy(encryptedAesKey, 0, this, 1, encryptedAesKey.size)
            System.arraycopy(iv, 0, this, 1 + encryptedAesKey.size, iv.size)
            System.arraycopy(encryptedMessage, 0, this, 1 + encryptedAesKey.size + iv.size, encryptedMessage.size)
        }
    }

    fun decryptMessage(packet: ByteArray, myPrivateKeyPem: String): String {
        val privKeyB64 = pemToBase64(myPrivateKeyPem)
        val privateKeyBytes = Base64.decode(privKeyB64, Base64.DEFAULT)
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))

        val keySizeId = packet[0]
        val encAesKeyLen = when (keySizeId.toInt()) {
            0 -> 256
            1 -> 512
            else -> throw IllegalArgumentException("Unknown key size ID: $keySizeId")
        }
        require(packet.size > 1 + encAesKeyLen + GCM_IV_LENGTH)

        val encryptedAesKey = packet.copyOfRange(1, 1 + encAesKeyLen)
        val iv = packet.copyOfRange(1 + encAesKeyLen, 1 + encAesKeyLen + GCM_IV_LENGTH)
        val cipherText = packet.copyOfRange(1 + encAesKeyLen + GCM_IV_LENGTH, packet.size)

        val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey)
        val aesKeyBytes = rsaCipher.doFinal(encryptedAesKey)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return String(aesCipher.doFinal(cipherText), Charsets.UTF_8)
    }

    private fun generateAesKey(): SecretKey {
        return KeyGenerator.getInstance("AES").apply { init(AES_KEY_SIZE) }.generateKey()
    }
}