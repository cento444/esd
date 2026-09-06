package com.example.ui.util

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Motor ligero y ultra-rápido de generación de archivos OpenXML (.xlsx) nativos para Android.
 * Soporta múltiples hojas, estilos profesionales, fórmulas de Excel, tipos de datos,
 * formato de moneda/fechas/porcentajes, auto-filtros, congelación de paneles y anchos de columna.
 */
object XlsxEngine {

    enum class Style(val id: Int) {
        NORMAL(0),
        NORMAL_BORDER(1),
        BOLD_BORDER(2),
        HEADER_GREEN(3),
        HEADER_SLATE(4),
        CURRENCY(5),
        CURRENCY_BOLD(6),
        CURRENCY_TOTAL(7),
        WEIGHT_KG(8),
        WEIGHT_KG_BOLD(9),
        HOURS(10),
        HANEGADAS(11),
        PRICE_PER_KG(12),
        PRICE_PER_KG_BOLD(13),
        EURO_PER_HG(14),
        DATE(15),
        PERCENTAGE(16),
        PERCENTAGE_BOLD(17),
        NUMBER_DEC(18),
        NUMBER_INT(19),
        TITLE_BANNER(20),
        SECTION_HEADER(21),
        KPI_TITLE(22),
        KPI_VALUE_GREEN(23),
        ALERT_RED(24),
        OK_GREEN(25),
        STRIPED_NORMAL(26),
        STRIPED_CURRENCY(27),
        VOLUME_M3(28),
        CENTER_BORDER(29)
    }

    class Cell(
        val colIndex: Int, // 1-based (1 = A, 2 = B, etc.)
        val style: Style,
        val textValue: String? = null,
        val numValue: Double? = null,
        val formula: String? = null
    )

    class Row(
        val rowIndex: Int, // 1-based
        val height: Double? = null,
        val cells: MutableList<Cell> = mutableListOf()
    ) {
        fun addText(col: Int, text: String?, style: Style = Style.NORMAL_BORDER) {
            cells.add(Cell(col, style, textValue = text ?: ""))
        }

        fun addNum(col: Int, value: Double?, style: Style = Style.NUMBER_DEC) {
            cells.add(Cell(col, style, numValue = value ?: 0.0))
        }

        fun addFormula(col: Int, formula: String, cachedValue: Double? = null, style: Style = Style.NUMBER_DEC) {
            cells.add(Cell(col, style, formula = formula, numValue = cachedValue))
        }
    }

    class MergeCell(val fromCol: Int, val fromRow: Int, val toCol: Int, val toRow: Int)

    class Sheet(
        val name: String,
        val freezeHeader: Boolean = true,
        val columnWidths: Map<Int, Double> = emptyMap(),
        val autoFilterRange: String? = null
    ) {
        val rows = mutableListOf<Row>()
        val merges = mutableListOf<MergeCell>()

        fun newRow(rowIndex: Int, height: Double? = null): Row {
            val r = Row(rowIndex, height)
            rows.add(r)
            return r
        }

        fun addMerge(fromCol: Int, fromRow: Int, toCol: Int, toRow: Int) {
            merges.add(MergeCell(fromCol, fromRow, toCol, toRow))
        }
    }

    class Workbook {
        val sheets = mutableListOf<Sheet>()

        fun addSheet(
            name: String,
            freezeHeader: Boolean = true,
            columnWidths: Map<Int, Double> = emptyMap(),
            autoFilterRange: String? = null
        ): Sheet {
            // Excel sheet names max 31 chars and no invalid chars
            val safeName = name.replace(Regex("[:\\\\/?*\\[\\]]"), " ")
                .take(31)
                .trim()
            val sheet = Sheet(safeName, freezeHeader, columnWidths, autoFilterRange)
            sheets.add(sheet)
            return sheet
        }

        fun writeToFile(file: File) {
            FileOutputStream(file).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    val writer = OutputStreamWriter(zos, StandardCharsets.UTF_8)

                    // 1. [Content_Types].xml
                    zos.putNextEntry(ZipEntry("[Content_Types].xml"))
                    writer.write(buildContentTypesXml(sheets.size))
                    writer.flush()
                    zos.closeEntry()

                    // 2. _rels/.rels
                    zos.putNextEntry(ZipEntry("_rels/.rels"))
                    writer.write(buildRootRelsXml())
                    writer.flush()
                    zos.closeEntry()

                    // 3. xl/_rels/workbook.xml.rels
                    zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
                    writer.write(buildWorkbookRelsXml(sheets.size))
                    writer.flush()
                    zos.closeEntry()

                    // 4. xl/workbook.xml
                    zos.putNextEntry(ZipEntry("xl/workbook.xml"))
                    writer.write(buildWorkbookXml(sheets))
                    writer.flush()
                    zos.closeEntry()

                    // 5. xl/styles.xml
                    zos.putNextEntry(ZipEntry("xl/styles.xml"))
                    writer.write(buildStylesXml())
                    writer.flush()
                    zos.closeEntry()

                    // 6. xl/worksheets/sheetN.xml
                    sheets.forEachIndexed { index, sheet ->
                        zos.putNextEntry(ZipEntry("xl/worksheets/sheet${index + 1}.xml"))
                        writer.write(buildWorksheetXml(sheet))
                        writer.flush()
                        zos.closeEntry()
                    }
                }
            }
        }
    }

    fun colToLetter(colIndex: Int): String {
        var temp = colIndex
        var colLetter = ""
        while (temp > 0) {
            val mod = (temp - 1) % 26
            colLetter = (65 + mod).toChar() + colLetter
            temp = (temp - mod) / 26
        }
        return colLetter
    }

    private fun escapeXml(str: String?): String {
        if (str == null) return ""
        val sb = java.lang.StringBuilder()
        for (c in str) {
            when (c) {
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '&' -> sb.append("&amp;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> {
                    if (c.code in 0x20..0xD7FF || c == '\t' || c == '\n' || c == '\r') {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun buildContentTypesXml(sheetCount: Int): String {
        val sb = java.lang.StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        sb.append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        sb.append("""<Default Extension="xml" ContentType="application/xml"/>""")
        sb.append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        sb.append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        for (i in 1..sheetCount) {
            sb.append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        sb.append("""</Types>""")
        return sb.toString()
    }

    private fun buildRootRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
    }

    private fun buildWorkbookRelsXml(sheetCount: Int): String {
        val sb = java.lang.StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        sb.append("""<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        for (i in 1..sheetCount) {
            sb.append("""<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        sb.append("""</Relationships>""")
        return sb.toString()
    }

    private fun buildWorkbookXml(sheets: List<Sheet>): String {
        val sb = java.lang.StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        sb.append("""<bookViews><workbookView xWindow="0" yWindow="0" windowWidth="20000" windowHeight="12000" activeTab="0"/></bookViews>""")
        sb.append("""<sheets>""")
        sheets.forEachIndexed { index, sheet ->
            val rId = "rId${index + 1}"
            val sheetId = index + 1
            sb.append("""<sheet name="${escapeXml(sheet.name)}" sheetId="$sheetId" r:id="$rId"/>""")
        }
        sb.append("""</sheets>""")
        sb.append("""</workbook>""")
        return sb.toString()
    }

    private fun buildStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="11">
    <numFmt numFmtId="164" formatCode="#,##0.00\ &quot;€&quot;;-#,##0.00\ &quot;€&quot;;&quot;-&quot;\ &quot;€&quot;"/>
    <numFmt numFmtId="165" formatCode="#,##0.00\ &quot;kg&quot;"/>
    <numFmt numFmtId="166" formatCode="#,##0.00\ &quot;h&quot;"/>
    <numFmt numFmtId="167" formatCode="#,##0.00\ &quot;hg&quot;"/>
    <numFmt numFmtId="168" formatCode="#,##0.000\ &quot;€/kg&quot;"/>
    <numFmt numFmtId="169" formatCode="#,##0.00\ &quot;€/hg&quot;"/>
    <numFmt numFmtId="170" formatCode="dd/mm/yyyy"/>
    <numFmt numFmtId="171" formatCode="0.0%"/>
    <numFmt numFmtId="172" formatCode="#,##0.00"/>
    <numFmt numFmtId="173" formatCode="#,##0"/>
    <numFmt numFmtId="174" formatCode="#,##0\ &quot;m³&quot;"/>
  </numFmts>
  <fonts count="9">
    <!-- 0: Normal Regular -->
    <font><sz val="10"/><color rgb="FF1E293B"/><name val="Calibri"/><family val="2"/></font>
    <!-- 1: Normal Bold -->
    <font><b/><sz val="10"/><color rgb="FF0F172A"/><name val="Calibri"/><family val="2"/></font>
    <!-- 2: Section Title (12pt Bold Green) -->
    <font><b/><sz val="12"/><color rgb="FF14532D"/><name val="Calibri"/><family val="2"/></font>
    <!-- 3: Big Banner Title (14pt Bold White) -->
    <font><b/><sz val="14"/><color rgb="FFFFFFFF"/><name val="Calibri"/><family val="2"/></font>
    <!-- 4: Table Header (10pt Bold White) -->
    <font><b/><sz val="10"/><color rgb="FFFFFFFF"/><name val="Calibri"/><family val="2"/></font>
    <!-- 5: Big KPI Value (15pt Bold Dark Green) -->
    <font><b/><sz val="15"/><color rgb="FF15803D"/><name val="Calibri"/><family val="2"/></font>
    <!-- 6: OK Green Bold -->
    <font><b/><sz val="10"/><color rgb="FF16A34A"/><name val="Calibri"/><family val="2"/></font>
    <!-- 7: Alert Red Bold -->
    <font><b/><sz val="10"/><color rgb="FFDC2626"/><name val="Calibri"/><family val="2"/></font>
    <!-- 8: Subtitle Small Gray -->
    <font><sz val="9"/><color rgb="FF64748B"/><name val="Calibri"/><family val="2"/></font>
  </fonts>
  <fills count="10">
    <!-- 0: None -->
    <fill><patternFill patternType="none"/></fill>
    <!-- 1: Gray125 -->
    <fill><patternFill patternType="gray125"/></fill>
    <!-- 2: Primary Forest Green (Title Banner) #1E5631 -->
    <fill><patternFill patternType="solid"><fgColor rgb="FF1E5631"/></patternFill></fill>
    <!-- 3: Dark Emerald Green (Headers) #14532D -->
    <fill><patternFill patternType="solid"><fgColor rgb="FF14532D"/></patternFill></fill>
    <!-- 4: Soft Green Highlight #DCFCE7 -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFDCFCE7"/></patternFill></fill>
    <!-- 5: Soft Amber #FEF3C7 -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFFEF3C7"/></patternFill></fill>
    <!-- 6: Soft Red #FEE2E2 -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFFEE2E2"/></patternFill></fill>
    <!-- 7: Dark Slate (Headers) #1E293B -->
    <fill><patternFill patternType="solid"><fgColor rgb="FF1E293B"/></patternFill></fill>
    <!-- 8: Soft Gray Striping #F8FAFC -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFF8FAFC"/></patternFill></fill>
    <!-- 9: Soft Blue #E0F2FE -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFE0F2FE"/></patternFill></fill>
  </fills>
  <borders count="5">
    <!-- 0: None -->
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <!-- 1: Thin Gray All Around -->
    <border>
      <left style="thin"><color rgb="FFE2E8F0"/></left>
      <right style="thin"><color rgb="FFE2E8F0"/></right>
      <top style="thin"><color rgb="FFE2E8F0"/></top>
      <bottom style="thin"><color rgb="FFE2E8F0"/></bottom>
    </border>
    <!-- 2: Table Header Border -->
    <border>
      <left style="thin"><color rgb="FF334155"/></left>
      <right style="thin"><color rgb="FF334155"/></right>
      <top style="thin"><color rgb="FF334155"/></top>
      <bottom style="medium"><color rgb="FF0F172A"/></bottom>
    </border>
    <!-- 3: Double Bottom Total -->
    <border>
      <left style="thin"><color rgb="FFE2E8F0"/></left>
      <right style="thin"><color rgb="FFE2E8F0"/></right>
      <top style="thin"><color rgb="FF94A3B8"/></top>
      <bottom style="double"><color rgb="FF0F172A"/></bottom>
    </border>
    <!-- 4: KPI Card Border -->
    <border>
      <left style="medium"><color rgb="FF22C55E"/></left>
      <right style="thin"><color rgb="FF86EFAC"/></right>
      <top style="thin"><color rgb="FF86EFAC"/></top>
      <bottom style="thin"><color rgb="FF86EFAC"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="30">
    <!-- 0: NORMAL -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <!-- 1: NORMAL_BORDER -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
    <!-- 2: BOLD_BORDER -->
    <xf numFmtId="0" fontId="1" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"/>
    <!-- 3: HEADER_GREEN -->
    <xf numFmtId="0" fontId="4" fillId="3" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 4: HEADER_SLATE -->
    <xf numFmtId="0" fontId="4" fillId="7" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 5: CURRENCY -->
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 6: CURRENCY_BOLD -->
    <xf numFmtId="164" fontId="1" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 7: CURRENCY_TOTAL -->
    <xf numFmtId="164" fontId="1" fillId="4" borderId="3" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 8: WEIGHT_KG -->
    <xf numFmtId="165" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 9: WEIGHT_KG_BOLD -->
    <xf numFmtId="165" fontId="1" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 10: HOURS -->
    <xf numFmtId="166" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 11: HANEGADAS -->
    <xf numFmtId="167" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 12: PRICE_PER_KG -->
    <xf numFmtId="168" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 13: PRICE_PER_KG_BOLD -->
    <xf numFmtId="168" fontId="1" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 14: EURO_PER_HG -->
    <xf numFmtId="169" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 15: DATE -->
    <xf numFmtId="170" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 16: PERCENTAGE -->
    <xf numFmtId="171" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 17: PERCENTAGE_BOLD -->
    <xf numFmtId="171" fontId="1" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 18: NUMBER_DEC -->
    <xf numFmtId="172" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 19: NUMBER_INT -->
    <xf numFmtId="173" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 20: TITLE_BANNER -->
    <xf numFmtId="0" fontId="3" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 21: SECTION_HEADER -->
    <xf numFmtId="0" fontId="2" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="left" vertical="center"/>
    </xf>
    <!-- 22: KPI_TITLE -->
    <xf numFmtId="0" fontId="8" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 23: KPI_VALUE_GREEN -->
    <xf numFmtId="0" fontId="5" fillId="4" borderId="4" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 24: ALERT_RED -->
    <xf numFmtId="0" fontId="7" fillId="6" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 25: OK_GREEN -->
    <xf numFmtId="0" fontId="6" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- 26: STRIPED_NORMAL -->
    <xf numFmtId="0" fontId="0" fillId="8" borderId="1" xfId="0" applyFill="1" applyBorder="1"/>
    <!-- 27: STRIPED_CURRENCY -->
    <xf numFmtId="164" fontId="0" fillId="8" borderId="1" xfId="0" applyNumberFormat="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 28: VOLUME_M3 -->
    <xf numFmtId="174" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- 29: CENTER_BORDER -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
  </cellXfs>
</styleSheet>"""
    }

    private fun buildWorksheetXml(sheet: Sheet): String {
        val sb = java.lang.StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        // Freeze pane
        if (sheet.freezeHeader) {
            sb.append("""<sheetViews><sheetView tabSelected="1" workbookViewId="0"><pane ySplit="1" topLeftCell="A2" state="frozen"/></sheetView></sheetViews>""")
        }

        // Columns width
        if (sheet.columnWidths.isNotEmpty()) {
            sb.append("""<cols>""")
            sheet.columnWidths.forEach { (col, width) ->
                sb.append("""<col min="$col" max="$col" width="$width" customWidth="1"/>""")
            }
            sb.append("""</cols>""")
        }

        // Sheet Data
        sb.append("""<sheetData>""")
        sheet.rows.forEach { row ->
            val heightAttr = if (row.height != null) """ ht="${row.height}" customHeight="1"""" else ""
            sb.append("""<row r="${row.rowIndex}"$heightAttr>""")

            row.cells.forEach { cell ->
                val cellRef = "${colToLetter(cell.colIndex)}${row.rowIndex}"
                val styleId = cell.style.id

                if (cell.formula != null) {
                    sb.append("""<c r="$cellRef" s="$styleId">""")
                    sb.append("""<f>${escapeXml(cell.formula)}</f>""")
                    if (cell.numValue != null) {
                        sb.append("""<v>${cell.numValue}</v>""")
                    }
                    sb.append("""</c>""")
                } else if (cell.numValue != null) {
                    sb.append("""<c r="$cellRef" s="$styleId"><v>${cell.numValue}</v></c>""")
                } else {
                    val txt = escapeXml(cell.textValue ?: "")
                    sb.append("""<c r="$cellRef" s="$styleId" t="inlineStr"><is><t>$txt</t></is></c>""")
                }
            }
            sb.append("""</row>""")
        }
        sb.append("""</sheetData>""")

        // AutoFilter
        if (sheet.autoFilterRange != null) {
            sb.append("""<autoFilter ref="${sheet.autoFilterRange}"/>""")
        }

        // Merge Cells
        if (sheet.merges.isNotEmpty()) {
            sb.append("""<mergeCells count="${sheet.merges.size}">""")
            sheet.merges.forEach { m ->
                val ref = "${colToLetter(m.fromCol)}${m.fromRow}:${colToLetter(m.toCol)}${m.toRow}"
                sb.append("""<mergeCell ref="$ref"/>""")
            }
            sb.append("""</mergeCells>""")
        }

        sb.append("""</worksheet>""")
        return sb.toString()
    }
}
