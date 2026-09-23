package com.quillfinch.rsvpreader.io

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object TextExtractor {

    /** Extracts plain text from a .txt, .pdf or .docx content [uri]. */
    suspend fun extract(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri) ?: ""
        val name = displayName(context, uri) ?: ""
        when {
            mime.contains("pdf") || name.endsWith(".pdf", ignoreCase = true) -> extractPdf(context, uri)
            mime.contains("wordprocessingml") || name.endsWith(".docx", ignoreCase = true) -> extractDocx(context, uri)
            else -> extractPlainText(context, uri)
        }
    }

    fun displayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private fun extractPlainText(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Could not open the file")
        val utf8 = String(bytes, Charsets.UTF_8)
        val replacementChars = utf8.count { it == '\uFFFD' }
        return if (replacementChars > utf8.length / 100) String(bytes, Charsets.ISO_8859_1) else utf8
    }

    private fun extractPdf(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                val stripper = PDFTextStripper()
                stripper.sortByPosition = true
                return stripper.getText(document)
            }
        } ?: throw IllegalStateException("Could not open the PDF")
    }

    private fun extractDocx(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") return parseWordprocessingMl(zip)
                    entry = zip.nextEntry
                }
            }
        }
        throw IllegalStateException("Not a valid Word (.docx) file")
    }

    /** Pulls the text out of word/document.xml: w:t runs, with newlines at w:p boundaries. */
    private fun parseWordprocessingMl(input: InputStream): String {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(input, null)
        val text = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "p" -> text.append('\n')
                    "tab" -> text.append('\t')
                    "br" -> text.append('\n')
                    "t" -> text.append(parser.nextText())
                }
            }
            event = parser.next()
        }
        return text.toString().trim()
    }
}
