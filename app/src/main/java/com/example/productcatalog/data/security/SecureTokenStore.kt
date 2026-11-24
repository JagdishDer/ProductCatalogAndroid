
package com.example.productcatalog.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureTokenStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(
            "secure_auth",
            Context.MODE_PRIVATE
        )

    private val keyStore = KeyStore.getInstance(
        "AndroidKeyStore"
    ).apply {
        load(null)
    }

    private fun getOrCreateKey(): SecretKey {
        val existing = keyStore.getKey(
            KEY_ALIAS,
            null
        ) as? SecretKey

        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .build()
        )

        return generator.generateKey()
    }

    @Synchronized
    fun saveToken(token: String) {
        require(token.isNotBlank())

        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateKey()
        )

        val encrypted = cipher.doFinal(
            token.toByteArray(Charsets.UTF_8)
        )

        val combined = cipher.iv + encrypted

        val encoded = Base64.encodeToString(
            combined,
            Base64.NO_WRAP
        )

        check(
            prefs.edit()
                .putString(TOKEN_KEY, encoded)
                .commit()
        ) {
            "Unable to save encrypted token"
        }
    }

    @Synchronized
    fun readToken(): String? {
        val saved = prefs.getString(
            TOKEN_KEY,
            null
        ) ?: return null

        return try {
            val bytes = Base64.decode(
                saved,
                Base64.DEFAULT
            )

            require(bytes.size > IV_LENGTH)

            val iv = bytes.copyOfRange(
                0,
                IV_LENGTH
            )

            val encrypted = bytes.copyOfRange(
                IV_LENGTH,
                bytes.size
            )

            val cipher = Cipher.getInstance(
                "AES/GCM/NoPadding"
            )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, iv)
            )

            String(
                cipher.doFinal(encrypted),
                Charsets.UTF_8
            )
        } catch (_: Exception) {
            prefs.edit().remove(TOKEN_KEY).commit()
            null
        }
    }

    @Synchronized
    fun clearToken() {
        check(
            prefs.edit()
                .remove(TOKEN_KEY)
                .commit()
        ) {
            "Unable to remove stored token"
        }
    }

    companion object {
        private const val KEY_ALIAS =
            "product_catalog_auth_key"

        private const val TOKEN_KEY =
            "access_token"

        private const val IV_LENGTH = 12
    }
}
