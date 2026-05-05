package com.verbun.universalis.data.ssh

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.StringWriter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.Base64
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openss1.jcajce.JcaPEMWriter
import org.bouncycastle.openss1.jcajce.JcaPEMWriter

class SSHKeyManager(private val context: Context) {
    companion object {
        private const val KEY_ALIAS = "verbum_ssh_key"
        private const val PREFS_NAME = "verbum_ssh_prefs"
        private const val PRIVATE_KEY_KEY = "private_key"
        private const val PUBLIC_KEY_KEY = "public_key"
    }

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        PREFS_NAME,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(4096)
        val keyPair = generator.generateKeyPair()

        // Store private key
        val privateKeyPEM = convertToPEM(keyPair.private)
        encryptedPrefs.edit().putString(PRIVATE_KEY_KEY, privateKeyPEM).apply()

        // Store public key
        val publicKeyStr = "ssh-rsa ${Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)}"
        encryptedPrefs.edit().putString(PUBLIC_KEY_KEY, publicKeyStr).apply()

        return keyPair
    }

    fun getPublicKey(): String? {
        return encryptedPrefs.getString(PUBLIC_KEY_KEY, null)
    }

    fun getPrivateKey(): String? {
        return encryptedPrefs.getString(PRIVATE_KEY_KEY, null)
    }

    private fun convertToPEM(privateKey: java.security.PrivateKey): String {
        val stringWriter = StringWriter()
        val pemWriter = JcaPEMWriter(stringWriter)
        pemWriter.writeObject(privateKey)
        pemWriter.close()
        return stringWriter.toString()
    }

    fun keyExists(): Boolean {
        return encryptedPrefs.contains(PUBLIC_KEY_KEY)
    }
}
