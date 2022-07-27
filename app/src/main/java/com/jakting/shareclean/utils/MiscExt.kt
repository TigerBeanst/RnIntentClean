package com.jakting.shareclean.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Rect
import android.net.Uri
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.alibaba.fastjson2.*
import com.jakting.shareclean.data.BackupMMKV
import com.jakting.shareclean.utils.application.Companion.kv
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.lang.reflect.Field


fun Context.openLink(url: String) {
    val uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW, uri)
    startActivity(intent)
}

fun Context.backgroundColor(@ColorRes colorRes: Int): ColorStateList =
    ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))


@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

fun getResId(resName: String, c: Class<*>): Int {
    return try {
        val idField: Field = c.getDeclaredField(resName)
        idField.getInt(idField)
    } catch (e: Exception) {
        e.printStackTrace()
        -1
    }
}

fun Context.isDarkMode(): Boolean {
    val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return currentNightMode == Configuration.UI_MODE_NIGHT_YES
}


fun Context.getPxFromDp(dp: Int): Int {
    val scale: Float = resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}

fun Context.getDpFromPx(px: Int): Int {
    val scale: Float = resources.displayMetrics.density
    return (px / scale + 0.5f).toInt()
}

fun View.isTotallyVisible(): Boolean {
    if (!isShown) {
        return false
    }
    val actualPosition = Rect()
    val isGlobalVisible = getGlobalVisibleRect(actualPosition)
    val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    val screenHeight = Resources.getSystem().displayMetrics.heightPixels
    val screen = Rect(0, 0, screenWidth, screenHeight)
    return isGlobalVisible && Rect.intersects(actualPosition, screen)
}

fun Context?.isDebug(): Boolean {
    return try {
        val info = this!!.applicationInfo
        info.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    } catch (e: java.lang.Exception) {
        false
    }
}

fun Context.backupMMKV(uri: Uri): Boolean {
    val mmkvList = mutableListOf<BackupMMKV>()
    kv.allKeys()?.forEach { key ->
        mmkvList.add(BackupMMKV(key, kv.getBoolean(key, false)))
    }
    try {
        this.contentResolver.openFileDescriptor(uri, "w")?.use { fileDescriptor ->
            FileOutputStream(fileDescriptor.fileDescriptor).use {
                it.write(mmkvList.toJSONString().toByteArray())
            }
        }
        return true
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}

fun Context.restoreMMKV(uri: Uri): Boolean {
    val stringBuilder = StringBuilder()
    try {
        this.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
    if(stringBuilder.toString().isJSONArray()){ //v2.x
        val mmkv = stringBuilder.toString().parseArray()
        mmkv.forEach {
            val backupMMKV = it as JSONObject
            kv.encode(backupMMKV.getString("mmkvKey"), backupMMKV.getBooleanValue("mmkvValue"))
        }
    }else{ //v1.x
        val mmkv = stringBuilder.toString().parseObject()
        mmkv.forEach {
            val component = it.key.substringBeforeLast("/")
            val newType = when(it.key.substringAfterLast("/")){
                "send"->"1_share"
                "send_multi"->"2_share_multi"
                "view"->"3_view"
                "text"->"4_text"
                "browser"->"5_browser"
                else-> "5_browser"
            }
            kv.encode("$newType/$component", it.value as Boolean)
        }
    }
    return true
}


