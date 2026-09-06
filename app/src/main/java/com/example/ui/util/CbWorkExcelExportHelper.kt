package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.MaterialItem
import com.example.data.model.MaterialsJsonHelper
import com.example.data.model.OrchardEntity
import com.example.data.model.WorkPartEntity
import com.example.ui.util.XlsxEngine.Style
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gestor de exportación a Excel (.xlsx) especializado para V&R C.B.
 * Genera el informe mensual/anual de liquidación de horas de trabajo personal,
 * maquinaria e insumos/materiales aportados por Carlos Vicente para repercutir
 * y cobrar a la sociedad V&R C.B. (participada al 50% entre ambos hermanos).
 */
object CbWorkExcelExportHelper {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Exporta el libro de liquidación completo a Excel para una comunidad de bienes o entidad
     */
    fun exportCbSettlementExcel(
        context: Context,
        periodTitle: String,
        parts: List<WorkPartEntity>,
        orchards: List<OrchardEntity>,
        laborCostTotal: Double,
        materialsCostTotal: Double,
        totalToBill: Double,
        totalHours: Double,
        entityName: String = "V&R C.B."
    ) {
        try {
            val dateStamp = fileTimestampFormat.format(Date())
            val periodClean = periodTitle.replace(" ", "_").replace("/", "-")
            val cleanEntity = entityName.replace(" ", "_").replace(".", "").replace("&", "y")
            val fileName = "Liquidacion_Trabajo_${cleanEntity}_${periodClean}_$dateStamp.xlsx"

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val file = File(exportDir, fileName)

            val wb = XlsxEngine.Workbook()
            val orchardMap = orchards.associateBy { it.id }

            // 1. Hoja de Resumen de Liquidación
            buildSummarySheet(
                wb = wb,
                periodTitle = periodTitle,
                parts = parts,
                laborCostTotal = laborCostTotal,
                materialsCostTotal = materialsCostTotal,
                totalToBill = totalToBill,
                totalHours = totalHours,
                entityName = entityName
            )

            // 2. Hoja de Detalle de Horas y Jornadas
            buildHoursSheet(wb, parts, orchardMap)

            // 3. Hoja de Insumos y Materiales Aportados
            buildMaterialsSheet(wb, parts, orchardMap)

            // 4. Hoja de Desglose por Huerto C.B.
            buildOrchardsSummarySheet(wb, parts, orchards)

            wb.writeToFile(file)

            shareXlsxFile(
                context = context,
                file = file,
                fileName = fileName,
                subject = "Liquidación V&R C.B. ($periodTitle) - Carlos Vicente"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Error al generar el informe de la C.B.: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun buildSummarySheet(
        wb: XlsxEngine.Workbook,
        periodTitle: String,
        parts: List<WorkPartEntity>,
        laborCostTotal: Double,
        materialsCostTotal: Double,
        totalToBill: Double,
        totalHours: Double,
        entityName: String = "V&R C.B."
    ) {
        val sheet = wb.addSheet(
            name = "LIQUIDACIÓN",
            freezeHeader = false,
            columnWidths = mapOf(
                1 to 4.0, 2 to 26.0, 3 to 18.0, 4 to 18.0, 5 to 22.0, 6 to 18.0
            )
        )

        // Banner principal
        val r1 = sheet.newRow(1, height = 34.0)
        r1.addText(2, "${entityName.uppercase()} • LIQUIDACIÓN DE SERVICIOS Y MATERIALES", Style.TITLE_BANNER)
        sheet.addMerge(2, 1, 6, 1)

        val r2 = sheet.newRow(2, height = 20.0)
        r2.addText(2, "Período liquidado: $periodTitle • Generado el ${dateTimeFormat.format(Date())}", Style.KPI_TITLE)
        sheet.addMerge(2, 2, 6, 2)

        // Ficha de la Sociedad
        val r4 = sheet.newRow(4, height = 22.0)
        r4.addText(2, "DATOS DE LA ENTIDAD Y CONDICIONES", Style.SECTION_HEADER)
        sheet.addMerge(2, 4, 6, 4)

        val r5 = sheet.newRow(5, height = 20.0)
        r5.addText(2, "Entidad Deudora:", Style.BOLD_BORDER)
        r5.addText(3, entityName, Style.NORMAL_BORDER)
        r5.addText(4, "CIF Sociedad:", Style.BOLD_BORDER)
        r5.addText(5, "E-98765432", Style.NORMAL_BORDER)

        val r6 = sheet.newRow(6, height = 20.0)
        r6.addText(2, "Socio Trabajador / Ejecutor:", Style.BOLD_BORDER)
        r6.addText(3, "Carlos Vicente", Style.NORMAL_BORDER)
        r6.addText(4, "Condición:", Style.BOLD_BORDER)
        r6.addText(5, "Socio Trabajador y Proveedor Insumos", Style.NORMAL_BORDER)

        val r7 = sheet.newRow(7, height = 20.0)
        r7.addText(2, "Régimen Liquidación:", Style.BOLD_BORDER)
        r7.addText(3, "Abono íntegro del 100% de horas trabajadas y materiales aportados a la C.B.", Style.NORMAL_BORDER)
        sheet.addMerge(3, 7, 6, 7)

        // SECCIÓN 2: RESUMEN ECONÓMICO A LIQUIDAR
        val r9 = sheet.newRow(9, height = 24.0)
        r9.addText(2, "RESUMEN ECONÓMICO A PERCIBIR POR CARLOS VICENTE (100%)", Style.SECTION_HEADER)
        sheet.addMerge(2, 9, 6, 9)

        val r10 = sheet.newRow(10, height = 22.0)
        r10.addText(2, "Concepto", Style.HEADER_GREEN)
        r10.addText(3, "Métrica / Unidades", Style.HEADER_GREEN)
        r10.addText(4, "Importe Neto (€)", Style.HEADER_GREEN)
        r10.addText(5, "Observaciones / Destino", Style.HEADER_GREEN)
        sheet.addMerge(5, 10, 6, 10)

        // Fila Mano de Obra
        val r11 = sheet.newRow(11, height = 20.0)
        r11.addText(2, "Mano de Obra y Faenas de Campo", Style.NORMAL_BORDER)
        r11.addNum(3, totalHours, Style.HOURS)
        r11.addNum(4, laborCostTotal, Style.CURRENCY)
        r11.addText(5, "100% horas trabajadas directamente por el socio", Style.NORMAL_BORDER)
        sheet.addMerge(5, 11, 6, 11)

        // Fila Materiales e Insumos
        val r12 = sheet.newRow(12, height = 20.0)
        r12.addText(2, "Materiales e Insumos Adelantados", Style.NORMAL_BORDER)
        val allMaterials = parts.flatMap { MaterialsJsonHelper.fromJson(it.materialsJson) }
        val adelantadosCount = allMaterials.count { it.isAdvancedByMe }
        r12.addText(3, "$adelantadosCount adel. de ${allMaterials.size} totales", Style.NORMAL_BORDER)
        r12.addNum(4, materialsCostTotal, Style.CURRENCY)
        r12.addText(5, "Reintegro de insumos pagados de su bolsillo por el socio", Style.NORMAL_BORDER)
        sheet.addMerge(5, 12, 6, 12)

        // Fila TOTAL A COBRAR
        val r13 = sheet.newRow(13, height = 24.0)
        r13.addText(2, "TOTAL A LIQUIDAR / COBRAR A LA C.B.", Style.BOLD_BORDER)
        r13.addText(3, "${parts.size} intervenciones", Style.BOLD_BORDER)
        r13.addNum(4, totalToBill, Style.CURRENCY_TOTAL)
        r13.addText(5, "IMPORTE ÍNTEGRO (100%) A TRANSFERIR AL SOCIO", Style.BOLD_BORDER)
        sheet.addMerge(5, 13, 6, 13)

        // SECCIÓN 3: PAGO Y RETRIBUCIÓN DEL TRABAJADOR (100%)
        val r15 = sheet.newRow(15, height = 22.0)
        r15.addText(2, "LIQUIDACIÓN ÍNTEGRA Y PAGO (100% A FAVOR DE CARLOS VICENTE)", Style.SECTION_HEADER)
        sheet.addMerge(2, 15, 6, 15)

        val r16 = sheet.newRow(16, height = 20.0)
        r16.addText(2, "Mano de Obra a liquidar (100%):", Style.BOLD_BORDER)
        r16.addNum(3, laborCostTotal, Style.CURRENCY)
        r16.addText(4, "Horas de faena de campo a abonar íntegramente al trabajador", Style.NORMAL_BORDER)
        sheet.addMerge(4, 16, 6, 16)

        val r17 = sheet.newRow(17, height = 20.0)
        r17.addText(2, "Materiales a reintegrar (Adelantados):", Style.BOLD_BORDER)
        r17.addNum(3, materialsCostTotal, Style.CURRENCY)
        r17.addText(4, "Reintegro de productos e insumos adelantados por el socio", Style.NORMAL_BORDER)
        sheet.addMerge(4, 17, 6, 17)

        val r18 = sheet.newRow(18, height = 22.0)
        r18.addText(2, "TOTAL A TRANSFERIR A CARLOS VICENTE:", Style.BOLD_BORDER)
        r18.addNum(3, totalToBill, Style.CURRENCY_BOLD)
        r18.addText(4, "Abono directo del 100% desde la cuenta bancaria de V&R C.B. al socio trabajador", Style.BOLD_BORDER)
        sheet.addMerge(4, 18, 6, 18)

        // SECCIÓN 4: CONFORMIDAD Y FIRMAS
        val r20 = sheet.newRow(20, height = 22.0)
        r20.addText(2, "DILIGENCIA DE CONFORMIDAD Y AUTORIZACIÓN DE TRANSFERENCIA", Style.SECTION_HEADER)
        sheet.addMerge(2, 20, 6, 20)

        val r21 = sheet.newRow(21, height = 20.0)
        r21.addText(2, "EL SOCIO TRABAJADOR (BENEFICIARIO 100%)", Style.HEADER_GREEN)
        r21.addText(3, "", Style.HEADER_GREEN)
        sheet.addMerge(2, 21, 3, 21)
        r21.addText(4, "", Style.NORMAL_BORDER)
        r21.addText(5, "POR LA SOCIEDAD V&R C.B. (ORDENANTE)", Style.HEADER_GREEN)
        r21.addText(6, "", Style.HEADER_GREEN)
        sheet.addMerge(5, 21, 6, 21)

        val r22 = sheet.newRow(22, height = 48.0)
        r22.addText(2, "Firma / Conforme:", Style.NORMAL_BORDER)
        r22.addText(3, "", Style.NORMAL_BORDER)
        sheet.addMerge(2, 22, 3, 22)
        r22.addText(4, "", Style.NORMAL_BORDER)
        r22.addText(5, "Firma / Conforme:", Style.NORMAL_BORDER)
        r22.addText(6, "", Style.NORMAL_BORDER)
        sheet.addMerge(5, 22, 6, 22)

        val r23 = sheet.newRow(23, height = 24.0)
        r23.addText(2, "Fdo. Carlos Vicente\nSocio Trabajador (Cobro 100%)", Style.BOLD_BORDER)
        r23.addText(3, "", Style.BOLD_BORDER)
        sheet.addMerge(2, 23, 3, 23)
        r23.addText(4, "", Style.NORMAL_BORDER)
        r23.addText(5, "Fdo. V&R C.B.\nAdministración - CIF: E-98765432", Style.BOLD_BORDER)
        r23.addText(6, "", Style.BOLD_BORDER)
        sheet.addMerge(5, 23, 6, 23)

        val r24 = sheet.newRow(24, height = 20.0)
        r24.addText(2, "Liquidación emitida con cargo íntegro a la cuenta corriente bancaria de la C.B.", Style.KPI_TITLE)
        sheet.addMerge(2, 24, 6, 24)

        // Filas de respiro inferior de seguridad (anti-recorte)
        sheet.newRow(25, height = 18.0)
        sheet.newRow(26, height = 18.0)
        sheet.newRow(27, height = 18.0)
    }

    private fun buildHoursSheet(
        wb: XlsxEngine.Workbook,
        parts: List<WorkPartEntity>,
        orchardMap: Map<Long, OrchardEntity>
    ) {
        val nonHarvestParts = parts.filter { !it.type.equals("produccion", ignoreCase = true) }
        val sheet = wb.addSheet(
            name = "HORAS Y JORNADAS",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 10.0, 2 to 14.0, 3 to 22.0, 4 to 16.0, 5 to 26.0,
                6 to 18.0, 7 to 12.0, 8 to 14.0, 9 to 16.0, 10 to 30.0
            ),
            autoFilterRange = "A1:J${(nonHarvestParts.size + 1).coerceAtLeast(2)}"
        )

        val r1 = sheet.newRow(1, height = 26.0)
        val headers = listOf(
            "ID Parte", "Fecha", "Huerto C.B.", "Variedad", "Faena / Tarea Realizada",
            "Operario", "Horas (h)", "Precio/h (€)", "Mano de Obra (€)", "Observaciones"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        var totalHoursSum = 0.0
        var totalLaborSum = 0.0

        nonHarvestParts.forEachIndexed { index, part ->
            val rowNum = index + 2
            val r = sheet.newRow(rowNum, height = 20.0)
            val o = orchardMap[part.orchardId]
            val date = Date(part.dateTimestamp)

            val laborCost = when {
                part.type == "varios" -> 0.0
                part.hours > 0.0 && part.pricePerHour > 0.0 -> part.hours * part.pricePerHour
                part.hours > 0.0 -> part.totalCost
                else -> 0.0
            }

            totalHoursSum += part.hours
            totalLaborSum += laborCost

            r.addText(1, "CB-${(index + 1).toString().padStart(4, '0')}", Style.CENTER_BORDER)
            r.addText(2, dateFormat.format(date), Style.DATE)
            r.addText(3, part.orchardName, Style.BOLD_BORDER)
            r.addText(4, o?.variety ?: "Cítricos", Style.NORMAL_BORDER)
            r.addText(5, part.taskName, Style.NORMAL_BORDER)
            r.addText(6, "Carlos Vicente", Style.NORMAL_BORDER)
            r.addNum(7, part.hours, Style.HOURS)
            r.addNum(8, part.pricePerHour, Style.CURRENCY)
            r.addNum(9, laborCost, Style.CURRENCY)
            r.addText(10, part.observations, Style.NORMAL_BORDER)
        }

        // Fila de Totales
        val totalRow = nonHarvestParts.size + 2
        val rTot = sheet.newRow(totalRow, height = 24.0)
        rTot.addText(1, "TOTAL", Style.BOLD_BORDER)
        sheet.addMerge(1, totalRow, 6, totalRow)
        if (nonHarvestParts.isNotEmpty()) {
            rTot.addFormula(7, "SUM(G2:G${totalRow - 1})", totalHoursSum, Style.HOURS)
            rTot.addText(8, "", Style.BOLD_BORDER)
            rTot.addFormula(9, "SUM(I2:I${totalRow - 1})", totalLaborSum, Style.CURRENCY_TOTAL)
        } else {
            rTot.addNum(7, 0.0, Style.HOURS)
            rTot.addText(8, "", Style.BOLD_BORDER)
            rTot.addNum(9, 0.0, Style.CURRENCY_TOTAL)
        }
        rTot.addText(10, "Total horas acumuladas para cobro a la C.B.", Style.BOLD_BORDER)
    }

    private fun buildMaterialsSheet(
        wb: XlsxEngine.Workbook,
        parts: List<WorkPartEntity>,
        orchardMap: Map<Long, OrchardEntity>
    ) {
        // Extraer todos los materiales
        data class MaterialRowData(
            val partId: Long,
            val date: Date,
            val orchardName: String,
            val taskName: String,
            val material: MaterialItem,
            val observations: String
        )

        val materialRows = mutableListOf<MaterialRowData>()
        parts.forEach { part ->
            val items = MaterialsJsonHelper.fromJson(part.materialsJson)
            if (items.isNotEmpty()) {
                items.forEach { item ->
                    materialRows.add(
                        MaterialRowData(
                            partId = part.id,
                            date = Date(part.dateTimestamp),
                            orchardName = part.orchardName,
                            taskName = part.taskName,
                            material = item,
                            observations = part.observations
                        )
                    )
                }
            } else if (part.type == "varios" && part.totalCost > 0.0) {
                materialRows.add(
                    MaterialRowData(
                        partId = part.id,
                        date = Date(part.dateTimestamp),
                        orchardName = part.orchardName,
                        taskName = part.taskName,
                        material = MaterialItem(
                            name = part.taskName.ifBlank { "Gasto de Material C.B." },
                            quantity = 1.0,
                            unitPrice = part.totalCost
                        ),
                        observations = part.observations
                    )
                )
            }
        }

        val sheet = wb.addSheet(
            name = "MATERIALES E INSUMOS",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 10.0, 2 to 14.0, 3 to 22.0, 4 to 22.0, 5 to 26.0,
                6 to 12.0, 7 to 10.0, 8 to 14.0, 9 to 16.0, 10 to 18.0, 11 to 28.0
            ),
            autoFilterRange = "A1:K${(materialRows.size + 1).coerceAtLeast(2)}"
        )

        val r1 = sheet.newRow(1, height = 26.0)
        val headers = listOf(
            "ID Parte", "Fecha", "Huerto C.B.", "Tarea / Faena", "Producto / Material Aportado",
            "Cantidad", "Unidad", "Precio Unitario (€)", "Coste Total (€)", "Origen de Pago", "Observaciones"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        var totalMaterialsSum = 0.0
        var totalAdelantadosSum = 0.0

        materialRows.forEachIndexed { index, m ->
            val rowNum = index + 2
            val r = sheet.newRow(rowNum, height = 20.0)
            val cost = m.material.totalCost
            totalMaterialsSum += cost
            if (m.material.isAdvancedByMe) {
                totalAdelantadosSum += cost
            }

            r.addText(1, "CB-${(index + 1).toString().padStart(4, '0')}", Style.CENTER_BORDER)
            r.addText(2, dateFormat.format(m.date), Style.DATE)
            r.addText(3, m.orchardName, Style.BOLD_BORDER)
            r.addText(4, m.taskName, Style.NORMAL_BORDER)
            r.addText(5, m.material.name, Style.NORMAL_BORDER)
            r.addNum(6, m.material.quantity, Style.NUMBER_DEC)
            r.addText(7, "uds / kg / L", Style.CENTER_BORDER)
            r.addNum(8, m.material.unitPrice, Style.CURRENCY)
            r.addNum(9, cost, Style.CURRENCY)
            r.addText(
                10,
                if (m.material.isAdvancedByMe) "Adelantado por mí" else "Cuenta C.B.",
                if (m.material.isAdvancedByMe) Style.BOLD_BORDER else Style.NORMAL_BORDER
            )
            r.addText(11, m.observations, Style.NORMAL_BORDER)
        }

        // Fila de Totales
        val totalRow = materialRows.size + 2
        val rTot = sheet.newRow(totalRow, height = 24.0)
        rTot.addText(1, "TOTAL", Style.BOLD_BORDER)
        sheet.addMerge(1, totalRow, 8, totalRow)
        if (materialRows.isNotEmpty()) {
            rTot.addFormula(9, "SUM(I2:I${totalRow - 1})", totalMaterialsSum, Style.CURRENCY_TOTAL)
        } else {
            rTot.addNum(9, 0.0, Style.CURRENCY_TOTAL)
        }
        rTot.addText(10, "Adelantado: € %.2f".format(totalAdelantadosSum), Style.BOLD_BORDER)
        rTot.addText(11, "Cuenta C.B.: € %.2f".format(totalMaterialsSum - totalAdelantadosSum), Style.NORMAL_BORDER)
    }

    private fun buildOrchardsSummarySheet(
        wb: XlsxEngine.Workbook,
        parts: List<WorkPartEntity>,
        orchards: List<OrchardEntity>
    ) {
        val sheet = wb.addSheet(
            name = "RESUMEN POR HUERTO",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 24.0, 2 to 18.0, 3 to 14.0, 4 to 14.0,
                5 to 14.0, 6 to 18.0, 7 to 18.0, 8 to 20.0, 9 to 18.0
            ),
            autoFilterRange = "A1:I${(orchards.size + 1).coerceAtLeast(2)}"
        )

        val r1 = sheet.newRow(1, height = 26.0)
        val headers = listOf(
            "Huerto V&R C.B.", "Variedad", "Hanegadas (hg)", "Intervenciones",
            "Horas Totales (h)", "Mano de Obra (€)", "Materiales (€)", "Coste Total (€)", "Coste / hg (€/hg)"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        orchards.forEachIndexed { index, o ->
            val rowNum = index + 2
            val r = sheet.newRow(rowNum, height = 20.0)

            val orchardParts = parts.filter { it.orchardId == o.id || it.orchardName.equals(o.name, ignoreCase = true) }
            val hours = orchardParts.sumOf { it.hours }

            val labor = orchardParts.sumOf { part ->
                when {
                    part.type == "varios" -> 0.0
                    part.hours > 0.0 && part.pricePerHour > 0.0 -> part.hours * part.pricePerHour
                    part.hours > 0.0 -> part.totalCost
                    else -> 0.0
                }
            }

            val materials = orchardParts.sumOf { part ->
                val items = MaterialsJsonHelper.fromJson(part.materialsJson)
                if (items.isNotEmpty()) items.sumOf { it.totalCost }
                else if (part.type == "varios" || part.hours == 0.0) part.totalCost
                else 0.0
            }

            val total = labor + materials
            val costPerHg = if (o.hanegadas > 0) total / o.hanegadas else 0.0

            r.addText(1, o.name, Style.BOLD_BORDER)
            r.addText(2, o.variety, Style.NORMAL_BORDER)
            r.addNum(3, o.hanegadas, Style.HANEGADAS)
            r.addNum(4, orchardParts.size.toDouble(), Style.NUMBER_INT)
            r.addNum(5, hours, Style.HOURS)
            r.addNum(6, labor, Style.CURRENCY)
            r.addNum(7, materials, Style.CURRENCY)
            r.addNum(8, total, Style.CURRENCY_BOLD)
            r.addNum(9, costPerHg, Style.EURO_PER_HG)
        }
    }

    private fun shareXlsxFile(context: Context, file: File, fileName: String, subject: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(
                Intent.EXTRA_TEXT,
                "Adjunto informe oficial de liquidación de trabajos y materiales para V&R C.B.: $fileName"
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Compartir Liquidación V&R C.B. (Excel)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(chooser)
        Toast.makeText(context, "Excel de la C.B. generado correctamente", Toast.LENGTH_SHORT).show()
    }

    /**
     * Genera y descarga un Excel ficticio de 1 año completo estructurado MES A MES
     * (con 12 pestañas mensuales individuales: 01-Enero a 12-Diciembre) exactamente
     * como se sacan las liquidaciones reales a final de cada mes para cobrar de la cuenta
     * bancaria de V&R C.B., más una hoja de balance anual comparativo y consolidado.
     */
    fun exportDemoOneYearCbExcel(context: Context) {
        try {
            val fileName = "Modelo_Anual_Liquidacion_VR_CB_1Ano_Mes_a_Mes.xlsx"
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val file = File(exportDir, fileName)

            val wb = XlsxEngine.Workbook()
            val operations = getDemoYearOperations()

            // 1. Hoja de Resumen Anual con Balance Comparativo de los 12 Meses
            buildDemoAnnualSummarySheet(wb, operations)

            // 2. Las 12 Pestañas Mensuales Individuales (01-Enero a 12-Diciembre)
            // Cada pestaña es la liquidación idéntica a la que se extrae a fin de mes
            val monthNames = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
            monthNames.forEachIndexed { idx, mName ->
                val mNum = idx + 1
                val mOps = operations.filter { it.monthNum == mNum }
                buildDemoMonthSheet(wb, mNum, mName, mOps)
            }

            // 3. Hoja de Detalle Consolidado de las 36 Operaciones del Año
            buildDemoOperationsSheet(wb, operations)

            // 4. Hoja de Insumos y Materiales Utilizados en el Año
            buildDemoMaterialsSheet(wb, operations)

            // 5. Hoja de Resumen por Huerto de la C.B.
            buildDemoOrchardsSheet(wb, operations)

            wb.writeToFile(file)

            shareXlsxFile(
                context = context,
                file = file,
                fileName = fileName,
                subject = "Modelo 1 Año Mes a Mes (12 Pestañas) Liquidación V&R C.B. - Carlos Vicente"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Error al generar el Excel de ejemplo anual: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Genera y descarga un Excel ficticio de UN MES INDIVIDUAL (por ejemplo, el mes que el usuario
     * tenga seleccionado), mostrando exactamente cómo queda el archivo que se saca a final de mes
     * para transferir el 100% de mano de obra y materiales desde la cuenta bancaria de la C.B.
     */
    fun exportDemoSingleMonthCbExcel(context: Context, monthNum: Int, monthName: String) {
        try {
            val fileName = "Modelo_Liquidacion_Mensual_VR_CB_${monthName}_2025.xlsx"
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val file = File(exportDir, fileName)

            val wb = XlsxEngine.Workbook()
            val operations = getDemoYearOperations()
            val monthOps = operations.filter { it.monthNum == monthNum }

            // 1. Hoja Principal de Liquidación del Mes (con formato de diligencia bancaria)
            buildDemoMonthSheet(wb, monthNum, monthName, monthOps)

            // 2. Hoja de Faenas y Horas del Mes
            val sheetHours = wb.addSheet(
                name = "HORAS Y FAENAS",
                freezeHeader = true,
                columnWidths = mapOf(1 to 8.0, 2 to 14.0, 3 to 24.0, 4 to 14.0, 5 to 30.0, 6 to 12.0, 7 to 14.0, 8 to 16.0, 9 to 36.0),
                autoFilterRange = "A1:I${monthOps.size + 1}"
            )
            val rH1 = sheetHours.newRow(1, height = 26.0)
            listOf("ID", "Fecha", "Huerto C.B.", "Variedad", "Faena Agrícola", "Horas (h)", "Precio/h (€)", "Mano Obra 100% (€)", "Observaciones Técnicas")
                .forEachIndexed { i, h -> rH1.addText(i + 1, h, Style.HEADER_GREEN) }
            monthOps.forEachIndexed { idx, op ->
                val r = sheetHours.newRow(idx + 2, height = 20.0)
                r.addNum(1, op.id.toDouble(), Style.NUMBER_INT)
                r.addText(2, op.date, Style.DATE)
                r.addText(3, op.orchard, Style.NORMAL_BORDER)
                r.addText(4, op.variety, Style.NORMAL_BORDER)
                r.addText(5, op.task, Style.NORMAL_BORDER)
                r.addNum(6, op.hours, Style.HOURS)
                r.addNum(7, op.pricePerHour, Style.CURRENCY)
                r.addNum(8, op.laborCost, Style.CURRENCY_BOLD)
                r.addText(9, op.observations, Style.NORMAL_BORDER)
            }
            // Fila Total Horas y Respiro Inferior
            val lastHoursRow = monthOps.size + 2
            val rTotHours = sheetHours.newRow(lastHoursRow, height = 24.0)
            rTotHours.addText(1, "TOTAL", Style.BOLD_BORDER)
            rTotHours.addText(2, "", Style.BOLD_BORDER)
            rTotHours.addText(3, "", Style.BOLD_BORDER)
            rTotHours.addText(4, "", Style.BOLD_BORDER)
            rTotHours.addText(5, "${monthOps.size} faenas realizadas", Style.BOLD_BORDER)
            rTotHours.addNum(6, monthOps.sumOf { it.hours }, Style.HOURS)
            rTotHours.addText(7, "", Style.BOLD_BORDER)
            rTotHours.addNum(8, monthOps.sumOf { it.laborCost }, Style.CURRENCY_TOTAL)
            rTotHours.addText(9, "Mano de obra 100% a abonar a Carlos Vicente", Style.BOLD_BORDER)
            sheetHours.newRow(lastHoursRow + 1, height = 18.0)
            sheetHours.newRow(lastHoursRow + 2, height = 18.0)
            sheetHours.newRow(lastHoursRow + 3, height = 18.0)

            // 3. Hoja de Insumos del Mes si los hay
            val monthMaterials = monthOps.filter { it.materialCost > 0.0 }
            if (monthMaterials.isNotEmpty()) {
                val sheetMat = wb.addSheet(
                    name = "INSUMOS Y PRODUCTOS",
                    freezeHeader = true,
                    columnWidths = mapOf(1 to 8.0, 2 to 14.0, 3 to 24.0, 4 to 32.0, 5 to 12.0, 6 to 12.0, 7 to 14.0, 8 to 16.0, 9 to 32.0),
                    autoFilterRange = "A1:I${monthMaterials.size + 1}"
                )
                val rM1 = sheetMat.newRow(1, height = 26.0)
                listOf("ID", "Fecha", "Huerto C.B.", "Producto / Insumo Aportado", "Cantidad", "Unidad", "Precio Unit (€)", "Total 100% (€)", "Justificación")
                    .forEachIndexed { i, h -> rM1.addText(i + 1, h, Style.HEADER_GREEN) }
                monthMaterials.forEachIndexed { idx, op ->
                    val r = sheetMat.newRow(idx + 2, height = 20.0)
                    r.addNum(1, op.id.toDouble(), Style.NUMBER_INT)
                    r.addText(2, op.date, Style.DATE)
                    r.addText(3, op.orchard, Style.NORMAL_BORDER)
                    r.addText(4, op.materialName ?: "", Style.NORMAL_BORDER)
                    r.addNum(5, op.materialQty, Style.NUMBER_INT)
                    r.addText(6, op.materialUnit, Style.NORMAL_BORDER)
                    r.addNum(7, op.materialUnitPrice, Style.CURRENCY)
                    r.addNum(8, op.materialCost, Style.CURRENCY_BOLD)
                    r.addText(9, op.observations, Style.NORMAL_BORDER)
                }
                // Fila Total Insumos y Respiro Inferior
                val lastMatRow = monthMaterials.size + 2
                val rTotMat = sheetMat.newRow(lastMatRow, height = 24.0)
                rTotMat.addText(1, "TOTAL", Style.BOLD_BORDER)
                rTotMat.addText(2, "", Style.BOLD_BORDER)
                rTotMat.addText(3, "", Style.BOLD_BORDER)
                rTotMat.addText(4, "${monthMaterials.size} productos aplicados", Style.BOLD_BORDER)
                rTotMat.addText(5, "", Style.BOLD_BORDER)
                rTotMat.addText(6, "", Style.BOLD_BORDER)
                rTotMat.addText(7, "", Style.BOLD_BORDER)
                rTotMat.addNum(8, monthMaterials.sumOf { it.materialCost }, Style.CURRENCY_TOTAL)
                rTotMat.addText(9, "Total materiales adelantados reintegrables al 100%", Style.BOLD_BORDER)
                sheetMat.newRow(lastMatRow + 1, height = 18.0)
                sheetMat.newRow(lastMatRow + 2, height = 18.0)
                sheetMat.newRow(lastMatRow + 3, height = 18.0)
            }

            wb.writeToFile(file)

            shareXlsxFile(
                context = context,
                file = file,
                fileName = fileName,
                subject = "Modelo Liquidación Mensual ($monthName 2025) V&R C.B. - Carlos Vicente"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Error al generar el ejemplo mensual: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Construye una pestaña mensual completa e independiente (ej. "01-Enero")
     * con formato oficial de orden de liquidación y pago bancario al 100%.
     */
    private fun buildDemoMonthSheet(
        wb: XlsxEngine.Workbook,
        monthNum: Int,
        monthName: String,
        monthOps: List<DemoOp>
    ) {
        val tabTitle = String.format("%02d-%s", monthNum, monthName)
        val sheet = wb.addSheet(
            name = tabTitle,
            freezeHeader = false,
            columnWidths = mapOf(
                1 to 6.0,   // ID
                2 to 13.0,  // Fecha
                3 to 25.0,  // Huerto C.B.
                4 to 14.0,  // Variedad
                5 to 28.0,  // Faena Agrícola
                6 to 10.0,  // Horas
                7 to 13.0,  // Precio/h
                8 to 15.0,  // Mano Obra 100%
                9 to 28.0,  // Insumo Aportado
                10 to 14.0, // Coste Insumo
                11 to 16.0, // Total Operación
                12 to 34.0  // Observaciones Técnicas
            )
        )

        val totalHours = monthOps.sumOf { it.hours }
        val totalLabor = monthOps.sumOf { it.laborCost }
        val totalMaterials = monthOps.sumOf { it.materialCost }
        val totalMonthBill = totalLabor + totalMaterials

        // 1. Banner Principal de la Liquidación
        val r1 = sheet.newRow(1, height = 34.0)
        r1.addText(1, "V&R AGRO C.B. • LIQUIDACIÓN MENSUAL DE TRABAJOS Y MATERIALES ($monthName.uppercase() 2025)", Style.TITLE_BANNER)
        sheet.addMerge(1, 1, 12, 1)

        val r2 = sheet.newRow(2, height = 20.0)
        r2.addText(1, "Período: $monthName 2025 • Cuenta de Cargo: C/C Bancaria V&R C.B. • Beneficiario: Carlos Vicente (100%)", Style.KPI_TITLE)
        sheet.addMerge(1, 2, 12, 2)

        // 2. Ficha Fiscal y Datos de Cargo en Cuenta
        val r4 = sheet.newRow(4, height = 22.0)
        r4.addText(1, "DATOS DE LA ENTIDAD, CUENTA BANCARIA Y BENEFICIARIO", Style.SECTION_HEADER)
        sheet.addMerge(1, 4, 12, 4)

        val r5 = sheet.newRow(5, height = 20.0)
        r5.addText(1, "Sociedad Pagadora:", Style.BOLD_BORDER)
        sheet.addMerge(1, 5, 2, 5)
        r5.addText(3, "V&R C.B. (CIF: E-98765432)", Style.NORMAL_BORDER)
        sheet.addMerge(3, 5, 4, 5)
        r5.addText(5, "Cuenta de Cargo:", Style.BOLD_BORDER)
        r5.addText(6, "Cuenta Corriente Bancaria V&R C.B.", Style.NORMAL_BORDER)
        sheet.addMerge(6, 5, 8, 5)
        r5.addText(9, "Forma de Pago:", Style.BOLD_BORDER)
        r5.addText(10, "Transferencia bancaria a Carlos Vicente", Style.NORMAL_BORDER)
        sheet.addMerge(10, 5, 12, 5)

        val r6 = sheet.newRow(6, height = 20.0)
        r6.addText(1, "Socio Trabajador:", Style.BOLD_BORDER)
        sheet.addMerge(1, 6, 2, 6)
        r6.addText(3, "Carlos Vicente (Trabajador y Proveedor Insumos)", Style.NORMAL_BORDER)
        sheet.addMerge(3, 6, 4, 6)
        r6.addText(5, "Condición de Pago:", Style.BOLD_BORDER)
        r6.addText(6, "Abono íntegro del 100% de horas trabajadas y materiales adelantados a fin de mes", Style.NORMAL_BORDER)
        sheet.addMerge(6, 6, 12, 6)

        // 3. Resumen Económico del Mes
        val r8 = sheet.newRow(8, height = 22.0)
        r8.addText(1, "RESUMEN ECONÓMICO A ABONAR DESDE LA CUENTA DE LA C.B. ($monthName.uppercase())", Style.SECTION_HEADER)
        sheet.addMerge(1, 8, 12, 8)

        val r9 = sheet.newRow(9, height = 22.0)
        r9.addText(1, "Concepto", Style.HEADER_GREEN)
        sheet.addMerge(1, 9, 3, 9)
        r9.addText(4, "Métrica / Horas", Style.HEADER_GREEN)
        sheet.addMerge(4, 9, 5, 9)
        r9.addText(6, "Mano de Obra 100% (€)", Style.HEADER_GREEN)
        sheet.addMerge(6, 9, 7, 9)
        r9.addText(8, "Insumos 100% (€)", Style.HEADER_GREEN)
        sheet.addMerge(8, 9, 9, 9)
        r9.addText(10, "TOTAL A TRANSFERIR (€)", Style.HEADER_GREEN)
        sheet.addMerge(10, 9, 12, 9)

        val r10 = sheet.newRow(10, height = 22.0)
        r10.addText(1, "Liquidación Mensual de $monthName 2025", Style.BOLD_BORDER)
        sheet.addMerge(1, 10, 3, 10)
        r10.addText(4, "$totalHours h trabajadas", Style.HOURS)
        sheet.addMerge(4, 10, 5, 10)
        r10.addNum(6, totalLabor, Style.CURRENCY_BOLD)
        sheet.addMerge(6, 10, 7, 10)
        r10.addNum(8, totalMaterials, Style.CURRENCY_BOLD)
        sheet.addMerge(8, 10, 9, 10)
        r10.addNum(10, totalMonthBill, Style.CURRENCY_TOTAL)
        sheet.addMerge(10, 10, 12, 10)

        // 4. Detalle de Operaciones y Faenas del Mes
        val r12 = sheet.newRow(12, height = 22.0)
        r12.addText(1, "DETALLE DE JORNADAS, FAENAS Y MATERIALES DEL MES ($monthName.uppercase())", Style.SECTION_HEADER)
        sheet.addMerge(1, 12, 12, 12)

        val r13 = sheet.newRow(13, height = 24.0)
        val headers = listOf(
            "ID", "Fecha", "Huerto C.B.", "Variedad", "Faena / Trabajo Agrícola",
            "Horas (h)", "Precio/h (€)", "Mano Obra (€)", "Insumo / Material Aportado",
            "Coste Mat (€)", "Total (€)", "Observaciones Técnicas"
        )
        headers.forEachIndexed { i, h -> r13.addText(i + 1, h, Style.HEADER_GREEN) }

        var curRow = 14
        monthOps.forEach { op ->
            val r = sheet.newRow(curRow, height = 20.0)
            r.addNum(1, op.id.toDouble(), Style.NUMBER_INT)
            r.addText(2, op.date, Style.DATE)
            r.addText(3, op.orchard, Style.NORMAL_BORDER)
            r.addText(4, op.variety, Style.NORMAL_BORDER)
            r.addText(5, op.task, Style.NORMAL_BORDER)
            r.addNum(6, op.hours, Style.HOURS)
            r.addNum(7, op.pricePerHour, Style.CURRENCY)
            r.addNum(8, op.laborCost, Style.CURRENCY)
            r.addText(9, op.materialName?.let { "$it (${op.materialQty.toInt()} ${op.materialUnit})" } ?: "-", Style.NORMAL_BORDER)
            r.addNum(10, op.materialCost, Style.CURRENCY)
            r.addNum(11, op.totalCost, Style.CURRENCY_BOLD)
            r.addText(12, op.observations, Style.NORMAL_BORDER)
            curRow++
        }

        // Fila Total Mes
        val rTot = sheet.newRow(curRow, height = 24.0)
        rTot.addText(1, "TOTAL MES", Style.BOLD_BORDER)
        rTot.addText(2, "", Style.BOLD_BORDER)
        rTot.addText(3, "", Style.BOLD_BORDER)
        rTot.addText(4, "", Style.BOLD_BORDER)
        rTot.addText(5, "${monthOps.size} faenas en $monthName", Style.BOLD_BORDER)
        rTot.addNum(6, totalHours, Style.HOURS)
        rTot.addText(7, "", Style.BOLD_BORDER)
        rTot.addNum(8, totalLabor, Style.CURRENCY_BOLD)
        rTot.addText(9, "", Style.BOLD_BORDER)
        rTot.addNum(10, totalMaterials, Style.CURRENCY_BOLD)
        rTot.addNum(11, totalMonthBill, Style.CURRENCY_TOTAL)
        rTot.addText(12, "Importe total a abonar por transferencia desde la C/C de la C.B.", Style.BOLD_BORDER)
        curRow += 2

        // Diligencia de firmas y conforme para el banco
        val rSignHeader = sheet.newRow(curRow, height = 22.0)
        rSignHeader.addText(1, "DILIGENCIA DE CONFORMIDAD Y AUTORIZACIÓN DE TRANSFERENCIA BANCARIA", Style.SECTION_HEADER)
        (2..12).forEach { c -> rSignHeader.addText(c, "", Style.SECTION_HEADER) }
        sheet.addMerge(1, curRow, 12, curRow)
        curRow++

        val rSignTitles = sheet.newRow(curRow, height = 20.0)
        rSignTitles.addText(1, "EL SOCIO TRABAJADOR (BENEFICIARIO 100%)", Style.HEADER_GREEN)
        (2..5).forEach { c -> rSignTitles.addText(c, "", Style.HEADER_GREEN) }
        sheet.addMerge(1, curRow, 5, curRow)
        rSignTitles.addText(6, "", Style.NORMAL_BORDER)
        rSignTitles.addText(7, "POR LA SOCIEDAD V&R C.B. (ORDENANTE)", Style.HEADER_GREEN)
        (8..12).forEach { c -> rSignTitles.addText(c, "", Style.HEADER_GREEN) }
        sheet.addMerge(7, curRow, 12, curRow)
        curRow++

        val rSignSpace = sheet.newRow(curRow, height = 48.0)
        rSignSpace.addText(1, "Firma / Conforme:", Style.NORMAL_BORDER)
        (2..5).forEach { c -> rSignSpace.addText(c, "", Style.NORMAL_BORDER) }
        sheet.addMerge(1, curRow, 5, curRow)
        rSignSpace.addText(6, "", Style.NORMAL_BORDER)
        rSignSpace.addText(7, "Firma / Conforme:", Style.NORMAL_BORDER)
        (8..12).forEach { c -> rSignSpace.addText(c, "", Style.NORMAL_BORDER) }
        sheet.addMerge(7, curRow, 12, curRow)
        curRow++

        val rSignNames = sheet.newRow(curRow, height = 24.0)
        rSignNames.addText(1, "Fdo. Carlos Vicente (Socio Trabajador)", Style.BOLD_BORDER)
        (2..5).forEach { c -> rSignNames.addText(c, "", Style.BOLD_BORDER) }
        sheet.addMerge(1, curRow, 5, curRow)
        rSignNames.addText(6, "", Style.NORMAL_BORDER)
        rSignNames.addText(7, "Fdo. V&R C.B. (Administración - CIF E-98765432)", Style.BOLD_BORDER)
        (8..12).forEach { c -> rSignNames.addText(c, "", Style.BOLD_BORDER) }
        sheet.addMerge(7, curRow, 12, curRow)
        curRow++

        val rFoot = sheet.newRow(curRow, height = 20.0)
        rFoot.addText(1, "Documento justificativo de liquidación mensual emitido para su abono en cuenta • Agro Work", Style.KPI_TITLE)
        (2..12).forEach { c -> rFoot.addText(c, "", Style.KPI_TITLE) }
        sheet.addMerge(1, curRow, 12, curRow)

        // Filas de respiro inferior de seguridad (anti-recorte)
        sheet.newRow(curRow + 1, height = 18.0)
        sheet.newRow(curRow + 2, height = 18.0)
        sheet.newRow(curRow + 3, height = 18.0)
    }

    private data class DemoOp(
        val id: Int,
        val monthNum: Int,
        val monthName: String,
        val date: String,
        val orchard: String,
        val variety: String,
        val hanegadas: Double,
        val task: String,
        val hours: Double,
        val pricePerHour: Double = 16.0,
        val materialName: String? = null,
        val materialQty: Double = 0.0,
        val materialUnit: String = "",
        val materialUnitPrice: Double = 0.0,
        val observations: String
    ) {
        val laborCost: Double get() = hours * pricePerHour
        val materialCost: Double get() = materialQty * materialUnitPrice
        val totalCost: Double get() = laborCost + materialCost
    }

    private fun getDemoYearOperations(): List<DemoOp> = listOf(
        // ENERO
        DemoOp(1, 1, "Enero", "10/01/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Poda de aclareo e iluminación", 7.5, 16.0, null, 0.0, "", 0.0, "Poda invernal en sectores 1 y 2 para abrir centros y airear copas."),
        DemoOp(2, 1, "Enero", "16/01/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Picado y triturado de leña de poda", 4.5, 16.0, null, 0.0, "", 0.0, "Pase de tractor con trituradora para incorporar restos leñosos al suelo."),
        DemoOp(3, 1, "Enero", "24/01/2025", "Huerto Els Alters (V&R C.B.)", "Navelate", 12.0, "Reparación de gomas y goteros por helada", 3.5, 16.0, "Goteros autocompensantes 4 L/h (Pack 50)", 2.0, "Pack", 19.50, "Sustitución de ramales y goteros afectados por temperaturas de -2ºC."),

        // FEBRERO
        DemoOp(4, 2, "Febrero", "05/02/2025", "Huerto Els Alters (V&R C.B.)", "Navelate", 12.0, "Poda y deschuponado de ramas viejas", 6.5, 16.0, null, 0.0, "", 0.0, "Supresión de chupones centrales y renovación de ramas envejecidas."),
        DemoOp(5, 2, "Febrero", "14/02/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Tratamiento fungicida invernal de cobre", 4.5, 16.0, "Oxicloruro de Cobre 50% WP (Saco 10 kg)", 2.0, "Saco", 39.00, "Tratamiento preventivo para desinfección de madera y control de hongos."),
        DemoOp(6, 2, "Febrero", "22/02/2025", "Huerto La Vega (V&R C.B.)", "Valencia Late", 10.0, "Descalcificación y puesta a punto cabezal", 3.5, 16.0, "Ácido Nítrico 60% desincrustante (25 kg)", 1.0, "Garrafa", 44.00, "Limpieza química de anillas de filtrado y electroválvulas de riego."),

        // MARZO
        DemoOp(7, 3, "Marzo", "06/03/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Abonado de fondo primaveral NPK", 5.0, 16.0, "Abono Complejo NPK 15-15-15 (Saco 25 kg)", 8.0, "Saco", 28.50, "Aplicación localizada al pie de árbol previo a la reactivación vegetativa."),
        DemoOp(8, 3, "Marzo", "15/03/2025", "Huerto Els Alters (V&R C.B.)", "Navelate", 12.0, "Desbroce mecánico de calles y caballones", 4.5, 16.0, null, 0.0, "", 0.0, "Pase de desbrozadora para control de vegetación adventicia de primavera."),
        DemoOp(9, 3, "Marzo", "25/03/2025", "Huerto La Vega (V&R C.B.)", "Valencia Late", 10.0, "Revisión de manómetros y presiones de red", 3.0, 16.0, "Manómetros glicerina escala 0-6 bar", 2.0, "Unid", 14.50, "Calibración de presión y ajuste de reguladores de sector."),

        // ABRIL
        DemoOp(10, 4, "Abril", "04/04/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Tratamiento foliar brotación y cuajado", 5.5, 16.0, "Bioestimulante Aminoácidos + Boro + Zinc (5 L)", 2.0, "Envase", 36.00, "Aplicación foliar con atomizador a inicio de floración y brotación."),
        DemoOp(11, 4, "Abril", "15/04/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Deschuponado manual de brotes de patrón", 6.0, 16.0, null, 0.0, "", 0.0, "Eliminación sistemática de rebrotes de patrón Citrange Carrizo."),
        DemoOp(12, 4, "Abril", "26/04/2025", "Huerto Els Alters (V&R C.B.)", "Navelate", 12.0, "Fertirrigación arrancada de primavera", 3.5, 16.0, "Nitrato Amónico Cálcico 27% (Saco 25 kg)", 6.0, "Saco", 21.00, "Aporte nitrogenado soluble repartido en 3 turnos de riego."),

        // MAYO
        DemoOp(13, 5, "Mayo", "08/05/2025", "Huerto Els Alters (V&R C.B.)", "Navelate", 12.0, "Tratamiento contra pulgón y cotonet", 5.0, 16.0, "Insecticida Spirotetramat 100 SC (1 L)", 1.0, "Litro", 82.00, "Tratamiento específico de brotes tiernos contra pulgón negro y cotonet."),
        DemoOp(14, 5, "Mayo", "17/05/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Corrección clorosis con quelato de hierro", 3.5, 16.0, "Quelato de Hierro EDDHA 6% Fe (Cubo 5 kg)", 2.0, "Cubo", 56.00, "Inyección por fertirrigación para corregir amarilleamiento foliar."),
        DemoOp(15, 5, "Mayo", "28/05/2025", "Huerto La Vega (V&R C.B.)", "Valencia Late", 10.0, "Desbroce manual de márgenes y acequia", 4.0, 16.0, null, 0.0, "", 0.0, "Limpieza perimetral de lindes para evitar refugio de roedores y plagas."),

        // JUNIO
        DemoOp(16, 6, "Junio", "05/06/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Poda en verde y aclareo para calibre", 6.5, 16.0, null, 0.0, "", 0.0, "Aclareo selectivo en copas densas para favorecer el engorde del fruto."),
        DemoOp(17, 6, "Junio", "16/06/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Colocación de trampas Mosca de la Fruta", 3.0, 16.0, "Trampas Delta + Difusor Ceratitis (Pack 20)", 1.0, "Pack", 49.00, "Monitoreo biológico oficial de capturas en caras sur."),
        DemoOp(18, 6, "Junio", "26/06/2025", "Huerto Els Alters (V&R C.B.)", "Navelate", 12.0, "Fertirrigación intensiva de engorde", 4.0, 16.0, "Nitrato Potásico 13-0-46 soluble (Saco 25 kg)", 6.0, "Saco", 33.50, "Nutrición potásica estival para engorde y grosor de cáscara."),

        // JULIO
        DemoOp(19, 7, "Julio", "04/07/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Programación riegos nocturnos por calor", 3.5, 16.0, null, 0.0, "", 0.0, "Ajuste de turnos de 23:00 a 07:00 h para evitar evaporación estival."),
        DemoOp(20, 7, "Julio", "15/07/2025", "Huerto La Vega (V&R C.B.)", "Valencia Late", 10.0, "Tratamiento acaricida contra araña roja", 4.5, 16.0, "Acaricida Hexitiazox 10% WP (Envase 1 kg)", 1.0, "Envase", 46.00, "Tratamiento localizado al detectar primeros focos en hojas."),
        DemoOp(21, 7, "Julio", "26/07/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Reparación fuga en tubería de 50 mm", 4.0, 16.0, "Manguito unión rápida PE 50 mm + Válvula", 2.0, "Kit", 28.00, "Reparación de urgencia en sector 3 por sobrepresión."),

        // AGOSTO
        DemoOp(22, 8, "Agosto", "05/08/2025", "Huerto Els Alters (V&R C.B.)", "Navelate", 12.0, "Riegos de auxilio y regulación de goteros", 4.0, 16.0, null, 0.0, "", 0.0, "Revisión de bulbos húmedos y apoyo hídrico ante ola de calor."),
        DemoOp(23, 8, "Agosto", "17/08/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Tratamiento foliar potasio y calcio", 5.0, 16.0, "Potasio neutro 50% con Carboxílicos (5 L)", 2.0, "Garrafa", 37.50, "Fortalecimiento elástico de la corteza para evitar rajado (creasing)."),
        DemoOp(24, 8, "Agosto", "27/08/2025", "Huerto La Vega (V&R C.B.)", "Valencia Late", 10.0, "Pase superficial de cultivador de muelles", 4.0, 16.0, null, 0.0, "", 0.0, "Descompactación de costra superficial tras tormentas de verano."),

        // SEPTIEMBRE
        DemoOp(25, 9, "Septiembre", "05/09/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Renovación cebo atrayente mosca fruta", 3.5, 16.0, "Atrayente líquido Ceratitis (Garrafa 5 L)", 1.0, "Garrafa", 36.00, "Recarga de mosqueros antes del cambio de color de la fruta."),
        DemoOp(26, 9, "Septiembre", "16/09/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Desbroce integral de calles previo a cosecha", 5.0, 16.0, null, 0.0, "", 0.0, "Acondicionamiento del terreno para facilitar el paso de cuadrillas."),
        DemoOp(27, 9, "Septiembre", "26/09/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Tratamiento preventivo contra el aguado", 4.5, 16.0, "Fosetil-Aluminio 80% WP (Saco 5 kg)", 2.0, "Saco", 45.00, "Pulverización fungicida de faldas bajas tras primeras lluvias de otoño."),

        // OCTUBRE
        DemoOp(28, 10, "Octubre", "06/10/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Acondicionamiento de accesos a la finca", 4.0, 16.0, null, 0.0, "", 0.0, "Nivelado con pala y retirada de piedras para entrada de camiones."),
        DemoOp(29, 10, "Octubre", "18/10/2025", "Huerto Els Alters (V&R C.B.)", "Navelate", 12.0, "Elevación y sujeción de ramales de goteo", 3.5, 16.0, "Ganchos fijación alambre tubería 16 mm", 1.0, "Bolsa", 22.00, "Alzado de mangueras para evitar daños durante el tránsito agrícola."),
        DemoOp(30, 10, "Octubre", "29/10/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Control de maduración y análisis de azúcar", 3.0, 16.0, null, 0.0, "", 0.0, "Muestreo representativo de 60 frutos con refractómetro (11.8 ºBrix)."),

        // NOVIEMBRE
        DemoOp(31, 11, "Noviembre", "07/11/2025", "Huerto Els Alters (V&R C.B.)", "Navelate", 12.0, "Descope y aclareo de ramas bajas", 4.5, 16.0, null, 0.0, "", 0.0, "Levantamiento de ramas cargadas que rozan el suelo."),
        DemoOp(32, 11, "Noviembre", "18/11/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Purga y limpieza de filtros de arena", 3.0, 16.0, null, 0.0, "", 0.0, "Contralavado y purga de decantadores del cabezal principal."),
        DemoOp(33, 11, "Noviembre", "28/11/2025", "Huerto Els Alters (V&R C.B.)", "Navelate", 12.0, "Tratamiento cicatrizante de cobre post-viento", 4.0, 16.0, "Hidróxido Cúprico 50% (Saco 5 kg)", 1.0, "Saco", 39.50, "Desinfección foliar de heridas tras temporal otoñal de viento."),

        // DICIEMBRE
        DemoOp(34, 12, "Diciembre", "05/12/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Drenaje antihielo de tuberías y válvulas", 3.5, 16.0, null, 0.0, "", 0.0, "Apertura de válvulas de purga final ante previsión de heladas."),
        DemoOp(35, 12, "Diciembre", "15/12/2025", "Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0, "Limpieza integral de caseta de riego", 4.0, 16.0, "Detergente desincrustante circuitos (10 L)", 1.0, "Garrafa", 29.00, "Limpieza de cubas de inyección de abono y engrase de bombas."),
        DemoOp(36, 12, "Diciembre", "23/12/2025", "Huerto Els Alters (V&R C.B.)", "Navelate", 12.0, "Puesta a punto tractor y atomizador C.B.", 4.5, 16.0, "Aceite motor 15W40 + Filtros recambio", 1.0, "Kit", 58.00, "Mantenimiento anual de maquinaria para la invernada.")
    )

    private fun buildDemoAnnualSummarySheet(wb: XlsxEngine.Workbook, ops: List<DemoOp>) {
        val sheet = wb.addSheet(
            name = "LIQUIDACIÓN ANUAL C.B.",
            freezeHeader = false,
            columnWidths = mapOf(
                1 to 4.0, 2 to 24.0, 3 to 18.0, 4 to 18.0, 5 to 20.0, 6 to 22.0, 7 to 18.0
            )
        )

        val totalHours = ops.sumOf { it.hours }
        val totalLabor = ops.sumOf { it.laborCost }
        val totalMaterials = ops.sumOf { it.materialCost }
        val totalBill = totalLabor + totalMaterials

        // Banner principal
        val r1 = sheet.newRow(1, height = 34.0)
        r1.addText(2, "V&R AGRO C.B. • INFORME ANUAL DE LIQUIDACIÓN (EJERCICIO COMPLETO 1 AÑO)", Style.TITLE_BANNER)
        sheet.addMerge(2, 1, 7, 1)

        val r2 = sheet.newRow(2, height = 20.0)
        r2.addText(2, "Período: Ejercicio Anual 2025 (12 Meses) • Socio Trabajador Acreedor: Carlos Vicente", Style.KPI_TITLE)
        sheet.addMerge(2, 2, 7, 2)

        // Ficha Fiscal
        val r4 = sheet.newRow(4, height = 22.0)
        r4.addText(2, "DATOS FISCALES Y CONDICIONES DE LA SOCIEDAD", Style.SECTION_HEADER)
        sheet.addMerge(2, 4, 7, 4)

        val r5 = sheet.newRow(5, height = 20.0)
        r5.addText(2, "Sociedad Deudora:", Style.BOLD_BORDER)
        r5.addText(3, "V&R C.B.", Style.NORMAL_BORDER)
        r5.addText(4, "CIF Sociedad:", Style.BOLD_BORDER)
        r5.addText(5, "E-98765432", Style.NORMAL_BORDER)
        r5.addText(6, "Actividad:", Style.BOLD_BORDER)
        r5.addText(7, "Explotación Citrícola", Style.NORMAL_BORDER)

        val r6 = sheet.newRow(6, height = 20.0)
        r6.addText(2, "Socio Trabajador:", Style.BOLD_BORDER)
        r6.addText(3, "Carlos Vicente", Style.NORMAL_BORDER)
        r6.addText(4, "Régimen Liquidación:", Style.BOLD_BORDER)
        r6.addText(5, "100% Retribución Trabajo y Materiales", Style.NORMAL_BORDER)
        sheet.addMerge(5, 6, 7, 6)

        val r7 = sheet.newRow(7, height = 20.0)
        r7.addText(2, "Condición Operativa:", Style.BOLD_BORDER)
        r7.addText(3, "Abono íntegro del 100% de horas y productos adelantados por el socio para la C.B.", Style.NORMAL_BORDER)
        sheet.addMerge(3, 7, 7, 7)

        // Resumen Anual Kpis
        val r9 = sheet.newRow(9, height = 24.0)
        r9.addText(2, "BALANCE ANUAL A PERCIBIR POR CARLOS VICENTE (100% ÍNTEGRO)", Style.SECTION_HEADER)
        sheet.addMerge(2, 9, 7, 9)

        val r10 = sheet.newRow(10, height = 22.0)
        r10.addText(2, "Concepto Anual", Style.HEADER_GREEN)
        r10.addText(3, "Métrica Anual", Style.HEADER_GREEN)
        r10.addText(4, "Mano de Obra (€)", Style.HEADER_GREEN)
        r10.addText(5, "Insumos (€)", Style.HEADER_GREEN)
        r10.addText(6, "Total Anual (€)", Style.HEADER_GREEN)
        r10.addText(7, "Condición de Cobro", Style.HEADER_GREEN)

        val r11 = sheet.newRow(11, height = 22.0)
        r11.addText(2, "Total Ejercicio 1 Año (V&R C.B.)", Style.BOLD_BORDER)
        r11.addNum(3, totalHours, Style.HOURS)
        r11.addNum(4, totalLabor, Style.CURRENCY)
        r11.addNum(5, totalMaterials, Style.CURRENCY)
        r11.addNum(6, totalBill, Style.CURRENCY_TOTAL)
        r11.addText(7, "Abono 100% por transferencia C.B.", Style.BOLD_BORDER)

        // Tabla de los 12 meses
        val r13 = sheet.newRow(13, height = 24.0)
        r13.addText(2, "DESGLOSE MENSUAL DE LIQUIDACIONES (ENERO A DICIEMBRE)", Style.SECTION_HEADER)
        sheet.addMerge(2, 13, 7, 13)

        val r14 = sheet.newRow(14, height = 22.0)
        r14.addText(2, "Mes", Style.HEADER_GREEN)
        r14.addText(3, "Nº Tareas", Style.HEADER_GREEN)
        r14.addText(4, "Horas Dedicadas (h)", Style.HEADER_GREEN)
        r14.addText(5, "Mano Obra 100% (€)", Style.HEADER_GREEN)
        r14.addText(6, "Materiales 100% (€)", Style.HEADER_GREEN)
        r14.addText(7, "Total Mensual (€)", Style.HEADER_GREEN)

        val monthNames = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
        var curRow = 15
        monthNames.forEachIndexed { idx, mName ->
            val monthOps = ops.filter { it.monthNum == idx + 1 }
            val mHours = monthOps.sumOf { it.hours }
            val mLabor = monthOps.sumOf { it.laborCost }
            val mMat = monthOps.sumOf { it.materialCost }
            val mTotal = mLabor + mMat

            val row = sheet.newRow(curRow, height = 20.0)
            row.addText(2, mName, Style.NORMAL_BORDER)
            row.addNum(3, monthOps.size.toDouble(), Style.NUMBER_INT)
            row.addNum(4, mHours, Style.HOURS)
            row.addNum(5, mLabor, Style.CURRENCY)
            row.addNum(6, mMat, Style.CURRENCY)
            row.addNum(7, mTotal, Style.CURRENCY_BOLD)
            curRow++
        }

        // Fila Total Anual Acumulado
        val rTot = sheet.newRow(curRow, height = 24.0)
        rTot.addText(2, "TOTAL ANUAL ACUMULADO", Style.BOLD_BORDER)
        rTot.addNum(3, ops.size.toDouble(), Style.NUMBER_INT)
        rTot.addNum(4, totalHours, Style.HOURS)
        rTot.addNum(5, totalLabor, Style.CURRENCY_BOLD)
        rTot.addNum(6, totalMaterials, Style.CURRENCY_BOLD)
        rTot.addNum(7, totalBill, Style.CURRENCY_TOTAL)
        curRow += 2

        // Diligencia de firmas
        val rSignHeader = sheet.newRow(curRow, height = 22.0)
        rSignHeader.addText(2, "DILIGENCIA DE CONFORMIDAD Y LIQUIDACIÓN ANUAL", Style.SECTION_HEADER)
        (3..7).forEach { c -> rSignHeader.addText(c, "", Style.SECTION_HEADER) }
        sheet.addMerge(2, curRow, 7, curRow)
        curRow++

        val rSignTitles = sheet.newRow(curRow, height = 20.0)
        rSignTitles.addText(2, "EL SOCIO TRABAJADOR (BENEFICIARIO 100%)", Style.HEADER_GREEN)
        (3..4).forEach { c -> rSignTitles.addText(c, "", Style.HEADER_GREEN) }
        sheet.addMerge(2, curRow, 4, curRow)
        rSignTitles.addText(5, "POR LA SOCIEDAD V&R C.B. (ORDENANTE)", Style.HEADER_GREEN)
        (6..7).forEach { c -> rSignTitles.addText(c, "", Style.HEADER_GREEN) }
        sheet.addMerge(5, curRow, 7, curRow)
        curRow++

        val rSignSpace = sheet.newRow(curRow, height = 48.0)
        rSignSpace.addText(2, "Firma / Conforme:", Style.NORMAL_BORDER)
        (3..4).forEach { c -> rSignSpace.addText(c, "", Style.NORMAL_BORDER) }
        sheet.addMerge(2, curRow, 4, curRow)
        rSignSpace.addText(5, "Firma / Conforme:", Style.NORMAL_BORDER)
        (6..7).forEach { c -> rSignSpace.addText(c, "", Style.NORMAL_BORDER) }
        sheet.addMerge(5, curRow, 7, curRow)
        curRow++

        val rSignNames = sheet.newRow(curRow, height = 24.0)
        rSignNames.addText(2, "Fdo. Carlos Vicente (Socio Trabajador)", Style.BOLD_BORDER)
        (3..4).forEach { c -> rSignNames.addText(c, "", Style.BOLD_BORDER) }
        sheet.addMerge(2, curRow, 4, curRow)
        rSignNames.addText(5, "Fdo. V&R C.B. (Administración - CIF E-98765432)", Style.BOLD_BORDER)
        (6..7).forEach { c -> rSignNames.addText(c, "", Style.BOLD_BORDER) }
        sheet.addMerge(5, curRow, 7, curRow)
        curRow++

        val rFoot = sheet.newRow(curRow, height = 20.0)
        rFoot.addText(2, "Certificado anual de liquidaciones y pagos emitido para constancia bancaria y contable • Agro Work", Style.KPI_TITLE)
        (3..7).forEach { c -> rFoot.addText(c, "", Style.KPI_TITLE) }
        sheet.addMerge(2, curRow, 7, curRow)

        // Filas de respiro inferior de seguridad
        sheet.newRow(curRow + 1, height = 18.0)
        sheet.newRow(curRow + 2, height = 18.0)
        sheet.newRow(curRow + 3, height = 18.0)
    }

    private fun buildDemoOperationsSheet(wb: XlsxEngine.Workbook, ops: List<DemoOp>) {
        val sheet = wb.addSheet(
            name = "DETALLE 1 AÑO (36 TAREAS)",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 8.0, 2 to 14.0, 3 to 14.0, 4 to 24.0, 5 to 14.0,
                6 to 30.0, 7 to 16.0, 8 to 12.0, 9 to 14.0, 10 to 16.0,
                11 to 32.0, 12 to 16.0, 13 to 16.0, 14 to 36.0
            ),
            autoFilterRange = "A1:N${ops.size + 1}"
        )

        val r1 = sheet.newRow(1, height = 26.0)
        val headers = listOf(
            "ID", "Fecha", "Mes", "Huerto C.B.", "Variedad",
            "Faena / Trabajo Agrícola", "Operario", "Horas (h)", "Precio/h (€)", "Mano Obra (€)",
            "Material / Insumo Aportado", "Coste Mat (€)", "Total (€)", "Observaciones Técnicas"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        ops.forEachIndexed { index, op ->
            val r = sheet.newRow(index + 2, height = 20.0)
            r.addNum(1, op.id.toDouble(), Style.NUMBER_INT)
            r.addText(2, op.date, Style.DATE)
            r.addText(3, op.monthName, Style.NORMAL_BORDER)
            r.addText(4, op.orchard, Style.NORMAL_BORDER)
            r.addText(5, op.variety, Style.NORMAL_BORDER)
            r.addText(6, op.task, Style.NORMAL_BORDER)
            r.addText(7, "Carlos Vicente", Style.NORMAL_BORDER)
            r.addNum(8, op.hours, Style.HOURS)
            r.addNum(9, op.pricePerHour, Style.CURRENCY)
            r.addNum(10, op.laborCost, Style.CURRENCY)
            r.addText(11, op.materialName?.let { "$it (${op.materialQty.toInt()} ${op.materialUnit})" } ?: "-", Style.NORMAL_BORDER)
            r.addNum(12, op.materialCost, Style.CURRENCY)
            r.addNum(13, op.totalCost, Style.CURRENCY_BOLD)
            r.addText(14, op.observations, Style.NORMAL_BORDER)
        }

        // Fila Total al final
        val lastOpRow = ops.size + 2
        val totalRow = sheet.newRow(lastOpRow, height = 24.0)
        totalRow.addText(1, "TOTAL ANUAL", Style.BOLD_BORDER)
        totalRow.addText(2, "", Style.BOLD_BORDER)
        totalRow.addText(3, "", Style.BOLD_BORDER)
        totalRow.addText(4, "", Style.BOLD_BORDER)
        totalRow.addText(5, "", Style.BOLD_BORDER)
        totalRow.addText(6, "${ops.size} operaciones", Style.BOLD_BORDER)
        totalRow.addText(7, "", Style.BOLD_BORDER)
        totalRow.addNum(8, ops.sumOf { it.hours }, Style.HOURS)
        totalRow.addText(9, "", Style.BOLD_BORDER)
        totalRow.addNum(10, ops.sumOf { it.laborCost }, Style.CURRENCY_BOLD)
        totalRow.addText(11, "", Style.BOLD_BORDER)
        totalRow.addNum(12, ops.sumOf { it.materialCost }, Style.CURRENCY_BOLD)
        totalRow.addNum(13, ops.sumOf { it.totalCost }, Style.CURRENCY_TOTAL)
        totalRow.addText(14, "Liquidación 100% de mano de obra y materiales a Carlos Vicente", Style.BOLD_BORDER)

        sheet.newRow(lastOpRow + 1, height = 18.0)
        sheet.newRow(lastOpRow + 2, height = 18.0)
        sheet.newRow(lastOpRow + 3, height = 18.0)
    }

    private fun buildDemoMaterialsSheet(wb: XlsxEngine.Workbook, ops: List<DemoOp>) {
        val matOps = ops.filter { it.materialCost > 0.0 }
        val sheet = wb.addSheet(
            name = "INSUMOS Y MATERIALES (1 AÑO)",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 8.0, 2 to 14.0, 3 to 14.0, 4 to 24.0, 5 to 34.0,
                6 to 12.0, 7 to 12.0, 8 to 14.0, 9 to 16.0, 10 to 36.0
            ),
            autoFilterRange = "A1:J${matOps.size + 1}"
        )

        val r1 = sheet.newRow(1, height = 26.0)
        val headers = listOf(
            "ID", "Fecha", "Mes", "Huerto C.B.", "Producto / Insumo Aportado",
            "Cantidad", "Unidad", "Precio Unit (€)", "Total (€)", "Destino / Justificación Técnica"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        matOps.forEachIndexed { index, op ->
            val r = sheet.newRow(index + 2, height = 20.0)
            r.addNum(1, op.id.toDouble(), Style.NUMBER_INT)
            r.addText(2, op.date, Style.DATE)
            r.addText(3, op.monthName, Style.NORMAL_BORDER)
            r.addText(4, op.orchard, Style.NORMAL_BORDER)
            r.addText(5, op.materialName ?: "", Style.NORMAL_BORDER)
            r.addNum(6, op.materialQty, Style.NUMBER_INT)
            r.addText(7, op.materialUnit, Style.NORMAL_BORDER)
            r.addNum(8, op.materialUnitPrice, Style.CURRENCY)
            r.addNum(9, op.materialCost, Style.CURRENCY_BOLD)
            r.addText(10, op.observations, Style.NORMAL_BORDER)
        }

        // Fila Total
        val lastMatRow = matOps.size + 2
        val totalRow = sheet.newRow(lastMatRow, height = 24.0)
        totalRow.addText(1, "TOTAL INSUMOS", Style.BOLD_BORDER)
        totalRow.addText(2, "", Style.BOLD_BORDER)
        totalRow.addText(3, "", Style.BOLD_BORDER)
        totalRow.addText(4, "", Style.BOLD_BORDER)
        totalRow.addText(5, "${matOps.size} productos aportados", Style.BOLD_BORDER)
        totalRow.addText(6, "", Style.BOLD_BORDER)
        totalRow.addText(7, "", Style.BOLD_BORDER)
        totalRow.addText(8, "", Style.BOLD_BORDER)
        totalRow.addNum(9, matOps.sumOf { it.materialCost }, Style.CURRENCY_TOTAL)
        totalRow.addText(10, "Total materiales e insumos reintegrables al 100% por la C.B.", Style.BOLD_BORDER)

        sheet.newRow(lastMatRow + 1, height = 18.0)
        sheet.newRow(lastMatRow + 2, height = 18.0)
        sheet.newRow(lastMatRow + 3, height = 18.0)
    }

    private fun buildDemoOrchardsSheet(wb: XlsxEngine.Workbook, ops: List<DemoOp>) {
        val sheet = wb.addSheet(
            name = "RESUMEN POR HUERTO (1 AÑO)",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 26.0, 2 to 16.0, 3 to 14.0, 4 to 14.0, 5 to 14.0,
                6 to 16.0, 7 to 16.0, 8 to 18.0, 9 to 16.0
            ),
            autoFilterRange = "A1:I4"
        )

        val r1 = sheet.newRow(1, height = 26.0)
        val headers = listOf(
            "Huerto de la C.B.", "Variedad", "Superficie (hg)", "Operaciones", "Horas (h)",
            "Mano de Obra (€)", "Materiales (€)", "Coste Total Anual (€)", "Coste / Hanegada (€/hg)"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        val orchards = listOf(
            Triple("Huerto San Jaime (V&R C.B.)", "Clemenules", 18.0),
            Triple("Huerto Els Alters (V&R C.B.)", "Navelate", 12.0),
            Triple("Huerto La Vega (V&R C.B.)", "Valencia Late", 10.0)
        )

        orchards.forEachIndexed { index, (name, variety, hg) ->
            val orchardOps = ops.filter { it.orchard == name }
            val hours = orchardOps.sumOf { it.hours }
            val labor = orchardOps.sumOf { it.laborCost }
            val mat = orchardOps.sumOf { it.materialCost }
            val total = labor + mat
            val costPerHg = if (hg > 0) total / hg else 0.0

            val r = sheet.newRow(index + 2, height = 20.0)
            r.addText(1, name, Style.BOLD_BORDER)
            r.addText(2, variety, Style.NORMAL_BORDER)
            r.addNum(3, hg, Style.HANEGADAS)
            r.addNum(4, orchardOps.size.toDouble(), Style.NUMBER_INT)
            r.addNum(5, hours, Style.HOURS)
            r.addNum(6, labor, Style.CURRENCY)
            r.addNum(7, mat, Style.CURRENCY)
            r.addNum(8, total, Style.CURRENCY_BOLD)
            r.addNum(9, costPerHg, Style.EURO_PER_HG)
        }

        // Fila Total C.B.
        val lastOrchardRow = orchards.size + 2
        val totalRow = sheet.newRow(lastOrchardRow, height = 24.0)
        totalRow.addText(1, "TOTAL PARCELAS V&R C.B.", Style.BOLD_BORDER)
        totalRow.addText(2, "Cítricos Variados", Style.BOLD_BORDER)
        totalRow.addNum(3, orchards.sumOf { it.third }, Style.HANEGADAS)
        totalRow.addNum(4, ops.size.toDouble(), Style.NUMBER_INT)
        totalRow.addNum(5, ops.sumOf { it.hours }, Style.HOURS)
        totalRow.addNum(6, ops.sumOf { it.laborCost }, Style.CURRENCY_BOLD)
        totalRow.addNum(7, ops.sumOf { it.materialCost }, Style.CURRENCY_BOLD)
        totalRow.addNum(8, ops.sumOf { it.totalCost }, Style.CURRENCY_TOTAL)
        val avgPerHg = ops.sumOf { it.totalCost } / orchards.sumOf { it.third }
        totalRow.addNum(9, avgPerHg, Style.EURO_PER_HG)

        sheet.newRow(lastOrchardRow + 1, height = 18.0)
        sheet.newRow(lastOrchardRow + 2, height = 18.0)
        sheet.newRow(lastOrchardRow + 3, height = 18.0)
    }
}
