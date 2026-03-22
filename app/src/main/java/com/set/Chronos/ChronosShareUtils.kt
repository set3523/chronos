package com.set.Chronos.utils

import android.graphics.Bitmap
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.set.Chronos.AlarmSetting
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

// 1. 초경량 알람 클래스 (용량 최소화)
@Serializable
data class MinAlarm(
    val t: String, val r: Boolean, val rt: String, val v: Float,
    val c: Boolean, val tm: Boolean, val tx: String
)

// 2. 초경량 프리셋 묶음 클래스
@Serializable
data class MinPreset(
    val cn: String, val pn: String, val a: List<MinAlarm>
)

// 3. 확장 함수: 무거운 클래스 <-> 가벼운 클래스 변환
fun AlarmSetting.toMinAlarm() = MinAlarm(alarmTime, isRelative, relativeTime, volume, isCrescendo, isTtsMode, ttsText)
fun MinAlarm.toAlarmSetting() = AlarmSetting(
    alarmTime = t, isRelative = r, relativeTime = rt, volume = v,
    isCrescendo = c, isTtsMode = tm, ttsText = tx, soundUri = null // 기본음으로 초기화
)

object ChronosShareUtils {
    private const val SECRET_KEY = "ChronosSecretKey" // 16바이트(글자) 필수

    // 압축 및 암호화
    fun encryptAndCompressPayload(payload: MinPreset): String {
        return try {
            val jsonString = Json.encodeToString(payload)
            val byteStream = ByteArrayOutputStream()
            GZIPOutputStream(byteStream).bufferedWriter(Charsets.UTF_8).use { it.write(jsonString) }
            val compressedData = byteStream.toByteArray()

            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(SECRET_KEY.toByteArray(), "AES"))
            Base64.encodeToString(cipher.doFinal(compressedData), Base64.URL_SAFE or Base64.NO_WRAP)
        } catch (e: Exception) { "" }
    }

    // 복호화 및 압축 해제
    fun decryptAndDecompressPayload(encryptedBase64: String): MinPreset? {
        return try {
            val encryptedData = Base64.decode(encryptedBase64, Base64.URL_SAFE or Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(SECRET_KEY.toByteArray(), "AES"))
            val compressedData = cipher.doFinal(encryptedData)

            val jsonString = GZIPInputStream(ByteArrayInputStream(compressedData)).bufferedReader(Charsets.UTF_8).use { it.readText() }
            Json.decodeFromString<MinPreset>(jsonString)
        } catch (e: Exception) { null }
    }

    // QR 비트맵 생성
    fun generateQRBitmap(content: String, size: Int = 512): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
            bitmap
        } catch (e: Exception) { null }
    }
}