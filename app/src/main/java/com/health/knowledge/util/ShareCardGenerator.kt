package com.health.knowledge.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.health.knowledge.domain.model.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ShareCardGenerator {

    suspend fun shareArticleAsImageCard(context: Context, article: Article) {
        val file = withContext(Dispatchers.Default) {
            renderArticleCardToFile(context, article)
        } ?: return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "🌿 ${article.title}\n\n${article.content.take(260)}...\n\n— স্বাস্থ্য জ্ঞান অ্যাপ"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "ইমেজ কার্ড শেয়ার করুন"))
    }

    private fun renderArticleCardToFile(context: Context, article: Article): File? {
        return try {
            val width = 1080
            val pad = 56f
            val innerWidth = (width - pad * 2 - 80f).toInt()

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#163A19")
                textSize = 44f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#2B332C")
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#2E7D32")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val cleanBody = if (article.content.length > 620) {
                article.content.take(620).trimEnd() + "...\n\n(বিস্তারিত পড়ুন 'স্বাস্থ্য জ্ঞান' অ্যাপে)"
            } else {
                article.content
            }

            val titleLayout = StaticLayout.Builder
                .obtain(article.title, 0, article.title.length, titlePaint, innerWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(8f, 1.15f)
                .build()

            val medsText = if (article.medicines.isNotEmpty()) {
                "🌿 ভেষজ ও উপাদান: ${article.medicines.take(6).joinToString(" • ")}"
            } else ""

            val medsLayout = if (medsText.isNotEmpty()) {
                StaticLayout.Builder
                    .obtain(medsText, 0, medsText.length, metaPaint, innerWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(4f, 1.1f)
                    .build()
            } else null

            val bodyLayout = StaticLayout.Builder
                .obtain(cleanBody, 0, cleanBody.length, bodyPaint, innerWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(10f, 1.25f)
                .build()

            val headerHeight = 165f
            val footerHeight = 120f
            val medsHeight = (medsLayout?.height ?: 0) + if (medsLayout != null) 44f else 0f
            val totalHeight = (pad * 2 + headerHeight + titleLayout.height + medsHeight + bodyLayout.height + footerHeight + 130f)
                .toInt()
                .coerceAtLeast(960)

            val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Background gradient (Herbal Deep Green to Soft Forest)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), totalHeight.toFloat(),
                    Color.parseColor("#1B5E20"),
                    Color.parseColor("#2E7D32"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), totalHeight.toFloat(), bgPaint)

            // 2. Inner Parchment/Cream Card
            val cardRect = RectF(pad, pad, width - pad, totalHeight - pad)
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FAFDF7")
            }
            canvas.drawRoundRect(cardRect, 42f, 42f, cardPaint)

            // 3. Top Header Banner inside Card
            val headerRect = RectF(pad, pad, width - pad, pad + headerHeight)
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E8F5E9")
            }
            val headerPath = Path().apply {
                addRoundRect(
                    headerRect,
                    floatArrayOf(42f, 42f, 42f, 42f, 0f, 0f, 0f, 0f),
                    Path.Direction.CW
                )
            }
            canvas.drawPath(headerPath, headerPaint)

            val brandTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1B5E20")
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val brandSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4E6E50")
                textSize = 24f
            }
            canvas.drawText("🌿 স্বাস্থ্য জ্ঞান", pad + 40f, pad + 82f, brandTitlePaint)
            val catLabel = if (article.categories.isNotEmpty()) {
                article.categories.take(3).joinToString(" • ") { it.replace("_", " ") }
            } else "প্রাকৃতিক ও আয়ুর্বেদিক স্বাস্থ্যতথ্য"
            canvas.drawText("বিভাগ: $catLabel", pad + 40f, pad + 126f, brandSubPaint)

            // 4. Draw Article Title
            var currentY = pad + headerHeight + 48f
            canvas.save()
            canvas.translate(pad + 40f, currentY)
            titleLayout.draw(canvas)
            canvas.restore()
            currentY += titleLayout.height + 28f

            // Divider line
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#C8E6C9")
                strokeWidth = 3f
            }
            canvas.drawLine(pad + 40f, currentY, width - pad - 40f, currentY, dividerPaint)
            currentY += 28f

            // 5. Draw Herbal Ingredients Box if present
            if (medsLayout != null) {
                val medsBoxRect = RectF(
                    pad + 34f,
                    currentY - 10f,
                    width - pad - 34f,
                    currentY + medsLayout.height + 18f
                )
                val medsBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F1F8E9")
                }
                canvas.drawRoundRect(medsBoxRect, 20f, 20f, medsBoxPaint)

                canvas.save()
                canvas.translate(pad + 52f, currentY + 4f)
                medsLayout.draw(canvas)
                canvas.restore()
                currentY += medsLayout.height + 42f
            }

            // 6. Draw Body Text
            canvas.save()
            canvas.translate(pad + 40f, currentY)
            bodyLayout.draw(canvas)
            canvas.restore()

            // 7. Footer Watermark
            val footerY = totalHeight - pad - 42f
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#557A58")
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(
                "📅 ${article.date}   •   ❤️ ${article.reactions} জন পছন্দ করেছেন   •   স্বাস্থ্য জ্ঞান অ্যাপ",
                pad + 40f,
                footerY,
                footerPaint
            )

            val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
            val outFile = File(dir, "health_card_${article.id}.png")
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            outFile
        } catch (e: Exception) {
            null
        }
    }
}
