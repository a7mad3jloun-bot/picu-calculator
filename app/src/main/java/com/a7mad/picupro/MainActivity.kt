package com.a7mad.picupro

import android.os.Bundle
import android.webkit.WebView
import android.provider.Settings
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.MessageDigest
import android.content.pm.PackageManager
import android.util.Base64

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isAppSignatureValid()) {
            Toast.makeText(this, "تنبيه أمني: نسخة غير أصلية!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPrefs = EncryptedSharedPreferences.create(
            "Secure_Data", masterKeyAlias, this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "000"
        val activationCode = generateFinalHash(androidId)

        if (sharedPrefs.getBoolean("activated", false)) {
            launchCalculator()
        } else {
            showActivationWindow(androidId, activationCode, sharedPrefs)
        }
    }

    private fun isAppSignatureValid(): Boolean {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in packageInfo.signatures) {
                val sha1 = MessageDigest.getInstance("SHA1").digest(signature.toByteArray())
                val currentSignature = Base64.encodeToString(sha1, Base64.NO_WRAP)
                // في النسخة النهائية ستضع بصمتك الحقيقية هنا
                return true
            }
            false
        } catch (e: Exception) { false }
    }

    private fun showActivationWindow(id: String, correct: String, prefs: android.content.SharedPreferences) {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("نظام الحماية الطبية")
            .setMessage("معرف الجهاز: $id\n\nيرجى التواصل مع أحمد القضاة للتفعيل.")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("تفعيل") { _, _ ->
                if (input.text.toString().trim() == correct) {
                    prefs.edit().putBoolean("activated", true).apply()
                    launchCalculator()
                } else {
                    finish()
                }
            }
            .show()
    }

    private fun launchCalculator() {
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.loadUrl("file:///android_asset/index.html")
        setContentView(webView)
    }

    private fun generateFinalHash(id: String): String {
        val salt = "PICU" + "2026" + "Ahmed" + "Qudah"
        val raw = id + salt
        val digest = MessageDigest.getInstance("SHA-256")
        val result = digest.digest(raw.toByteArray())
        return result.joinToString("") { "%02x".format(it) }.take(14).uppercase()
    }
}
