package com.leadflow.app.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.leadflow.app.data.Lead
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Turns a list of leads into the bytes of a CSV, XLSX or PDF file. */
object Exporter {
    private val HEADERS = listOf("Business Name", "Address", "Phone", "Email", "Website", "Notes", "Date Added")

    private fun date(ms: Long): String =
        SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.ENGLISH).format(Date(ms))

    private fun row(l: Lead) = listOf(l.businessName, l.address, l.phone, l.email, l.website, l.notes, date(l.createdAt))

    // ---------------------------------------------------------------- CSV
    fun toCsv(leads: List<Lead>): ByteArray {
        fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        val sb = StringBuilder("\uFEFF") // BOM so Excel reads Tamil/Unicode correctly
        sb.append(HEADERS.joinToString(",") { q(it) }).append("\r\n")
        leads.forEach { l -> sb.append(row(l).joinToString(",") { q(it) }).append("\r\n") }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    // --------------------------------------------------------------- XLSX
    // A .xlsx file is a ZIP of small XML files. Writing it directly is reliable on
    // Android and adds no library weight (stock Apache POI does not run well on Android).
    fun toXlsx(leads: List<Lead>): ByteArray {
        fun esc(s: String) = s.filter { it.code >= 32 || it == '\n' || it == '\t' }
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

        fun cell(col: Char, rowNum: Int, text: String, style: Int) =
            "<c r=\"$col$rowNum\" t=\"inlineStr\" s=\"$style\"><is><t xml:space=\"preserve\">${esc(text)}</t></is></c>"

        fun rowXml(rowNum: Int, values: List<String>, style: Int) =
            "<row r=\"$rowNum\">" +
                values.mapIndexed { i, v -> cell('A' + i, rowNum, v, style) }.joinToString("") +
                "</row>"

        val sheet = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            append("""<cols><col min="1" max="1" width="30" customWidth="1"/><col min="2" max="2" width="40" customWidth="1"/><col min="3" max="3" width="16" customWidth="1"/><col min="4" max="5" width="30" customWidth="1"/><col min="6" max="6" width="40" customWidth="1"/><col min="7" max="7" width="18" customWidth="1"/></cols>""")
            append("<sheetData>")
            append(rowXml(1, HEADERS, 1))
            leads.forEachIndexed { i, l -> append(rowXml(i + 2, row(l), 0)) }
            append("</sheetData></worksheet>")
        }

        val xmlHead = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            fun put(name: String, content: String) {
                z.putNextEntry(ZipEntry(name))
                z.write(content.toByteArray(Charsets.UTF_8))
                z.closeEntry()
            }
            put("[Content_Types].xml", xmlHead +
                """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>""")
            put("_rels/.rels", xmlHead +
                """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            put("xl/workbook.xml", xmlHead +
                """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Leads" sheetId="1" r:id="rId1"/></sheets></workbook>""")
            put("xl/_rels/workbook.xml.rels", xmlHead +
                """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>""")
            put("xl/styles.xml", xmlHead +
                """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts><fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>""")
            put("xl/worksheets/sheet1.xml", sheet)
        }
        return bos.toByteArray()
    }

    // ---------------------------------------------------------------- PDF
    private fun fit(p: Paint, s: String, width: Float): String {
        val clean = s.replace("\n", " ")
        val n = p.breakText(clean, true, width, null)
        return if (n < clean.length) clean.take(maxOf(0, n - 1)) + "…" else clean
    }

    fun toPdf(leads: List<Lead>): ByteArray {
        val pageW = 842 // A4 landscape in points
        val pageH = 595
        val margin = 30f
        val colX = floatArrayOf(30f, 155f, 300f, 390f, 510f, 635f, 755f)
        val colW = floatArrayOf(118f, 138f, 82f, 112f, 118f, 112f, 55f)
        val title = Paint().apply { textSize = 16f; isFakeBoldText = true }
        val head = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val body = Paint().apply { textSize = 10f }

        val doc = PdfDocument()
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var pageNo = 0
        var y = 0f

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
            page = p
            val c = p.canvas
            canvas = c
            c.drawText("LeadFlow - Leads", margin, 40f, title)
            y = 70f
            HEADERS.forEachIndexed { i, h -> c.drawText(h, colX[i], y, head) }
            y += 8f
            c.drawLine(margin, y, pageW - margin, y, body)
            y += 18f
        }

        newPage()
        leads.forEach { l ->
            if (y > pageH - 40) newPage()
            row(l).forEachIndexed { i, t ->
                canvas!!.drawText(fit(body, t, colW[i] - 8f), colX[i], y, body)
            }
            y += 20f
        }
        page?.let { doc.finishPage(it) }

        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }
}
