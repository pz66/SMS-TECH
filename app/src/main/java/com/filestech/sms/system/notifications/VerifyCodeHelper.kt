package com.filestech.sms.system.notifications

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast


private val VERIFY_CODE_REGEX = Regex("(?<!\\d)(\\d{4,8})(?!\\d)")
private val KEYWORD_COMBINED_REGEX = Regex(
    "验证.?码|校验.?码|注册.?码|动态.?密码|动态.?码|动态.?口令|安全.?码|确认.?码|随机.?码|verify|code|verification",
    RegexOption.IGNORE_CASE
)

object VerifyCodeHelper {
    fun extractVerificationCode(text: String): String? {
        val keywordPositions = KEYWORD_COMBINED_REGEX.findAll(text)
            .map { it.range.first }
            .toList()
        if (keywordPositions.isEmpty()) return null

        for (pos in keywordPositions) {
            val start = maxOf(0, pos - 80)
            val end = minOf(text.length, pos + 80)
            val windowText = text.substring(start, end)
            val keywordPosInWindow = pos - start

            val candidates = VERIFY_CODE_REGEX.findAll(windowText)
                .map { match ->
                    val code = match.groupValues[1]
                    Pair(code, match.range.first)
                }
                .toList()

            if (candidates.isEmpty()) continue

            val best = candidates
                .minByOrNull { (_, offset) ->
                    val distance = offset - keywordPosInWindow
                    if (distance >= 0) distance else Int.MAX_VALUE / 2 + distance
                }
            if (best != null) return best.first
        }
        return null
    }
    private fun copyToClipboard(ctx: Context, text: String) {
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("验证码", text))
    }
    fun copyToClipboardAndToast(context: Context, text: String) {
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            copyToClipboard(context, text)
            Toast.makeText(context, "已复制$text", Toast.LENGTH_SHORT).show()
        }
    }
}
