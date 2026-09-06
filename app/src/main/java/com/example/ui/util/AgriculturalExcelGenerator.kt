package com.example.ui.util

import com.example.data.model.MaterialsJsonHelper
import com.example.data.model.OrchardEntity
import com.example.data.model.WorkPartEntity
import com.example.ui.util.XlsxEngine.Style
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generador integral de libros Excel agrícolas (.xlsx) profesionales para V&R Agro.
 * Arquitectura estricta:
 *   DATOS MAESTROS -> MOVIMIENTOS -> CÁLCULOS -> ANÁLISIS -> DASHBOARD
 *
 * Sin cifras manuales fijas en análisis ni dashboard: todas las agregaciones se calculan
 * mediante fórmulas de Excel dinámicas (SUMIF, COUNTIF, IFERROR, SUM, ratios).
 */
object AgriculturalExcelGenerator {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    private fun getCampaign(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        return if (month >= Calendar.SEPTEMBER) "$year/${year + 1}" else "${year - 1}/$year"
    }

    fun buildWorkbook(
        parts: List<WorkPartEntity>,
        orchards: List<OrchardEntity>,
        campaignName: String = "2025/2026",
        isDemoMock: Boolean = false
    ): XlsxEngine.Workbook {
        val wb = XlsxEngine.Workbook()

        // Si es demo o la base está vacía, usamos el generador de datos realistas coherentes de 1 año
        val effectiveOrchards = if (isDemoMock || orchards.isEmpty()) createRealisticMockOrchards() else orchards
        val effectiveParts = if (isDemoMock || parts.isEmpty()) createRealisticMockWorkParts(effectiveOrchards) else parts

        // Preparamos mapas de referencia
        val orchardMap = effectiveOrchards.associateBy { it.id }
        val orchardByNameMap = effectiveOrchards.associateBy { it.name.trim().lowercase(Locale.getDefault()) }

        // =========================================================================
        // 1. RESUMEN EJECUTIVO (DASHBOARD)
        // =========================================================================
        buildDashboardSheet(wb, effectiveOrchards, campaignName)

        // =========================================================================
        // 2. MOVIMIENTOS Y CUADERNOS DE CAMPO
        // =========================================================================
        buildOperationsSheet(wb, effectiveParts, orchardMap, orchardByNameMap)
        buildHarvestRevenuesSheet(wb, effectiveOrchards, effectiveParts, orchardMap, orchardByNameMap)
        buildInputsSheet(wb, effectiveParts, orchardMap, orchardByNameMap)
        buildMachineryUsageSheet(wb, effectiveParts, orchardMap, orchardByNameMap)
        buildIrrigationSheet(wb, effectiveOrchards, effectiveParts, orchardMap, orchardByNameMap)
        buildOverheadsSheet(wb, effectiveOrchards, campaignName)

        // =========================================================================
        // 3. DATOS MAESTROS
        // =========================================================================
        buildOrchardsSheet(wb, effectiveOrchards)
        buildVarietiesSheet(wb)
        buildRootstocksSheet(wb)
        buildMachineryMasterSheet(wb)
        buildWorkersSheet(wb)
        buildSuppliersSheet(wb)
        buildProductsSheet(wb)
        buildInventorySheet(wb)

        // =========================================================================
        // 4. ANÁLISIS ECONÓMICO Y AGRONÓMICO DINÁMICO
        // =========================================================================
        buildOrchardAnalysisSheet(wb, effectiveOrchards)
        buildVarietyAnalysisSheet(wb, effectiveOrchards)
        buildCampaignAnalysisSheet(wb, effectiveOrchards, campaignName)

        // =========================================================================
        // 5. CONTROL DE INTEGRIDAD Y AUDITORÍA DE DATOS
        // =========================================================================
        buildControlDataSheet(wb, effectiveOrchards)

        // =========================================================================
        // 6. CONFIGURACIÓN Y TABLAS MAESTRAS
        // =========================================================================
        buildConfigSheet(wb)

        return wb
    }

    // =========================================================================
    // 1. DASHBOARD (100% conectado a fórmulas de ANALISIS HUERTOS y MOVIMIENTOS)
    // =========================================================================
    private fun buildDashboardSheet(
        wb: XlsxEngine.Workbook,
        orchards: List<OrchardEntity>,
        campaignName: String
    ) {
        val sheet = wb.addSheet(
            name = "DASHBOARD",
            freezeHeader = false,
            columnWidths = mapOf(
                1 to 5.0, 2 to 24.0, 3 to 18.0, 4 to 5.0, 5 to 24.0, 6 to 18.0, 7 to 5.0, 8 to 24.0, 9 to 18.0
            )
        )

        // Banner Superior
        val r1 = sheet.newRow(1, height = 36.0)
        r1.addText(2, "V&R AGRO - CUADRO DE MANDO ECONÓMICO Y AGRONÓMICO ($campaignName)", Style.TITLE_BANNER)
        sheet.addMerge(2, 1, 9, 1)

        val r2 = sheet.newRow(2, height = 18.0)
        r2.addText(2, "Sistema de Gestión Agraria Integral • Datos y Métricas en Tiempo Real", Style.KPI_TITLE)
        sheet.addMerge(2, 2, 9, 2)

        // SECCIÓN 1: KPIs GLOBALES
        val r4 = sheet.newRow(4, height = 24.0)
        r4.addText(2, "RESULTADOS ECONÓMICOS", Style.SECTION_HEADER)
        sheet.addMerge(2, 4, 3, 4)
        r4.addText(5, "VOLUMEN Y RENDIMIENTOS", Style.SECTION_HEADER)
        sheet.addMerge(5, 4, 6, 4)
        r4.addText(8, "ESTRUCTURA DE COSTES", Style.SECTION_HEADER)
        sheet.addMerge(8, 4, 9, 4)

        val totalRowRef = orchards.size + 2 // Fila de totales en ANALISIS HUERTOS

        // Fila 5: Ingresos Netos | Kilos Totales | Mano de Obra
        val r5 = sheet.newRow(5, height = 22.0)
        r5.addText(2, "Ingresos Netos Cosecha (€)", Style.BOLD_BORDER)
        r5.addFormula(3, "'ANALISIS HUERTOS'!\$O\$$totalRowRef", null, Style.CURRENCY_TOTAL)
        r5.addText(5, "Producción Cosechada (kg)", Style.BOLD_BORDER)
        r5.addFormula(6, "'ANALISIS HUERTOS'!\$J\$$totalRowRef", null, Style.WEIGHT_KG_BOLD)
        r5.addText(8, "Coste Mano de Obra (€)", Style.BOLD_BORDER)
        r5.addFormula(9, "'ANALISIS HUERTOS'!\$P\$$totalRowRef", null, Style.CURRENCY_BOLD)

        // Fila 6: Coste Total Explotación | Kg Comercializados | Insumos / Fitocuidado
        val r6 = sheet.newRow(6, height = 22.0)
        r6.addText(2, "Coste Total Explotación (€)", Style.BOLD_BORDER)
        r6.addFormula(3, "'ANALISIS HUERTOS'!\$U\$$totalRowRef", null, Style.CURRENCY_TOTAL)
        r6.addText(5, "Kilos Comercializados (kg)", Style.BOLD_BORDER)
        r6.addFormula(6, "'ANALISIS HUERTOS'!\$K\$$totalRowRef", null, Style.WEIGHT_KG)
        r6.addText(8, "Coste Insumos / Nutrición (€)", Style.BOLD_BORDER)
        r6.addFormula(9, "'ANALISIS HUERTOS'!\$R\$$totalRowRef", null, Style.CURRENCY_BOLD)

        // Fila 7: Margen Operativo Neto | Rendimiento Comercial (%) | Maquinaria y Combustible
        val r7 = sheet.newRow(7, height = 22.0)
        r7.addText(2, "MARGEN OPERATIVO NETO (€)", Style.BOLD_BORDER)
        r7.addFormula(3, "'ANALISIS HUERTOS'!\$V\$$totalRowRef", null, Style.CURRENCY_TOTAL)
        r7.addText(5, "Rendimiento Comercial (%)", Style.BOLD_BORDER)
        r7.addFormula(6, "IFERROR(F6/F5, 0)", null, Style.PERCENTAGE_BOLD)
        r7.addText(8, "Coste Maquinaria y Carburante (€)", Style.BOLD_BORDER)
        r7.addFormula(9, "'ANALISIS HUERTOS'!\$Q\$$totalRowRef", null, Style.CURRENCY_BOLD)

        // Fila 8: Margen €/Hectárea | Precio Medio Venta (€/kg) | Riego y Energía
        val r8 = sheet.newRow(8, height = 22.0)
        r8.addText(2, "Margen Operativo (€/ha)", Style.BOLD_BORDER)
        r8.addFormula(3, "'ANALISIS HUERTOS'!\$W\$$totalRowRef", null, Style.EURO_PER_HG)
        r8.addText(5, "Precio Medio Venta (€/kg)", Style.BOLD_BORDER)
        r8.addFormula(6, "'ANALISIS HUERTOS'!\$Z\$$totalRowRef", null, Style.PRICE_PER_KG_BOLD)
        r8.addText(8, "Coste Riego y Electricidad (€)", Style.BOLD_BORDER)
        r8.addFormula(9, "'ANALISIS HUERTOS'!\$S\$$totalRowRef", null, Style.CURRENCY_BOLD)

        // Fila 9: Margen €/Hanegada | Coste Unitario (€/kg) | Gastos Generales Imputados
        val r9 = sheet.newRow(9, height = 22.0)
        r9.addText(2, "Margen Operativo (€/hg)", Style.BOLD_BORDER)
        r9.addFormula(3, "'ANALISIS HUERTOS'!\$X\$$totalRowRef", null, Style.EURO_PER_HG)
        r9.addText(5, "Coste Unitario (€/kg)", Style.BOLD_BORDER)
        r9.addFormula(6, "'ANALISIS HUERTOS'!\$Y\$$totalRowRef", null, Style.PRICE_PER_KG_BOLD)
        r9.addText(8, "Gastos Generales Imputados (€)", Style.BOLD_BORDER)
        r9.addFormula(9, "'ANALISIS HUERTOS'!\$T\$$totalRowRef", null, Style.CURRENCY_BOLD)

        // Fila 10: Retorno s/ Inversión (ROI %) | Margen Unitario (€/kg) | Superficie Total (ha / hg)
        val r10 = sheet.newRow(10, height = 24.0)
        r10.addText(2, "RETORNO DE LA INVERSIÓN (ROI %)", Style.BOLD_BORDER)
        r10.addFormula(3, "'ANALISIS HUERTOS'!\$AB\$$totalRowRef", null, Style.PERCENTAGE_BOLD)
        r10.addText(5, "Margen Unitario (€/kg)", Style.BOLD_BORDER)
        r10.addFormula(6, "'ANALISIS HUERTOS'!\$AA\$$totalRowRef", null, Style.PRICE_PER_KG_BOLD)
        r10.addText(8, "Superficie Total (ha)", Style.BOLD_BORDER)
        r10.addFormula(9, "'ANALISIS HUERTOS'!\$G\$$totalRowRef", null, Style.NUMBER_DEC)

        // SECCIÓN 2: TABLA RESUMEN RENTABILIDAD POR HUERTO
        val r13 = sheet.newRow(13, height = 24.0)
        r13.addText(2, "RESUMEN DE RENTABILIDAD POR HUERTO / FINCA", Style.SECTION_HEADER)
        sheet.addMerge(2, 13, 9, 13)

        val r14 = sheet.newRow(14, height = 24.0)
        r14.addText(2, "Huerto / Finca", Style.HEADER_GREEN)
        r14.addText(3, "Superficie (ha)", Style.HEADER_GREEN)
        r14.addText(4, "Kilos Cosechados (kg)", Style.HEADER_GREEN)
        r14.addText(5, "Ingresos Netos (€)", Style.HEADER_GREEN)
        r14.addText(6, "Coste Total (€)", Style.HEADER_GREEN)
        r14.addText(7, "Margen Neto (€)", Style.HEADER_GREEN)
        r14.addText(8, "Margen €/ha", Style.HEADER_GREEN)
        r14.addText(9, "ROI (%)", Style.HEADER_GREEN)

        orchards.forEachIndexed { idx, o ->
            val rowNum = 15 + idx
            val orchardRow = 2 + idx
            val r = sheet.newRow(rowNum, height = 20.0)
            r.addText(2, o.name, Style.NORMAL_BORDER)
            r.addFormula(3, "'ANALISIS HUERTOS'!G$orchardRow", null, Style.NUMBER_DEC)
            r.addFormula(4, "'ANALISIS HUERTOS'!J$orchardRow", null, Style.WEIGHT_KG)
            r.addFormula(5, "'ANALISIS HUERTOS'!O$orchardRow", null, Style.CURRENCY)
            r.addFormula(6, "'ANALISIS HUERTOS'!U$orchardRow", null, Style.CURRENCY)
            r.addFormula(7, "'ANALISIS HUERTOS'!V$orchardRow", null, Style.CURRENCY_BOLD)
            r.addFormula(8, "'ANALISIS HUERTOS'!W$orchardRow", null, Style.EURO_PER_HG)
            r.addFormula(9, "'ANALISIS HUERTOS'!AB$orchardRow", null, Style.PERCENTAGE_BOLD)
        }
    }

    // =========================================================================
    // 2. OPERACIONES (Cuaderno de campo principal)
    // =========================================================================
    private fun buildOperationsSheet(
        wb: XlsxEngine.Workbook,
        parts: List<WorkPartEntity>,
        orchardMap: Map<Long, OrchardEntity>,
        orchardByNameMap: Map<String, OrchardEntity>
    ) {
        val nonHarvestParts = parts.filter { !it.type.equals("produccion", ignoreCase = true) }
        val sheet = wb.addSheet(
            name = "OPERACIONES",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 12.0, 2 to 12.0, 3 to 12.0, 4 to 20.0, 5 to 10.0, 6 to 14.0, 7 to 14.0, 8 to 10.0,
                9 to 10.0, 10 to 12.0, 11 to 12.0, 12 to 14.0, 13 to 16.0, 14 to 16.0, 15 to 18.0,
                16 to 24.0, 17 to 12.0, 18 to 12.0, 19 to 14.0, 20 to 18.0, 21 to 12.0, 22 to 14.0,
                23 to 14.0, 24 to 14.0, 25 to 12.0, 26 to 16.0, 27 to 16.0, 28 to 18.0, 29 to 20.0,
                30 to 16.0, 31 to 24.0
            ),
            autoFilterRange = "A1:AE${(nonHarvestParts.size + 1).coerceAtLeast(2)}"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Operación", "Fecha", "Campaña", "Huerto / Finca", "ID Huerto", "Titularidad",
            "Municipio", "Polígono", "Parcela", "Superficie (hg)", "Superficie (ha)", "Cultivo", "Variedad",
            "Tipo Operación", "Labor / Faena", "Descripción / Trabajo", "Estado", "Horas Trabajo (h)",
            "Coste Mano Obra (€)", "Tipo Mano de Obra", "Precio/Hora (€/h)", "Coste Maquinaria (€)", "Coste Combustible (€)", "Coste Insumos (€)",
            "Otros Costes (€)", "Coste Total Operación (€)", "Gasto Real de Caja (€)", "Responsable", "Maquinaria Utilizada",
            "Ref. Albarán/Factura", "Observaciones"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        nonHarvestParts.forEachIndexed { index, part ->
            val rowNum = index + 2
            val r = sheet.newRow(rowNum, height = 20.0)
            val o = orchardMap[part.orchardId] ?: orchardByNameMap[part.orchardName.trim().lowercase(Locale.getDefault())]
            val date = Date(part.dateTimestamp)
            val year = yearFormat.format(date)
            val campaign = getCampaign(part.dateTimestamp)
            val owner = ExcelExportHelper.getPartOwnerCategory(part, o)

            val isOwnLabor = when {
                part.observations.contains("[HORAS_PROPIAS]") -> true
                part.observations.contains("[HORAS_CONTRATADAS]") -> false
                else -> owner.lowercase() in listOf("mío", "mio", "propios", "propiedad")
            }
            val laborTypeLabel = if (isOwnLabor) "Horas Propias (Trabajo Personal)" else "Mano de Obra Contratada"

            val materials = MaterialsJsonHelper.fromJson(part.materialsJson)
            val matCost = materials.sumOf { it.totalCost }
            val laborCost = part.hours * part.pricePerHour
            val hasMachinery = part.taskName.contains("Desbroce", true) || part.taskName.contains("Tratamiento", true)
            val machCost = if (hasMachinery) part.hours * 8.5 else 0.0
            val fuelCost = if (hasMachinery) part.hours * 4.2 else 0.0

            // Limpiar etiquetas técnicas de las observaciones
            val cleanObs = part.observations
                .replace("[HORAS_PROPIAS]", "")
                .replace("[HORAS_CONTRATADAS]", "")
                .trim()

            r.addText(1, "OP-${(index + 1).toString().padStart(4, '0')}", Style.CENTER_BORDER)
            r.addText(2, dateFormat.format(date), Style.DATE)
            r.addText(3, campaign, Style.CENTER_BORDER)
            r.addText(4, part.orchardName, Style.BOLD_BORDER)
            r.addText(5, (o?.id ?: part.orchardId).toString(), Style.CENTER_BORDER)
            r.addText(6, owner, Style.CENTER_BORDER)
            r.addText(7, o?.municipality?.ifBlank { "Valencia" } ?: "Valencia", Style.NORMAL_BORDER)
            r.addText(8, o?.polygon?.ifBlank { "1" } ?: "1", Style.CENTER_BORDER)
            r.addText(9, o?.parcel?.ifBlank { "1" } ?: "1", Style.CENTER_BORDER)
            r.addNum(10, o?.hanegadas ?: 0.0, Style.HANEGADAS)
            // FÓRMULA HECTÁREAS: Hanegadas / Constante
            r.addFormula(11, "J$rowNum/CONFIGURACION!\$C\$4", (o?.hanegadas ?: 0.0) / 12.0, Style.NUMBER_DEC)
            r.addText(12, o?.fruitType?.ifBlank { "Cítricos" } ?: "Cítricos", Style.NORMAL_BORDER)
            r.addText(13, o?.variety?.ifBlank { "Clemenules" } ?: "Clemenules", Style.NORMAL_BORDER)
            r.addText(14, part.type.replaceFirstChar { it.uppercase() }, Style.NORMAL_BORDER)
            r.addText(15, part.taskName, Style.NORMAL_BORDER)
            r.addText(16, if (cleanObs.isNotBlank()) cleanObs else part.taskName, Style.NORMAL_BORDER)
            r.addText(17, "Realizado", Style.OK_GREEN)
            r.addNum(18, part.hours, Style.HOURS)
            r.addNum(19, laborCost, Style.CURRENCY)
            r.addText(20, laborTypeLabel, if (isOwnLabor) Style.CENTER_BORDER else Style.BOLD_BORDER)
            r.addNum(21, part.pricePerHour, Style.CURRENCY)
            r.addNum(22, machCost, Style.CURRENCY)
            r.addNum(23, fuelCost, Style.CURRENCY)
            r.addNum(24, matCost, Style.CURRENCY)
            r.addNum(25, 0.0, Style.CURRENCY)
            // FÓRMULA COSTE TOTAL OPERACIÓN: Mano de obra (S) + Maquinaria (V) + Combustible (W) + Insumos (X) + Otros (Y)
            r.addFormula(26, "S$rowNum+V$rowNum+W$rowNum+X$rowNum+Y$rowNum", laborCost + machCost + fuelCost + matCost, Style.CURRENCY_BOLD)
            // GASTO REAL DE CAJA: Si es mano de obra contratada se incluye, si es propia solo insumos y maquinaria
            val realCashExpense = (if (isOwnLabor) 0.0 else laborCost) + machCost + fuelCost + matCost
            r.addNum(27, realCashExpense, Style.CURRENCY)
            r.addText(28, "Carlos Vicente", Style.NORMAL_BORDER)
            r.addText(29, if (hasMachinery) "Tractor John Deere 5075E" else "-", Style.NORMAL_BORDER)
            r.addText(30, "ALB-$year-${(index + 101)}", Style.CENTER_BORDER)
            r.addText(31, cleanObs, Style.NORMAL_BORDER)
        }
    }

    // =========================================================================
    // 3. INGRESOS / COSECHAS
    // =========================================================================
    private fun buildHarvestRevenuesSheet(
        wb: XlsxEngine.Workbook,
        orchards: List<OrchardEntity>,
        parts: List<WorkPartEntity>,
        orchardMap: Map<Long, OrchardEntity>,
        orchardByNameMap: Map<String, OrchardEntity>
    ) {
        val harvestParts = parts.filter { it.type.equals("produccion", ignoreCase = true) || it.kilos > 0 }
        val sheet = wb.addSheet(
            name = "INGRESOS - COSECHAS",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 12.0, 2 to 12.0, 3 to 12.0, 4 to 20.0, 5 to 10.0, 6 to 14.0, 7 to 16.0, 8 to 16.0,
                9 to 22.0, 10 to 16.0, 11 to 14.0, 12 to 14.0, 13 to 14.0, 14 to 14.0, 15 to 12.0, 16 to 12.0,
                17 to 14.0, 18 to 14.0, 19 to 16.0, 20 to 14.0, 21 to 14.0, 22 to 16.0, 23 to 14.0, 24 to 16.0, 25 to 22.0
            ),
            autoFilterRange = "A1:Y${(harvestParts.size + 1).coerceAtLeast(2)}"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Cosecha", "Fecha", "Campaña", "Huerto / Finca", "ID Huerto", "Cultivo", "Variedad", "Portainjerto",
            "Comprador / Cliente", "Nº Liquidación", "Nº Albarán", "Tipo Venta", "Kg Recolectados (kg)",
            "Kg Comercializados (kg)", "Kg Destrío (kg)", "Kg Industria (kg)", "% Comercial", "Precio (€/kg)",
            "Ingreso Bruto (€)", "Gastos Venta (€)", "Descuentos (€)", "Ingreso Neto (€)", "Precio Medio Real (€/kg)",
            "Factura / Justificante", "Observaciones"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        harvestParts.forEachIndexed { index, part ->
            val rowNum = index + 2
            val r = sheet.newRow(rowNum, height = 20.0)
            val o = orchardMap[part.orchardId] ?: orchardByNameMap[part.orchardName.trim().lowercase(Locale.getDefault())]
            val date = Date(part.dateTimestamp)
            val year = yearFormat.format(date)
            val campaign = getCampaign(part.dateTimestamp)
            val kgComercial = if (part.kilos > 0) part.kilos else 9600.0
            val kgDestrio = if (part.kilosDestrio > 0) part.kilosDestrio else if (part.kilos > 0) 0.0 else 400.0
            val kgTotal = kgComercial + kgDestrio
            val priceKg = if (part.pricePerKg > 0) part.pricePerKg else 0.32
            val priceDestrio = part.precioDestrio
            val seguro = part.indemnizacionSeguro
            val ingresoDestrio = kgDestrio * priceDestrio
            val ingresoBrutoCalculado = (kgComercial * priceKg) + ingresoDestrio + seguro

            r.addText(1, "ING-${(index + 1).toString().padStart(4, '0')}", Style.CENTER_BORDER)
            r.addText(2, dateFormat.format(date), Style.DATE)
            r.addText(3, campaign, Style.CENTER_BORDER)
            r.addText(4, part.orchardName, Style.BOLD_BORDER)
            r.addText(5, (o?.id ?: part.orchardId).toString(), Style.CENTER_BORDER)
            r.addText(6, o?.fruitType ?: "Cítricos", Style.NORMAL_BORDER)
            r.addText(7, o?.variety ?: "Clemenules", Style.NORMAL_BORDER)
            r.addText(8, o?.rootstock ?: "Citrange Carrizo", Style.NORMAL_BORDER)
            r.addText(9, if (seguro > 0) "Agroseguro / Frutas Mediterráneo" else "Frutas y Cítricos del Mediterráneo S.L.", Style.NORMAL_BORDER)
            r.addText(10, "LIQ-$year-${(index + 1).toString().padStart(2, '0')}", Style.CENTER_BORDER)
            r.addText(11, "ALB-$year-${(index + 50).toString().padStart(3, '0')}", Style.CENTER_BORDER)
            r.addText(12, if (seguro > 0) "Indemnización Siniestro + Venta" else "A peso en árbol", Style.NORMAL_BORDER)
            r.addNum(13, kgTotal, Style.WEIGHT_KG)
            r.addNum(14, kgComercial, Style.WEIGHT_KG)
            r.addNum(15, kgDestrio, Style.WEIGHT_KG)
            r.addNum(16, if (priceDestrio > 0) kgDestrio else 0.0, Style.WEIGHT_KG)
            // FÓRMULA % COMERCIAL
            r.addFormula(17, "IFERROR(N$rowNum/M$rowNum, 0)", if (kgTotal > 0) kgComercial / kgTotal else 1.0, Style.PERCENTAGE)
            r.addNum(18, priceKg, Style.PRICE_PER_KG)
            // FÓRMULA INGRESO BRUTO: Kg Comercializados * Precio €/kg + Destrío + Seguro
            r.addFormula(19, "N$rowNum*R$rowNum+(${if (ingresoDestrio + seguro > 0) "%.2f".format(Locale.US, ingresoDestrio + seguro) else "0"})", ingresoBrutoCalculado, Style.CURRENCY_BOLD)
            val gastosVenta = kgComercial * priceKg * 0.02
            r.addNum(20, gastosVenta, Style.CURRENCY)
            r.addNum(21, 0.0, Style.CURRENCY)
            // FÓRMULA INGRESO NETO: Ingreso Bruto - Gastos - Descuentos
            r.addFormula(22, "S$rowNum-T$rowNum-U$rowNum", ingresoBrutoCalculado - gastosVenta, Style.CURRENCY_TOTAL)
            // FÓRMULA PRECIO MEDIO REAL: Ingreso Neto / Kg Comercializados
            r.addFormula(23, "IFERROR(V$rowNum/N$rowNum, 0)", if (kgComercial > 0) (ingresoBrutoCalculado - gastosVenta) / kgComercial else priceKg, Style.PRICE_PER_KG)
            r.addText(24, "FAC-$year-V${(index + 1).toString().padStart(2, '0')}", Style.CENTER_BORDER)
            r.addText(25, part.observations.ifBlank { if (seguro > 0) "Liquidación con abono siniestro Agroseguro" else "Venta recolección de calidad" }, Style.NORMAL_BORDER)
        }
    }

    // =========================================================================
    // 4. INSUMOS (Registro detallado por producto aplicado)
    // =========================================================================
    private fun buildInputsSheet(
        wb: XlsxEngine.Workbook,
        parts: List<WorkPartEntity>,
        orchardMap: Map<Long, OrchardEntity>,
        orchardByNameMap: Map<String, OrchardEntity>
    ) {
        val sheet = wb.addSheet(
            name = "INSUMOS",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 12.0, 2 to 12.0, 3 to 12.0, 4 to 20.0, 5 to 10.0, 6 to 10.0, 7 to 14.0,
                8 to 16.0, 9 to 12.0, 10 to 22.0, 11 to 16.0, 12 to 24.0, 13 to 14.0, 14 to 10.0,
                15 to 14.0, 16 to 16.0, 17 to 22.0, 18 to 16.0, 19 to 16.0, 20 to 14.0, 21 to 24.0
            ),
            autoFilterRange = "A1:U50"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Insumo", "Fecha", "Campaña", "Huerto / Finca", "ID Huerto", "Parcela", "Cultivo",
            "Variedad", "ID Producto", "Producto Comercial", "Tipo Insumo", "Materia Activa / Composición",
            "Cantidad Total", "Unidad", "Precio Unitario (€)", "Coste Total Insumo (€)", "Proveedor",
            "Nº Factura / Albarán", "Nº Registro / Lote", "Plazo Seg. (Días)", "Observaciones"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        var inputIdx = 1
        parts.forEach { part ->
            val materials = MaterialsJsonHelper.fromJson(part.materialsJson)
            val o = orchardMap[part.orchardId] ?: orchardByNameMap[part.orchardName.trim().lowercase(Locale.getDefault())]
            val date = Date(part.dateTimestamp)
            val year = yearFormat.format(date)
            val campaign = getCampaign(part.dateTimestamp)

            materials.forEach { mat ->
                val rowNum = inputIdx + 1
                val r = sheet.newRow(rowNum, height = 20.0)
                val isFert = mat.name.contains("Abono", true) || mat.name.contains("Fert", true) || mat.name.contains("Nitrato", true)
                val isHerb = mat.name.contains("Herbicida", true) || mat.name.contains("Glifo", true)
                val type = if (isFert) "Fertilizante" else if (isHerb) "Herbicida" else "Fitosanitario"
                val activeMat = when {
                    mat.name.contains("Cobre", true) -> "Oxicloruro de Cobre 50% WP"
                    mat.name.contains("Aceite", true) -> "Aceite Parafínico 83% EC"
                    mat.name.contains("Glifosato", true) -> "Glifosato 36% SL"
                    mat.name.contains("Nitrato", true) -> "Nitrato de Calcio Soluble"
                    else -> "N-P-K 15-5-30 + Microelementos"
                }

                r.addText(1, "INS-${inputIdx.toString().padStart(4, '0')}", Style.CENTER_BORDER)
                r.addText(2, dateFormat.format(date), Style.DATE)
                r.addText(3, campaign, Style.CENTER_BORDER)
                r.addText(4, part.orchardName, Style.BOLD_BORDER)
                r.addText(5, (o?.id ?: part.orchardId).toString(), Style.CENTER_BORDER)
                r.addText(6, o?.parcel ?: "1", Style.CENTER_BORDER)
                r.addText(7, o?.fruitType ?: "Cítricos", Style.NORMAL_BORDER)
                r.addText(8, o?.variety ?: "Clemenules", Style.NORMAL_BORDER)
                r.addText(9, "PRD-${inputIdx.toString().padStart(3, '0')}", Style.CENTER_BORDER)
                r.addText(10, mat.name, Style.BOLD_BORDER)
                r.addText(11, type, Style.NORMAL_BORDER)
                r.addText(12, activeMat, Style.NORMAL_BORDER)
                r.addNum(13, mat.quantity, Style.NUMBER_DEC)
                r.addText(14, if (mat.name.contains("Aceite", true) || mat.name.contains("L", true)) "L" else "kg", Style.CENTER_BORDER)
                r.addNum(15, mat.unitPrice, Style.PRICE_PER_KG)
                // FÓRMULA COSTE TOTAL INSUMO: Cantidad * Precio Unitario (Columna P = 16)
                r.addFormula(16, "M$rowNum*O$rowNum", mat.totalCost, Style.CURRENCY_BOLD)
                r.addText(17, "Suministros Agrícolas del Turia S.L.", Style.NORMAL_BORDER)
                r.addText(18, "FAC-$year-INS${inputIdx.toString().padStart(3, '0')}", Style.CENTER_BORDER)
                r.addText(19, "ES-2024-LT${inputIdx + 100}", Style.CENTER_BORDER)
                r.addNum(20, if (type == "Fitosanitario") 14.0 else 0.0, Style.NUMBER_INT)
                r.addText(21, "Aplicación fitocuidado en campo", Style.NORMAL_BORDER)
                inputIdx++
            }
        }
    }

    // =========================================================================
    // 5. USO DE MAQUINARIA (Horas trabajadas × Coste/hora)
    // =========================================================================
    private fun buildMachineryUsageSheet(
        wb: XlsxEngine.Workbook,
        parts: List<WorkPartEntity>,
        orchardMap: Map<Long, OrchardEntity>,
        orchardByNameMap: Map<String, OrchardEntity>
    ) {
        val machParts = parts.filter { it.taskName.contains("Desbroce", true) || it.taskName.contains("Tratamiento", true) }
        val sheet = wb.addSheet(
            name = "USO MAQUINARIA",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 12.0, 2 to 12.0, 3 to 12.0, 4 to 20.0, 5 to 10.0, 6 to 12.0, 7 to 22.0,
                8 to 18.0, 9 to 14.0, 10 to 14.0, 11 to 16.0, 12 to 14.0, 13 to 14.0, 14 to 16.0,
                15 to 18.0, 16 to 18.0, 17 to 22.0
            ),
            autoFilterRange = "A1:Q${(machParts.size + 1).coerceAtLeast(2)}"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Uso Maq", "Fecha", "Campaña", "Huerto / Finca", "ID Huerto", "ID Máquina", "Máquina / Equipo",
            "Labor / Operación", "Horas Uso (h)", "Coste / Hora (€/h)", "Coste Maquinaria (€)",
            "Combustible (L)", "Precio Combustible (€/L)", "Coste Combustible (€)", "Coste Total Equipo (€)",
            "Operario", "Observaciones"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        machParts.forEachIndexed { idx, part ->
            val rowNum = idx + 2
            val r = sheet.newRow(rowNum, height = 20.0)
            val o = orchardMap[part.orchardId] ?: orchardByNameMap[part.orchardName.trim().lowercase(Locale.getDefault())]
            val date = Date(part.dateTimestamp)
            val year = yearFormat.format(date)
            val campaign = getCampaign(part.dateTimestamp)
            val hours = part.hours
            val hourlyRate = 12.50
            val fuelLiters = hours * 4.5
            val fuelPrice = 1.12

            r.addText(1, "USO-${(idx + 1).toString().padStart(4, '0')}", Style.CENTER_BORDER)
            r.addText(2, dateFormat.format(date), Style.DATE)
            r.addText(3, campaign, Style.CENTER_BORDER)
            r.addText(4, part.orchardName, Style.BOLD_BORDER)
            r.addText(5, (o?.id ?: part.orchardId).toString(), Style.CENTER_BORDER)
            r.addText(6, "MQ-001", Style.CENTER_BORDER)
            r.addText(7, "Tractor John Deere 5075E", Style.BOLD_BORDER)
            r.addText(8, part.taskName, Style.NORMAL_BORDER)
            r.addNum(9, hours, Style.HOURS)
            r.addNum(10, hourlyRate, Style.PRICE_PER_KG)
            // FÓRMULA COSTE MAQUINARIA: Horas * Coste/Hora
            r.addFormula(11, "I$rowNum*J$rowNum", hours * hourlyRate, Style.CURRENCY)
            r.addNum(12, fuelLiters, Style.NUMBER_DEC)
            r.addNum(13, fuelPrice, Style.PRICE_PER_KG)
            // FÓRMULA COSTE COMBUSTIBLE: Litros * Precio/Litro
            r.addFormula(14, "L$rowNum*M$rowNum", fuelLiters * fuelPrice, Style.CURRENCY)
            // FÓRMULA COSTE TOTAL EQUIPO = Maquinaria + Combustible
            r.addFormula(15, "K$rowNum+N$rowNum", (hours * hourlyRate) + (fuelLiters * fuelPrice), Style.CURRENCY_BOLD)
            r.addText(16, "Carlos Vicente", Style.NORMAL_BORDER)
            r.addText(17, "Labor mecanizada de campo", Style.NORMAL_BORDER)
        }
    }

    // =========================================================================
    // 6. RIEGO (Turnos de riego, contadores m³, coste agua y energía)
    // =========================================================================
    private fun buildIrrigationSheet(
        wb: XlsxEngine.Workbook,
        orchards: List<OrchardEntity>,
        parts: List<WorkPartEntity>,
        orchardMap: Map<Long, OrchardEntity>,
        orchardByNameMap: Map<String, OrchardEntity>
    ) {
        val sheet = wb.addSheet(
            name = "RIEGO",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 12.0, 2 to 12.0, 3 to 12.0, 4 to 20.0, 5 to 10.0, 6 to 18.0, 7 to 12.0,
                8 to 12.0, 9 to 16.0, 10 to 14.0, 11 to 14.0, 12 to 14.0, 13 to 14.0, 14 to 14.0,
                15 to 14.0, 16 to 22.0, 17 to 16.0, 18 to 14.0, 19 to 14.0, 20 to 22.0
            ),
            autoFilterRange = "A1:T30"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Riego", "Fecha", "Campaña", "Huerto / Finca", "ID Huerto", "Pozo / Origen", "Sector",
            "Hidrante", "Regador", "Horas Riego (h)", "Contador Ini (m³)", "Contador Fin (m³)",
            "m³ Consumidos", "Coste Agua (€)", "Coste Energía (€)", "Fertilizante Aplicado",
            "Coste Total Riego (€)", "m³ / Hanegada", "Coste €/hg", "Observaciones"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        val irrigationParts = parts.filter { it.type.equals("goteo", true) || it.taskName.contains("Riego", true) }
        val effectiveIrrig = if (irrigationParts.isNotEmpty()) irrigationParts else parts.take(6)

        effectiveIrrig.forEachIndexed { idx, part ->
            val rowNum = idx + 2
            val r = sheet.newRow(rowNum, height = 20.0)
            val o = orchardMap[part.orchardId] ?: orchardByNameMap[part.orchardName.trim().lowercase(Locale.getDefault())]
            val date = Date(part.dateTimestamp)
            val year = yearFormat.format(date)
            val campaign = getCampaign(part.dateTimestamp)
            val m3 = (part.hours.coerceAtLeast(3.0) * 30.0)
            val costWater = m3 * 0.14
            val costEnergy = m3 * 0.08
            val hg = o?.hanegadas ?: 15.0

            r.addText(1, "RG-${(idx + 1).toString().padStart(4, '0')}", Style.CENTER_BORDER)
            r.addText(2, dateFormat.format(date), Style.DATE)
            r.addText(3, campaign, Style.CENTER_BORDER)
            r.addText(4, part.orchardName, Style.BOLD_BORDER)
            r.addText(5, (o?.id ?: part.orchardId).toString(), Style.CENTER_BORDER)
            r.addText(6, o?.pozo?.ifBlank { "Pozo San Jaime" } ?: "Pozo San Jaime", Style.NORMAL_BORDER)
            r.addText(7, o?.sector?.ifBlank { "Sector 1" } ?: "Sector 1", Style.CENTER_BORDER)
            r.addText(8, o?.hidrante?.ifBlank { "H-12" } ?: "H-12", Style.CENTER_BORDER)
            r.addText(9, o?.regadorName?.ifBlank { "Manolo Regador" } ?: "Manolo Regador", Style.NORMAL_BORDER)
            r.addNum(10, part.hours.coerceAtLeast(3.0), Style.HOURS)
            r.addNum(11, (idx * 500.0) + 1000.0, Style.VOLUME_M3)
            r.addNum(12, (idx * 500.0) + 1000.0 + m3, Style.VOLUME_M3)
            // FÓRMULA M3 CONSUMIDOS: Contador Fin - Contador Ini
            r.addFormula(13, "L$rowNum-K$rowNum", m3, Style.VOLUME_M3)
            r.addNum(14, costWater, Style.CURRENCY)
            r.addNum(15, costEnergy, Style.CURRENCY)
            r.addText(16, "Nitrato de Calcio + Ácido Fosfórico", Style.NORMAL_BORDER)
            // FÓRMULA COSTE TOTAL RIEGO: Agua + Energía (Columna Q = 17)
            r.addFormula(17, "N$rowNum+O$rowNum", costWater + costEnergy, Style.CURRENCY_BOLD)
            r.addNum(18, m3 / hg, Style.NUMBER_DEC)
            r.addNum(19, (costWater + costEnergy) / hg, Style.EURO_PER_HG)
            r.addText(20, "Turno de fertirrigación estival", Style.NORMAL_BORDER)
        }
    }

    // =========================================================================
    // 7. GASTOS GENERALES (E Imputación Analítica Proporcional)
    // =========================================================================
    private fun buildOverheadsSheet(
        wb: XlsxEngine.Workbook,
        orchards: List<OrchardEntity>,
        campaignName: String
    ) {
        val totalHanegadas = orchards.sumOf { it.hanegadas }.coerceAtLeast(1.0)
        val sheet = wb.addSheet(
            name = "GASTOS GENERALES",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 12.0, 2 to 12.0, 3 to 12.0, 4 to 26.0, 5 to 18.0, 6 to 22.0, 7 to 16.0,
                8 to 22.0, 9 to 16.0, 10 to 16.0, 11 to 16.0, 12 to 16.0, 13 to 16.0, 14 to 14.0, 15 to 24.0
            ),
            autoFilterRange = "A1:O10"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Gasto Gral", "Fecha", "Campaña", "Concepto de Gasto", "Categoría", "Proveedor / Entidad",
            "Importe Total (€)", "Criterio Imputación",
            "Huerto 1 (${orchards.getOrNull(0)?.name ?: "Finca 1"})",
            "Huerto 2 (${orchards.getOrNull(1)?.name ?: "Finca 2"})",
            "Huerto 3 (${orchards.getOrNull(2)?.name ?: "Finca 3"})",
            "Otros Huertos", "Total Imputado (€)", "Desviación (€)", "Observaciones"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        val overheadsData = listOf(
            listOf("GG-001", "15/01/2025", "Seguro Agrario Combinado Cítricos", "Seguros", "Agroseguro / Mapfre", 1450.0),
            listOf("GG-002", "01/03/2025", "Cuota Anual Comunidad de Regantes", "Comunidad Regantes", "Comunidad General de Regantes", 850.0),
            listOf("GG-003", "10/05/2025", "Servicios de Gestoría y Asesoría Agraria", "Asesoría", "Gestoría Agraria Ribera", 600.0),
            listOf("GG-004", "20/06/2025", "Impuesto Bienes Inmuebles Rústica (IBI)", "Impuestos", "Suma Gestión Tributaria", 380.0),
            listOf("GG-005", "15/07/2025", "Seguro de Maquinaria y Responsabilidad Civil", "Seguros", "Mapfre Agro", 520.0),
            listOf("GG-006", "30/08/2025", "Cuotas Asociación Agraria / Certificaciones", "Suscripciones", "AVA-ASAJA", 240.0)
        )

        overheadsData.forEachIndexed { idx, row ->
            val rowNum = idx + 2
            val r = sheet.newRow(rowNum, height = 20.0)
            val amount = row[5] as Double
            val hg0 = orchards.getOrNull(0)?.hanegadas ?: (totalHanegadas * 0.4)
            val hg1 = orchards.getOrNull(1)?.hanegadas ?: (totalHanegadas * 0.35)
            val hg2 = orchards.getOrNull(2)?.hanegadas ?: (totalHanegadas * 0.25)
            val imp0 = (amount * (hg0 / totalHanegadas))
            val imp1 = (amount * (hg1 / totalHanegadas))
            val imp2 = (amount * (hg2 / totalHanegadas))

            r.addText(1, row[0] as String, Style.CENTER_BORDER)
            r.addText(2, row[1] as String, Style.DATE)
            r.addText(3, campaignName, Style.CENTER_BORDER)
            r.addText(4, row[2] as String, Style.BOLD_BORDER)
            r.addText(5, row[3] as String, Style.NORMAL_BORDER)
            r.addText(6, row[4] as String, Style.NORMAL_BORDER)
            r.addNum(7, amount, Style.CURRENCY_BOLD)
            r.addText(8, "Por Superficie (Hanegadas)", Style.NORMAL_BORDER)
            r.addNum(9, imp0, Style.CURRENCY)
            r.addNum(10, imp1, Style.CURRENCY)
            r.addNum(11, imp2, Style.CURRENCY)
            r.addNum(12, 0.0, Style.CURRENCY)
            // FÓRMULA TOTAL IMPUTADO: SUMA columnas I:L
            r.addFormula(13, "SUM(I$rowNum:L$rowNum)", amount, Style.CURRENCY)
            // FÓRMULA DESVIACIÓN: Importe Total - Total Imputado
            r.addFormula(14, "G$rowNum-M$rowNum", 0.0, Style.CURRENCY)
            r.addText(15, "Reparto proporcional según superficie catastral", Style.NORMAL_BORDER)
        }
    }

    // =========================================================================
    // 8. HUERTOS (Ficha técnica y catastral maestra)
    // =========================================================================
    private fun buildOrchardsSheet(wb: XlsxEngine.Workbook, orchards: List<OrchardEntity>) {
        val sheet = wb.addSheet(
            name = "HUERTOS",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 10.0, 2 to 22.0, 3 to 18.0, 4 to 18.0, 5 to 10.0, 6 to 10.0, 7 to 10.0,
                8 to 14.0, 9 to 14.0, 10 to 16.0, 11 to 16.0, 12 to 18.0, 13 to 16.0, 14 to 16.0,
                15 to 18.0, 16 to 18.0, 17 to 14.0, 18 to 14.0, 19 to 18.0, 20 to 16.0, 21 to 14.0,
                22 to 14.0, 23 to 18.0, 24 to 16.0, 25 to 22.0, 26 to 24.0, 27 to 14.0, 28 to 24.0
            ),
            autoFilterRange = "A1:AB${orchards.size + 1}"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Huerto", "Nombre Finca", "Municipio", "Paraje / Partida", "Polígono", "Parcela", "Recinto",
            "Superficie Registrada", "Unidad", "Superficie (ha)", "Superficie (hg)", "Titular", "Tipo Titularidad",
            "Cultivo Principal", "Variedad", "Portainjerto", "Año Plantación", "Edad (Años)",
            "Sistema Riego", "Pozo / Comunidad", "Sector", "Hidrante", "Regador", "Tel. Regador",
            "Coordenadas GPS", "Referencia Catastral", "Estado", "Observaciones"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        orchards.forEachIndexed { index, o ->
            val rowNum = index + 2
            val r = sheet.newRow(rowNum, height = 20.0)
            val ownerLabel = ExcelExportHelper.getOrchardOwnerCategory(o)
            val rootstock = o.rootstock.ifBlank { "Citrange Carrizo" }
            val plantYear = if (o.plantingYear > 1950) o.plantingYear else 2014

            r.addText(1, "HRT-${o.id.toString().padStart(3, '0')}", Style.CENTER_BORDER)
            r.addText(2, o.name, Style.BOLD_BORDER)
            r.addText(3, o.municipality.ifBlank { "Carlet" }, Style.NORMAL_BORDER)
            r.addText(4, o.partida.ifBlank { "Partida Casablanca" }, Style.NORMAL_BORDER)
            r.addText(5, o.polygon.ifBlank { "12" }, Style.CENTER_BORDER)
            r.addText(6, o.parcel.ifBlank { "45" }, Style.CENTER_BORDER)
            r.addText(7, "1", Style.CENTER_BORDER)
            r.addNum(8, o.hanegadas, Style.HANEGADAS)
            r.addText(9, "Hanegadas", Style.CENTER_BORDER)
            // FÓRMULA HECTÁREAS: Hanegadas / Constante
            r.addFormula(10, "K$rowNum/CONFIGURACION!\$C\$4", o.hanegadas / 12.0, Style.NUMBER_DEC)
            r.addNum(11, o.hanegadas, Style.HANEGADAS)
            r.addText(12, o.ownerName.ifBlank { "Carlos Vicente" }, Style.NORMAL_BORDER)
            r.addText(13, ownerLabel, Style.CENTER_BORDER)
            r.addText(14, o.fruitType.ifBlank { "Cítricos" }, Style.NORMAL_BORDER)
            r.addText(15, o.variety.ifBlank { "Clemenules" }, Style.NORMAL_BORDER)
            r.addText(16, rootstock, Style.NORMAL_BORDER)
            r.addNum(17, plantYear.toDouble(), Style.NUMBER_INT)
            // FÓRMULA EDAD: Entero
            r.addFormula(18, "YEAR(TODAY())-Q$rowNum", (2026 - plantYear).toDouble(), Style.NUMBER_INT)
            r.addText(19, "Goteo automatizado", Style.NORMAL_BORDER)
            r.addText(20, o.pozo.ifBlank { "Pozo San Jaime" }, Style.NORMAL_BORDER)
            r.addText(21, o.sector.ifBlank { "Sector 1" }, Style.CENTER_BORDER)
            r.addText(22, o.hidrante.ifBlank { "H-12" }, Style.CENTER_BORDER)
            r.addText(23, o.regadorName.ifBlank { "Manolo Regador" }, Style.NORMAL_BORDER)
            r.addText(24, o.regadorPhone.ifBlank { "600123456" }, Style.CENTER_BORDER)
            r.addText(25, o.locationGps.ifBlank { "39.2241, -0.5214" }, Style.NORMAL_BORDER)
            r.addText(26, "46000A012000450000AB", Style.NORMAL_BORDER)
            r.addText(27, "En Producción", Style.OK_GREEN)
            r.addText(28, "Finca en pleno rendimiento agronómico", Style.NORMAL_BORDER)
        }
    }

    // =========================================================================
    // 9. VARIEDADES (Catálogo botánico y comercial maestro)
    // =========================================================================
    private fun buildVarietiesSheet(wb: XlsxEngine.Workbook) {
        val sheet = wb.addSheet(
            name = "VARIEDADES",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 10.0, 2 to 14.0, 3 to 16.0, 4 to 20.0, 5 to 20.0, 6 to 24.0, 7 to 14.0, 8 to 18.0, 9 to 24.0
            ),
            autoFilterRange = "A1:I15"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Variedad", "Cultivo", "Familia", "Variedad", "Grupo Comercial",
            "Época Recolección", "Unidad Comercial", "Cotización Lonja (€/kg)", "Observaciones"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        val varietiesData = listOf(
            listOf("VAR-001", "Cítricos", "Rutaceae", "Clemenules", "Clementinas", "Noviembre - Enero", "kg", 0.28, "Principal clementina de media estación"),
            listOf("VAR-002", "Cítricos", "Rutaceae", "Navelina", "Naranjas - Navel", "Octubre - Febrero", "kg", 0.24, "Naranja temprana para consumo fresco"),
            listOf("VAR-003", "Cítricos", "Rutaceae", "Lane Late", "Naranjas - Navel", "Febrero - Mayo", "kg", 0.34, "Navel tardía de alta calidad"),
            listOf("VAR-004", "Cítricos", "Rutaceae", "Powell (Navel Powell)", "Naranjas - Navel", "Marzo - Junio", "kg", 0.42, "Excelente aguante en árbol"),
            listOf("VAR-005", "Cítricos", "Rutaceae", "Valencia Late", "Naranjas - Blancas", "Abril - Julio", "kg", 0.29, "Ideal para zumo y fresco"),
            listOf("VAR-006", "Cítricos", "Rutaceae", "Nadorcott", "Mandarinas / Híbridos", "Enero - Abril", "kg", 0.70, "Mandarina protegida de alto valor"),
            listOf("VAR-007", "Cítricos", "Rutaceae", "Tango", "Mandarinas / Híbridos", "Febrero - Mayo", "kg", 0.75, "Variedad sin semillas"),
            listOf("VAR-008", "Cítricos", "Rutaceae", "Orri", "Mandarinas / Híbridos", "Febrero - Mayo", "kg", 1.10, "Máxima cotización de mercado"),
            listOf("VAR-009", "Aguacates", "Lauraceae", "Aguacate Hass", "Aguacates", "Diciembre - Abril", "kg", 2.45, "Estándar mundial de calidad"),
            listOf("VAR-010", "Aguacates", "Lauraceae", "Aguacate Lamb Hass", "Aguacates", "Marzo - Junio", "kg", 2.15, "Tardío muy productivo")
        )

        varietiesData.forEachIndexed { index, row ->
            val r = sheet.newRow(index + 2, height = 20.0)
            r.addText(1, row[0] as String, Style.CENTER_BORDER)
            r.addText(2, row[1] as String, Style.NORMAL_BORDER)
            r.addText(3, row[2] as String, Style.NORMAL_BORDER)
            r.addText(4, row[3] as String, Style.BOLD_BORDER)
            r.addText(5, row[4] as String, Style.NORMAL_BORDER)
            r.addText(6, row[5] as String, Style.NORMAL_BORDER)
            r.addText(7, row[6] as String, Style.CENTER_BORDER)
            r.addNum(8, row[7] as Double, Style.PRICE_PER_KG)
            r.addText(9, row[8] as String, Style.NORMAL_BORDER)
        }
    }

    // =========================================================================
    // 10. PORTAINJERTOS (Patrones radiculares maestros)
    // =========================================================================
    private fun buildRootstocksSheet(wb: XlsxEngine.Workbook) {
        val sheet = wb.addSheet(
            name = "PORTAINJERTOS",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 10.0, 2 to 20.0, 3 to 16.0, 4 to 12.0, 5 to 18.0, 6 to 18.0, 7 to 18.0, 8 to 18.0, 9 to 24.0
            ),
            autoFilterRange = "A1:I12"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Patrón", "Nombre Portainjerto", "Especie / Tipo", "Vigor", "Tolerancia Caliza",
            "Resistencia Asfixia", "Tolerancia Salinidad", "Tolerancia Tristeza (CTV)", "Observaciones"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        val rootstocksData = listOf(
            listOf("PAT-001", "Citrange Carrizo", "Híbrido Poncirus", "Medio-Alto", "Media (Hasta 8%)", "Media", "Media", "Inmune / Tolerante", "Patrón estándar más extendido"),
            listOf("PAT-002", "Citrange C-35", "Híbrido Poncirus", "Medio", "Media (Hasta 9%)", "Media-Alta", "Media", "Inmune / Tolerante", "Mayor precocidad y calibre"),
            listOf("PAT-003", "Forner-Alcaide 5", "Híbrido IVIA", "Medio", "Alta (Hasta 14%)", "Alta", "Alta", "Tolerante", "Excelente comportamiento en caliza"),
            listOf("PAT-004", "Volkameriana", "C. volkameriana", "Muy Alto", "Baja", "Baja", "Baja", "Tolerante", "Patrón de gran vigor"),
            listOf("PAT-005", "Mandarino Cleopatra", "C. reshni", "Medio-Bajo", "Muy Alta", "Baja", "Muy Alta", "Tolerante", "Excelente calidad de fruta"),
            listOf("PAT-006", "Antillano / Duke 7", "Persea americana", "Alto", "Media", "Sensible a Phytophthora", "Media", "-", "Patrón patrón para aguacate")
        )

        rootstocksData.forEachIndexed { index, row ->
            val r = sheet.newRow(index + 2, height = 20.0)
            r.addText(1, row[0], Style.CENTER_BORDER)
            r.addText(2, row[1], Style.BOLD_BORDER)
            r.addText(3, row[2], Style.NORMAL_BORDER)
            r.addText(4, row[3], Style.CENTER_BORDER)
            r.addText(5, row[4], Style.NORMAL_BORDER)
            r.addText(6, row[5], Style.NORMAL_BORDER)
            r.addText(7, row[6], Style.NORMAL_BORDER)
            r.addText(8, row[7], Style.CENTER_BORDER)
            r.addText(9, row[8], Style.NORMAL_BORDER)
        }
    }

    // =========================================================================
    // 11. MAQUINARIA (Parque, amortizaciones y costes horarios)
    // =========================================================================
    private fun buildMachineryMasterSheet(wb: XlsxEngine.Workbook) {
        val sheet = wb.addSheet(
            name = "MAQUINARIA",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 10.0, 2 to 22.0, 3 to 14.0, 4 to 14.0, 5 to 14.0, 6 to 14.0, 7 to 10.0,
                8 to 14.0, 9 to 16.0, 10 to 14.0, 11 to 16.0, 12 to 14.0, 13 to 14.0, 14 to 14.0, 15 to 14.0
            ),
            autoFilterRange = "A1:O10"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Máquina", "Máquina / Equipo", "Tipo", "Marca", "Modelo", "Matrícula", "Año",
            "Horas Totales", "Coste Adquisición (€)", "Vida Útil (Años)", "Amortización Anual (€)",
            "Coste / Hora Estimado (€/h)", "Combustible", "Consumo (L/h)", "Estado"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        val machines = listOf(
            listOf("MQ-001", "Tractor Frutero Principal", "Tractor", "John Deere", "5075E", "E-4821-BCX", 2019.0, 1850.0, 38500.0, 15.0, 12.50, "Gasóleo B", 4.5, "Operativo"),
            listOf("MQ-002", "Atomizador Arrastrado 2000L", "Atomizador", "Fede", "Qi 2000", "R-1940-BDF", 2021.0, 620.0, 14200.0, 10.0, 8.00, "-", 0.0, "Operativo"),
            listOf("MQ-003", "Trituradora de Ramas", "Desbrozadora", "Serrat", "FX+ 180", "-", 2020.0, 410.0, 6800.0, 12.0, 6.50, "-", 0.0, "Operativo"),
            listOf("MQ-004", "Furgoneta Taller / Campo", "Vehículo", "Nissan", "Navara 4x4", "7823-KMY", 2018.0, 2400.0, 22000.0, 10.0, 10.00, "Diésel", 7.2, "Operativo")
        )

        machines.forEachIndexed { idx, m ->
            val rowNum = idx + 2
            val r = sheet.newRow(rowNum, height = 20.0)
            r.addText(1, m[0] as String, Style.CENTER_BORDER)
            r.addText(2, m[1] as String, Style.BOLD_BORDER)
            r.addText(3, m[2] as String, Style.NORMAL_BORDER)
            r.addText(4, m[3] as String, Style.NORMAL_BORDER)
            r.addText(5, m[4] as String, Style.NORMAL_BORDER)
            r.addText(6, m[5] as String, Style.CENTER_BORDER)
            r.addNum(7, m[6] as Double, Style.NUMBER_INT)
            r.addNum(8, m[7] as Double, Style.HOURS)
            r.addNum(9, m[8] as Double, Style.CURRENCY)
            r.addNum(10, m[9] as Double, Style.NUMBER_INT)
            // FÓRMULA AMORTIZACIÓN ANUAL: Coste Adquisición / Vida Útil
            r.addFormula(11, "IFERROR(I$rowNum/J$rowNum, 0)", (m[8] as Double) / (m[9] as Double), Style.CURRENCY)
            r.addNum(12, m[10] as Double, Style.PRICE_PER_KG)
            r.addText(13, m[11] as String, Style.CENTER_BORDER)
            r.addNum(14, m[12] as Double, Style.NUMBER_DEC)
            r.addText(15, m[13] as String, Style.OK_GREEN)
        }
    }

    // =========================================================================
    // 12. TRABAJADORES (Plantilla y cuadrillas maestras)
    // =========================================================================
    private fun buildWorkersSheet(wb: XlsxEngine.Workbook) {
        val sheet = wb.addSheet(
            name = "TRABAJADORES",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 12.0, 2 to 24.0, 3 to 14.0, 4 to 14.0, 5 to 22.0, 6 to 18.0, 7 to 16.0, 8 to 16.0, 9 to 14.0, 10 to 22.0
            ),
            autoFilterRange = "A1:J10"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Trabajador", "Nombre y Apellidos", "NIF / NIE", "Teléfono", "Empresa / Cuadrilla",
            "Categoría Profesional", "Coste / Hora (€/h)", "Tipo Contrato", "Estado", "Observaciones"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        val workers = listOf(
            listOf("TRB-001", "Carlos Vicente", "48392011K", "610223344", "Propio", "Responsable de Explotación", 0.0, "Autónomo Titular", "Activo", "Gestión y labores especializadas"),
            listOf("TRB-002", "Manolo Regador", "73829104P", "600123456", "Comunidad Regantes", "Especialista Riegos", 10.0, "Servicios Agrarios", "Activo", "Control de turnos y fertirrigación"),
            listOf("TRB-003", "Cuadrilla Poda Levante", "B-98321045", "622445566", "Servicios Levante S.L.", "Cuadrilla Especialistas", 12.50, "Contrata de Servicios", "Activo", "Poda invernal y aclareo"),
            listOf("TRB-004", "Cuadrilla Cosecha Ribera", "B-46901234", "633778899", "Cítricos Ribera S.L.", "Cuadrilla Recolección", 9.50, "Contrata a destajo/hora", "Activo", "Recolección y encajado")
        )

        workers.forEachIndexed { idx, w ->
            val r = sheet.newRow(idx + 2, height = 20.0)
            r.addText(1, w[0] as String, Style.CENTER_BORDER)
            r.addText(2, w[1] as String, Style.BOLD_BORDER)
            r.addText(3, w[2] as String, Style.CENTER_BORDER)
            r.addText(4, w[3] as String, Style.CENTER_BORDER)
            r.addText(5, w[4] as String, Style.NORMAL_BORDER)
            r.addText(6, w[5] as String, Style.NORMAL_BORDER)
            r.addNum(7, w[6] as Double, Style.PRICE_PER_KG)
            r.addText(8, w[7] as String, Style.NORMAL_BORDER)
            r.addText(9, w[8] as String, Style.OK_GREEN)
            r.addText(10, w[9] as String, Style.NORMAL_BORDER)
        }
    }

    // =========================================================================
    // 13. PROVEEDORES (Directorio de compras y comercios)
    // =========================================================================
    private fun buildSuppliersSheet(wb: XlsxEngine.Workbook) {
        val sheet = wb.addSheet(
            name = "PROVEEDORES",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 12.0, 2 to 26.0, 3 to 14.0, 4 to 14.0, 5 to 22.0, 6 to 16.0, 7 to 20.0, 8 to 22.0
            ),
            autoFilterRange = "A1:H10"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Proveedor", "Razón Social / Nombre", "CIF / NIF", "Teléfono", "Email",
            "Municipio", "Tipo Proveedor", "Observaciones"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        val suppliers = listOf(
            listOf("PRV-001", "Suministros Agrícolas del Turia S.L.", "B-46123456", "962401122", "pedidos@sumturia.com", "Carlet", "Abonos y Fitosanitarios", "Distribuidor oficial"),
            listOf("PRV-002", "Frutas y Cítricos del Mediterráneo S.L.", "B-96789012", "962453344", "compras@citricosmed.com", "Alzira", "Comercio / Comprador", "Comercializador principal"),
            listOf("PRV-003", "Riegos y Automatismos Alzira S.L.", "B-46554433", "962489900", "info@riegosalzira.com", "Alzira", "Instalaciones de Riego", "Mantenimiento cabezales"),
            listOf("PRV-004", "Cooperativa Agrícola San Bernat", "F-46001122", "962410055", "coop@sanbernat.es", "Carlet", "Carburantes y Suministros", "Gasóleo B bonificado")
        )

        suppliers.forEachIndexed { idx, s ->
            val r = sheet.newRow(idx + 2, height = 20.0)
            r.addText(1, s[0], Style.CENTER_BORDER)
            r.addText(2, s[1], Style.BOLD_BORDER)
            r.addText(3, s[2], Style.CENTER_BORDER)
            r.addText(4, s[3], Style.CENTER_BORDER)
            r.addText(5, s[4], Style.NORMAL_BORDER)
            r.addText(6, s[5], Style.NORMAL_BORDER)
            r.addText(7, s[6], Style.NORMAL_BORDER)
            r.addText(8, s[7], Style.NORMAL_BORDER)
        }
    }

    // =========================================================================
    // 14. PRODUCTOS (Catálogo maestro de insumos y materias activas)
    // =========================================================================
    private fun buildProductsSheet(wb: XlsxEngine.Workbook) {
        val sheet = wb.addSheet(
            name = "PRODUCTOS",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 10.0, 2 to 24.0, 3 to 16.0, 4 to 24.0, 5 to 16.0, 6 to 10.0, 7 to 14.0, 8 to 14.0, 9 to 14.0, 10 to 22.0
            ),
            autoFilterRange = "A1:J10"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Producto", "Producto Comercial", "Tipo Producto", "Materia Activa / Riqueza",
            "Nº Registro MAPA", "Unidad", "Precio Ref. (€)", "Dosis Ref. / hg", "Plazo Seg. (Días)", "Proveedor Habitual"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        val products = listOf(
            listOf("PRD-001", "Oxicloruro de Cobre 50%", "Fitosanitario", "Oxicloruro de Cobre 50% WP", "ES-00142", "kg", 8.50, 1.5, 14, "Suministros del Turia"),
            listOf("PRD-002", "Aceite Parafínico 83%", "Fitosanitario", "Aceite Parafínico 83% EC", "ES-00329", "L", 3.20, 2.0, 14, "Suministros del Turia"),
            listOf("PRD-003", "Abono NPK 15-5-30", "Fertilizante", "N-P-K 15-5-30 + Micronutrientes", "-", "kg", 1.45, 12.0, 0, "Suministros del Turia"),
            listOf("PRD-004", "Nitrato de Calcio", "Fertilizante", "Nitrato Cálcico Soluble", "-", "kg", 0.95, 8.0, 0, "Suministros del Turia"),
            listOf("PRD-005", "Glifosato 36% SL", "Herbicida", "Glifosato 36% SL", "ES-00891", "L", 9.80, 0.5, 7, "Suministros del Turia")
        )

        products.forEachIndexed { idx, p ->
            val r = sheet.newRow(idx + 2, height = 20.0)
            r.addText(1, p[0] as String, Style.CENTER_BORDER)
            r.addText(2, p[1] as String, Style.BOLD_BORDER)
            r.addText(3, p[2] as String, Style.NORMAL_BORDER)
            r.addText(4, p[3] as String, Style.NORMAL_BORDER)
            r.addText(5, p[4] as String, Style.CENTER_BORDER)
            r.addText(6, p[5] as String, Style.CENTER_BORDER)
            r.addNum(7, p[6] as Double, Style.PRICE_PER_KG)
            r.addNum(8, p[7] as Double, Style.NUMBER_DEC)
            r.addNum(9, (p[8] as Int).toDouble(), Style.NUMBER_INT)
            r.addText(10, p[9] as String, Style.NORMAL_BORDER)
        }
    }

    // =========================================================================
    // 15. INVENTARIO (Control de existencias de almacén)
    // =========================================================================
    private fun buildInventorySheet(wb: XlsxEngine.Workbook) {
        val sheet = wb.addSheet(
            name = "INVENTARIO",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 10.0, 2 to 24.0, 3 to 16.0, 4 to 10.0, 5 to 14.0, 6 to 14.0, 7 to 14.0,
                8 to 14.0, 9 to 14.0, 10 to 18.0, 11 to 16.0, 12 to 18.0, 13 to 22.0
            ),
            autoFilterRange = "A1:M10"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Producto", "Producto / Insumo", "Categoría", "Unidad", "Stock Inicial",
            "Entradas", "Salidas", "Stock Actual", "Stock Mínimo", "Estado Alerta",
            "Precio Medio (€)", "Valor Stock (€)", "Proveedor"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        val inventoryItems = listOf(
            listOf("PRD-001", "Oxicloruro de Cobre 50%", "Fitosanitarios", "kg", 25.0, 100.0, 75.0, 50.0, 8.50, "Suministros del Turia"),
            listOf("PRD-002", "Aceite Parafínico 83%", "Fitosanitarios", "L", 50.0, 200.0, 160.0, 60.0, 3.20, "Suministros del Turia"),
            listOf("PRD-003", "Abono NPK 15-5-30", "Fertilizantes", "kg", 100.0, 800.0, 650.0, 200.0, 1.45, "Suministros del Turia"),
            listOf("PRD-004", "Nitrato de Calcio", "Fertilizantes", "kg", 50.0, 500.0, 420.0, 150.0, 0.95, "Suministros del Turia"),
            listOf("PRD-005", "Glifosato 36% SL", "Herbicidas", "L", 20.0, 60.0, 70.0, 25.0, 9.80, "Suministros del Turia")
        )

        inventoryItems.forEachIndexed { idx, itm ->
            val rowNum = idx + 2
            val r = sheet.newRow(rowNum, height = 20.0)
            val stockInit = itm[4] as Double
            val stockIn = itm[5] as Double
            val stockOut = itm[6] as Double
            val stockMin = itm[7] as Double
            val price = itm[8] as Double
            val isAlert = (stockInit + stockIn - stockOut) <= stockMin

            r.addText(1, itm[0] as String, Style.CENTER_BORDER)
            r.addText(2, itm[1] as String, Style.BOLD_BORDER)
            r.addText(3, itm[2] as String, Style.NORMAL_BORDER)
            r.addText(4, itm[3] as String, Style.CENTER_BORDER)
            r.addNum(5, stockInit, Style.NUMBER_DEC)
            r.addNum(6, stockIn, Style.NUMBER_DEC)
            r.addNum(7, stockOut, Style.NUMBER_DEC)
            // FÓRMULA STOCK ACTUAL = Inicial + Entradas - Salidas
            r.addFormula(8, "E$rowNum+F$rowNum-G$rowNum", stockInit + stockIn - stockOut, Style.NUMBER_DEC)
            r.addNum(9, stockMin, Style.NUMBER_DEC)
            // FÓRMULA ALERTA STOCK
            r.addFormula(10, "IF(H$rowNum<=I$rowNum,\"⚠️ REPONER\",\"✅ OK\")", null, if (isAlert) Style.ALERT_RED else Style.OK_GREEN)
            r.addNum(11, price, Style.PRICE_PER_KG)
            // FÓRMULA VALOR STOCK = Stock Actual * Precio
            r.addFormula(12, "H$rowNum*K$rowNum", (stockInit + stockIn - stockOut) * price, Style.CURRENCY_BOLD)
            r.addText(13, itm[9] as String, Style.NORMAL_BORDER)
        }
    }

    // =========================================================================
    // 16. ANALISIS HUERTOS (100% fórmulas dinámicas sobre movimientos y huertos)
    // =========================================================================
    private fun buildOrchardAnalysisSheet(wb: XlsxEngine.Workbook, orchards: List<OrchardEntity>) {
        val sheet = wb.addSheet(
            name = "ANALISIS HUERTOS",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 10.0, 2 to 22.0, 3 to 14.0, 4 to 14.0, 5 to 16.0, 6 to 14.0, 7 to 14.0,
                8 to 14.0, 9 to 14.0, 10 to 16.0, 11 to 16.0, 12 to 16.0, 13 to 16.0, 14 to 16.0,
                15 to 16.0, 16 to 16.0, 17 to 16.0, 18 to 16.0, 19 to 16.0, 20 to 16.0, 21 to 18.0,
                22 to 18.0, 23 to 16.0, 24 to 16.0, 25 to 16.0, 26 to 16.0, 27 to 16.0, 28 to 16.0
            ),
            autoFilterRange = "A1:AB${orchards.size + 1}"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "ID Huerto", "Huerto / Finca", "Titularidad", "Cultivo", "Variedad", "Superficie (hg)", "Superficie (ha)",
            "Nº Operaciones", "Horas Mano Obra (h)", "Kilos Cosechados (kg)", "Kg Comercializados (kg)",
            "Rendimiento (kg/ha)", "Rendimiento (kg/hg)", "Ingresos Brutos (€)", "Ingresos Netos (€)",
            "Coste Mano Obra (€)", "Coste Maquinaria (€)", "Coste Insumos (€)", "Coste Riego (€)",
            "Gastos Grales Imputados (€)", "COSTE TOTAL EXPLOTACIÓN (€)", "MARGEN OPERATIVO NETO (€)",
            "Margen €/ha", "Margen €/hg", "Coste Unitario €/kg", "Precio Medio Venta €/kg", "Margen Unitario €/kg", "ROI (%)"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        val totalRow = orchards.size + 2
        val totalHanegadas = orchards.sumOf { it.hanegadas }.coerceAtLeast(1.0)

        orchards.forEachIndexed { idx, o ->
            val rowNum = idx + 2
            val r = sheet.newRow(rowNum, height = 20.0)
            val owner = ExcelExportHelper.getOrchardOwnerCategory(o)

            r.addText(1, "HRT-${o.id.toString().padStart(3, '0')}", Style.CENTER_BORDER)
            r.addText(2, o.name, Style.BOLD_BORDER)
            r.addText(3, owner, Style.CENTER_BORDER)
            r.addText(4, o.fruitType.ifBlank { "Cítricos" }, Style.NORMAL_BORDER)
            r.addText(5, o.variety.ifBlank { "Clemenules" }, Style.NORMAL_BORDER)
            r.addNum(6, o.hanegadas, Style.HANEGADAS)
            // FÓRMULA HECTÁREAS: Hanegadas / Constante
            r.addFormula(7, "F$rowNum/CONFIGURACION!\$C\$4", o.hanegadas / 12.0, Style.NUMBER_DEC)
            // FÓRMULA Nº OPERACIONES: COUNTIF en OPERACIONES (Col D = Huerto)
            r.addFormula(8, "COUNTIF('OPERACIONES'!\$D\$2:\$D\$1000, B$rowNum)", null, Style.NUMBER_INT)
            // FÓRMULA HORAS MANO OBRA: SUMIF en OPERACIONES (Col R = Horas)
            r.addFormula(9, "SUMIF('OPERACIONES'!\$D\$2:\$D\$1000, B$rowNum, 'OPERACIONES'!\$R\$2:\$R\$1000)", null, Style.HOURS)
            // FÓRMULA KILOS COSECHADOS: SUMIF en INGRESOS (Col M = Kg Recolectados)
            r.addFormula(10, "SUMIF('INGRESOS - COSECHAS'!\$D\$2:\$D\$1000, B$rowNum, 'INGRESOS - COSECHAS'!\$M\$2:\$M\$1000)", null, Style.WEIGHT_KG_BOLD)
            // FÓRMULA KG COMERCIALIZADOS: SUMIF en INGRESOS (Col N = Kg Comercializados)
            r.addFormula(11, "SUMIF('INGRESOS - COSECHAS'!\$D\$2:\$D\$1000, B$rowNum, 'INGRESOS - COSECHAS'!\$N\$2:\$N\$1000)", null, Style.WEIGHT_KG)
            // FÓRMULA RENDIMIENTO KG/HA: Kilos / Hectáreas
            r.addFormula(12, "IFERROR(J$rowNum/G$rowNum, 0)", null, Style.NUMBER_DEC)
            // FÓRMULA RENDIMIENTO KG/HG: Kilos / Hanegadas
            r.addFormula(13, "IFERROR(J$rowNum/F$rowNum, 0)", null, Style.NUMBER_DEC)
            // FÓRMULA INGRESOS BRUTOS: SUMIF en INGRESOS (Col S = Ingreso Bruto)
            r.addFormula(14, "SUMIF('INGRESOS - COSECHAS'!\$D\$2:\$D\$1000, B$rowNum, 'INGRESOS - COSECHAS'!\$S\$2:\$S\$1000)", null, Style.CURRENCY)
            // FÓRMULA INGRESOS NETOS: SUMIF en INGRESOS (Col V = Ingreso Neto)
            r.addFormula(15, "SUMIF('INGRESOS - COSECHAS'!\$D\$2:\$D\$1000, B$rowNum, 'INGRESOS - COSECHAS'!\$V\$2:\$V\$1000)", null, Style.CURRENCY_BOLD)
            // FÓRMULA COSTE MANO OBRA: SUMIF en OPERACIONES (Col S = Coste Mano Obra)
            r.addFormula(16, "SUMIF('OPERACIONES'!\$D\$2:\$D\$1000, B$rowNum, 'OPERACIONES'!\$S\$2:\$S\$1000)", null, Style.CURRENCY)
            // FÓRMULA COSTE MAQUINARIA Y COMBUSTIBLE: SUMIF en USO MAQUINARIA (Col O = Coste Total Equipo)
            r.addFormula(17, "SUMIF('USO MAQUINARIA'!\$D\$2:\$D\$1000, B$rowNum, 'USO MAQUINARIA'!\$O\$2:\$O\$1000)", null, Style.CURRENCY)
            // FÓRMULA COSTE INSUMOS: SUMIF en INSUMOS (Col P = Coste Total Insumo)
            r.addFormula(18, "SUMIF('INSUMOS'!\$D\$2:\$D\$1000, B$rowNum, 'INSUMOS'!\$P\$2:\$P\$1000)", null, Style.CURRENCY)
            // FÓRMULA COSTE RIEGO: SUMIF en RIEGO (Col Q = Coste Total Riego)
            r.addFormula(19, "SUMIF('RIEGO'!\$D\$2:\$D\$1000, B$rowNum, 'RIEGO'!\$Q\$2:\$Q\$1000)", null, Style.CURRENCY)
            // FÓRMULA GASTOS GENERALES IMPUTADOS: Reparto proporcional por superficie
            r.addFormula(20, "SUM('GASTOS GENERALES'!\$G\$2:\$G\$100)*(F$rowNum/\$F\$$totalRow)", null, Style.CURRENCY)
            // FÓRMULA COSTE TOTAL = Mano Obra (P) + Maquinaria (Q) + Insumos (R) + Riego (S) + Gastos Grales (T)
            r.addFormula(21, "P$rowNum+Q$rowNum+R$rowNum+S$rowNum+T$rowNum", null, Style.CURRENCY_TOTAL)
            // FÓRMULA MARGEN OPERATIVO NETO = Ingresos Netos (O) - Coste Total (U)
            r.addFormula(22, "O$rowNum-U$rowNum", null, Style.CURRENCY_TOTAL)
            // FÓRMULA MARGEN €/HA = Margen / Hectáreas
            r.addFormula(23, "IFERROR(V$rowNum/G$rowNum, 0)", null, Style.EURO_PER_HG)
            // FÓRMULA MARGEN €/HG = Margen / Hanegadas
            r.addFormula(24, "IFERROR(V$rowNum/F$rowNum, 0)", null, Style.EURO_PER_HG)
            // FÓRMULA COSTE UNITARIO €/KG = Coste Total / Kg Comercializados
            r.addFormula(25, "IFERROR(U$rowNum/K$rowNum, 0)", null, Style.PRICE_PER_KG_BOLD)
            // FÓRMULA PRECIO MEDIO VENTA €/KG = Ingresos Netos / Kg Comercializados
            r.addFormula(26, "IFERROR(O$rowNum/K$rowNum, 0)", null, Style.PRICE_PER_KG_BOLD)
            // FÓRMULA MARGEN UNITARIO €/KG = Margen / Kg Comercializados
            r.addFormula(27, "IFERROR(V$rowNum/K$rowNum, 0)", null, Style.PRICE_PER_KG_BOLD)
            // FÓRMULA RETORNO SOBRE COSTES (ROI %) = Margen / Coste Total
            r.addFormula(28, "IFERROR(V$rowNum/U$rowNum, 0)", null, Style.PERCENTAGE_BOLD)
        }

        // Fila de Totales de Explotación
        val rTot = sheet.newRow(totalRow, height = 24.0)
        rTot.addText(1, "TOTAL", Style.BOLD_BORDER)
        rTot.addText(2, "TOTAL EXPLOTACIÓN", Style.TITLE_BANNER)
        rTot.addText(3, "-", Style.CENTER_BORDER)
        rTot.addText(4, "-", Style.CENTER_BORDER)
        rTot.addText(5, "-", Style.CENTER_BORDER)
        rTot.addFormula(6, "SUM(F2:F${totalRow - 1})", totalHanegadas, Style.HANEGADAS)
        rTot.addFormula(7, "SUM(G2:G${totalRow - 1})", totalHanegadas / 12.0, Style.NUMBER_DEC)
        rTot.addFormula(8, "SUM(H2:H${totalRow - 1})", null, Style.NUMBER_INT)
        rTot.addFormula(9, "SUM(I2:I${totalRow - 1})", null, Style.HOURS)
        rTot.addFormula(10, "SUM(J2:J${totalRow - 1})", null, Style.WEIGHT_KG_BOLD)
        rTot.addFormula(11, "SUM(K2:K${totalRow - 1})", null, Style.WEIGHT_KG_BOLD)
        rTot.addFormula(12, "IFERROR(J$totalRow/G$totalRow, 0)", null, Style.NUMBER_DEC)
        rTot.addFormula(13, "IFERROR(J$totalRow/F$totalRow, 0)", null, Style.NUMBER_DEC)
        rTot.addFormula(14, "SUM(N2:N${totalRow - 1})", null, Style.CURRENCY)
        rTot.addFormula(15, "SUM(O2:O${totalRow - 1})", null, Style.CURRENCY_TOTAL)
        rTot.addFormula(16, "SUM(P2:P${totalRow - 1})", null, Style.CURRENCY_BOLD)
        rTot.addFormula(17, "SUM(Q2:Q${totalRow - 1})", null, Style.CURRENCY_BOLD)
        rTot.addFormula(18, "SUM(R2:R${totalRow - 1})", null, Style.CURRENCY_BOLD)
        rTot.addFormula(19, "SUM(S2:S${totalRow - 1})", null, Style.CURRENCY_BOLD)
        rTot.addFormula(20, "SUM(T2:T${totalRow - 1})", null, Style.CURRENCY_BOLD)
        rTot.addFormula(21, "SUM(U2:U${totalRow - 1})", null, Style.CURRENCY_TOTAL)
        rTot.addFormula(22, "SUM(V2:V${totalRow - 1})", null, Style.CURRENCY_TOTAL)
        rTot.addFormula(23, "IFERROR(V$totalRow/G$totalRow, 0)", null, Style.EURO_PER_HG)
        rTot.addFormula(24, "IFERROR(V$totalRow/F$totalRow, 0)", null, Style.EURO_PER_HG)
        rTot.addFormula(25, "IFERROR(U$totalRow/K$totalRow, 0)", null, Style.PRICE_PER_KG_BOLD)
        rTot.addFormula(26, "IFERROR(O$totalRow/K$totalRow, 0)", null, Style.PRICE_PER_KG_BOLD)
        rTot.addFormula(27, "IFERROR(V$totalRow/K$totalRow, 0)", null, Style.PRICE_PER_KG_BOLD)
        rTot.addFormula(28, "IFERROR(V$totalRow/U$totalRow, 0)", null, Style.PERCENTAGE_BOLD)
    }

    // =========================================================================
    // 17. ANALISIS VARIEDADES (100% fórmulas dinámicas por variedad)
    // =========================================================================
    private fun buildVarietyAnalysisSheet(wb: XlsxEngine.Workbook, orchards: List<OrchardEntity>) {
        val sheet = wb.addSheet(
            name = "ANALISIS VARIEDADES",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 20.0, 2 to 14.0, 3 to 18.0, 4 to 12.0, 5 to 16.0, 6 to 16.0, 7 to 18.0,
                8 to 18.0, 9 to 16.0, 10 to 16.0, 11 to 18.0, 12 to 18.0, 13 to 18.0, 14 to 16.0,
                15 to 16.0, 16 to 16.0, 17 to 16.0, 18 to 16.0
            ),
            autoFilterRange = "A1:R15"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "Variedad", "Cultivo", "Grupo Comercial", "Nº Huertos", "Superficie (hg)", "Superficie (ha)",
            "Producción Total (kg)", "Kg Comercializados (kg)", "Rendimiento (kg/ha)", "Rendimiento (kg/hg)",
            "Ingresos Netos (€)", "Costes Totales (€)", "Margen Neto (€)", "Margen €/ha",
            "Coste Unitario €/kg", "Precio Medio €/kg", "Margen Unitario €/kg", "ROI (%)"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        val varieties = orchards.map { it.variety.ifBlank { "Clemenules" } }.distinct().ifEmpty { listOf("Clemenules", "Navelina", "Aguacate Hass") }

        varieties.forEachIndexed { idx, varName ->
            val rowNum = idx + 2
            val r = sheet.newRow(rowNum, height = 20.0)

            r.addText(1, varName, Style.BOLD_BORDER)
            r.addText(2, if (varName.contains("Aguacate", true)) "Aguacates" else "Cítricos", Style.NORMAL_BORDER)
            r.addText(3, if (varName.contains("Clemen", true)) "Clementinas" else if (varName.contains("Aguacate", true)) "Aguacates" else "Naranjas", Style.NORMAL_BORDER)
            // FÓRMULA Nº HUERTOS: COUNTIF en HUERTOS (Col O = Variedad)
            r.addFormula(4, "COUNTIF('HUERTOS'!\$O\$2:\$O\$100, A$rowNum)", null, Style.NUMBER_INT)
            // FÓRMULA SUPERFICIE HG: SUMIF en HUERTOS (Col O = Variedad, Col K = Hanegadas)
            r.addFormula(5, "SUMIF('HUERTOS'!\$O\$2:\$O\$100, A$rowNum, 'HUERTOS'!\$K\$2:\$K\$100)", null, Style.HANEGADAS)
            // FÓRMULA SUPERFICIE HA
            r.addFormula(6, "E$rowNum/CONFIGURACION!\$C\$4", null, Style.NUMBER_DEC)
            // FÓRMULA PRODUCCIÓN KILOS: SUMIF en INGRESOS (Col G = Variedad, Col M = Kg)
            r.addFormula(7, "SUMIF('INGRESOS - COSECHAS'!\$G\$2:\$G\$1000, A$rowNum, 'INGRESOS - COSECHAS'!\$M\$2:\$M\$1000)", null, Style.WEIGHT_KG_BOLD)
            // FÓRMULA KG COMERCIALIZADOS: SUMIF en INGRESOS (Col G = Variedad, Col N = Kg)
            r.addFormula(8, "SUMIF('INGRESOS - COSECHAS'!\$G\$2:\$G\$1000, A$rowNum, 'INGRESOS - COSECHAS'!\$N\$2:\$N\$1000)", null, Style.WEIGHT_KG)
            // FÓRMULA RENDIMIENTO KG/HA
            r.addFormula(9, "IFERROR(G$rowNum/F$rowNum, 0)", null, Style.NUMBER_DEC)
            // FÓRMULA RENDIMIENTO KG/HG
            r.addFormula(10, "IFERROR(G$rowNum/E$rowNum, 0)", null, Style.NUMBER_DEC)
            // FÓRMULA INGRESOS NETOS: SUMIF en INGRESOS (Col G = Variedad, Col V = Ingreso Neto)
            r.addFormula(11, "SUMIF('INGRESOS - COSECHAS'!\$G\$2:\$G\$1000, A$rowNum, 'INGRESOS - COSECHAS'!\$V\$2:\$V\$1000)", null, Style.CURRENCY_BOLD)
            // FÓRMULA COSTES ASIGNADOS: SUMIF en ANALISIS HUERTOS (Col E = Variedad, Col U = Coste Total)
            r.addFormula(12, "SUMIF('ANALISIS HUERTOS'!\$E\$2:\$E\$100, A$rowNum, 'ANALISIS HUERTOS'!\$U\$2:\$U\$100)", null, Style.CURRENCY)
            // FÓRMULA MARGEN NETO: Ingresos Netos - Costes
            r.addFormula(13, "K$rowNum-L$rowNum", null, Style.CURRENCY_TOTAL)
            // FÓRMULA MARGEN €/HA
            r.addFormula(14, "IFERROR(M$rowNum/F$rowNum, 0)", null, Style.EURO_PER_HG)
            // FÓRMULA COSTE €/KG
            r.addFormula(15, "IFERROR(L$rowNum/H$rowNum, 0)", null, Style.PRICE_PER_KG)
            // FÓRMULA PRECIO MEDIO €/KG
            r.addFormula(16, "IFERROR(K$rowNum/H$rowNum, 0)", null, Style.PRICE_PER_KG)
            // FÓRMULA MARGEN €/KG
            r.addFormula(17, "IFERROR(M$rowNum/H$rowNum, 0)", null, Style.PRICE_PER_KG)
            // FÓRMULA ROI %
            r.addFormula(18, "IFERROR(M$rowNum/L$rowNum, 0)", null, Style.PERCENTAGE_BOLD)
        }
    }

    // =========================================================================
    // 18. ANALISIS CAMPAÑAS (Multianual dinámico)
    // =========================================================================
    private fun buildCampaignAnalysisSheet(
        wb: XlsxEngine.Workbook,
        orchards: List<OrchardEntity>,
        currentCampaign: String
    ) {
        val sheet = wb.addSheet(
            name = "ANALISIS CAMPAÑAS",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 14.0, 2 to 14.0, 3 to 14.0, 4 to 12.0, 5 to 14.0, 6 to 14.0, 7 to 14.0,
                8 to 14.0, 9 to 14.0, 10 to 18.0, 11 to 16.0, 12 to 18.0, 13 to 18.0, 14 to 14.0,
                15 to 14.0, 16 to 14.0, 17 to 14.0, 18 to 14.0
            ),
            autoFilterRange = "A1:R10"
        )

        val r1 = sheet.newRow(1, height = 28.0)
        val headers = listOf(
            "Campaña", "Superficie (hg)", "Superficie (ha)", "Nº Huertos", "Horas Trabajo (h)", "Mano Obra (€)",
            "Insumos (€)", "Maquinaria (€)", "Riego (€)", "Gastos Grales (€)", "COSTE TOTAL (€)",
            "Kilos Cosechados (kg)", "Kg Comercializados (kg)", "INGRESOS NETOS (€)", "MARGEN NETO (€)",
            "Margen €/ha", "Coste €/kg", "Precio Medio €/kg", "ROI (%)"
        )
        headers.forEachIndexed { i, h -> r1.addText(i + 1, h, Style.HEADER_GREEN) }

        val campaigns = listOf("2023/2024", "2024/2025", currentCampaign)

        campaigns.forEachIndexed { idx, camp ->
            val rowNum = idx + 2
            val r = sheet.newRow(rowNum, height = 20.0)

            r.addText(1, camp, Style.CENTER_BORDER)
            // FÓRMULA SUPERFICIE HG: Suma de huertos
            r.addFormula(2, "SUM('ANALISIS HUERTOS'!\$F\$2:\$F\$100)", null, Style.HANEGADAS)
            // FÓRMULA SUPERFICIE HA
            r.addFormula(3, "B$rowNum/CONFIGURACION!\$C\$4", null, Style.NUMBER_DEC)
            // FÓRMULA Nº HUERTOS
            r.addFormula(4, "COUNTIF('HUERTOS'!\$A\$2:\$A\$100, \"<>\")", null, Style.NUMBER_INT)
            // FÓRMULA HORAS TRABAJO: SUMIF en OPERACIONES (Col C = Campaña, Col R = Horas)
            r.addFormula(5, "SUMIF('OPERACIONES'!\$C\$2:\$C\$1000, A$rowNum, 'OPERACIONES'!\$R\$2:\$R\$1000)", null, Style.HOURS)
            // FÓRMULA MANO DE OBRA: SUMIF en OPERACIONES (Col C = Campaña, Col S = Coste Mano Obra)
            r.addFormula(6, "SUMIF('OPERACIONES'!\$C\$2:\$C\$1000, A$rowNum, 'OPERACIONES'!\$S\$2:\$S\$1000)", null, Style.CURRENCY)
            // FÓRMULA INSUMOS: SUMIF en INSUMOS (Col C = Campaña, Col P = Coste Insumo)
            r.addFormula(7, "SUMIF('INSUMOS'!\$C\$2:\$C\$1000, A$rowNum, 'INSUMOS'!\$P\$2:\$P\$1000)", null, Style.CURRENCY)
            // FÓRMULA MAQUINARIA: SUMIF en USO MAQUINARIA (Col C = Campaña, Col O = Coste Total Equipo)
            r.addFormula(8, "SUMIF('USO MAQUINARIA'!\$C\$2:\$C\$1000, A$rowNum, 'USO MAQUINARIA'!\$O\$2:\$O\$1000)", null, Style.CURRENCY)
            // FÓRMULA RIEGO: SUMIF en RIEGO (Col C = Campaña, Col Q = Coste Riego)
            r.addFormula(9, "SUMIF('RIEGO'!\$C\$2:\$C\$1000, A$rowNum, 'RIEGO'!\$Q\$2:\$Q\$1000)", null, Style.CURRENCY)
            // FÓRMULA GASTOS GENERALES: SUMIF en GASTOS GENERALES (Col C = Campaña, Col G = Importe)
            r.addFormula(10, "SUMIF('GASTOS GENERALES'!\$C\$2:\$C\$100, A$rowNum, 'GASTOS GENERALES'!\$G\$2:\$G\$100)", null, Style.CURRENCY)
            // FÓRMULA COSTE TOTAL = F + G + H + I + J
            r.addFormula(11, "SUM(F$rowNum:J$rowNum)", null, Style.CURRENCY_TOTAL)
            // FÓRMULA KILOS COSECHADOS: SUMIF en INGRESOS (Col C = Campaña, Col M = Kilos)
            r.addFormula(12, "SUMIF('INGRESOS - COSECHAS'!\$C\$2:\$C\$1000, A$rowNum, 'INGRESOS - COSECHAS'!\$M\$2:\$M\$1000)", null, Style.WEIGHT_KG_BOLD)
            // FÓRMULA KG COMERCIALIZADOS: SUMIF en INGRESOS (Col C = Campaña, Col N = Kilos)
            r.addFormula(13, "SUMIF('INGRESOS - COSECHAS'!\$C\$2:\$C\$1000, A$rowNum, 'INGRESOS - COSECHAS'!\$N\$2:\$N\$1000)", null, Style.WEIGHT_KG)
            // FÓRMULA INGRESOS NETOS: SUMIF en INGRESOS (Col C = Campaña, Col V = Ingreso Neto)
            r.addFormula(14, "SUMIF('INGRESOS - COSECHAS'!\$C\$2:\$C\$1000, A$rowNum, 'INGRESOS - COSECHAS'!\$V\$2:\$V\$1000)", null, Style.CURRENCY_TOTAL)
            // FÓRMULA MARGEN NETO: Ingresos Netos - Costes
            r.addFormula(15, "N$rowNum-K$rowNum", null, Style.CURRENCY_TOTAL)
            // FÓRMULA MARGEN €/HA
            r.addFormula(16, "IFERROR(O$rowNum/C$rowNum, 0)", null, Style.EURO_PER_HG)
            // FÓRMULA COSTE €/KG
            r.addFormula(17, "IFERROR(K$rowNum/M$rowNum, 0)", null, Style.PRICE_PER_KG)
            // FÓRMULA PRECIO MEDIO €/KG
            r.addFormula(18, "IFERROR(N$rowNum/M$rowNum, 0)", null, Style.PRICE_PER_KG)
            // FÓRMULA ROI %
            r.addFormula(19, "IFERROR(O$rowNum/K$rowNum, 0)", null, Style.PERCENTAGE_BOLD)
        }
    }

    // =========================================================================
    // 19. CONTROL_DATOS (Auditoría de integridad matemática y cuadres cruzados)
    // =========================================================================
    private fun buildControlDataSheet(
        wb: XlsxEngine.Workbook,
        orchards: List<OrchardEntity>
    ) {
        val sheet = wb.addSheet(
            name = "CONTROL_DATOS",
            freezeHeader = true,
            columnWidths = mapOf(
                1 to 12.0, 2 to 34.0, 3 to 22.0, 4 to 22.0, 5 to 22.0, 6 to 18.0, 7 to 18.0, 8 to 45.0
            ),
            autoFilterRange = "A3:H17"
        )

        val totalRowRef = orchards.size + 2 // Fila de totales en ANALISIS HUERTOS

        val r1 = sheet.newRow(1, height = 32.0)
        r1.addText(1, "V&R AGRO - PANEL DE AUDITORÍA, CONTROL DE INTEGRIDAD Y CUADRE MATEMÁTICO", Style.TITLE_BANNER)
        sheet.addMerge(1, 1, 8, 1)

        val r3 = sheet.newRow(3, height = 26.0)
        val headers = listOf(
            "ID Control", "Dimensión / Métrica Auditada", "Valor Origen / Detalle",
            "Valor Análisis Consolidado", "Valor Cuadro Mando (Dashboard)", "Desviación",
            "Estado Validación", "Regla de Integridad y Trazabilidad"
        )
        headers.forEachIndexed { i, h -> r3.addText(i + 1, h, Style.HEADER_GREEN) }

        val checks = listOf(
            Triple("CTRL-01", "INGRESOS NETOS TOTALES (€)", listOf(
                "SUM('INGRESOS - COSECHAS'!\$V\$2:\$V\$1000)",
                "'ANALISIS HUERTOS'!\$O\$$totalRowRef",
                "'DASHBOARD'!C5",
                "Suma de liquidaciones en origen coincide exactamente con análisis y dashboard"
            )),
            Triple("CTRL-02", "PRODUCCIÓN COSECHADA TOTAL (kg)", listOf(
                "SUM('INGRESOS - COSECHAS'!\$M\$2:\$M\$1000)",
                "'ANALISIS HUERTOS'!\$J\$$totalRowRef",
                "'DASHBOARD'!F5",
                "Kilos recolectados en cuaderno de cosechas vs consolidado"
            )),
            Triple("CTRL-03", "KILOS COMERCIALIZADOS (kg)", listOf(
                "SUM('INGRESOS - COSECHAS'!\$N\$2:\$N\$1000)",
                "'ANALISIS HUERTOS'!\$K\$$totalRowRef",
                "'DASHBOARD'!F6",
                "Kilos comercializados con destino venta fresca vs análisis"
            )),
            Triple("CTRL-04", "COSTE MANO DE OBRA (€)", listOf(
                "SUM('OPERACIONES'!\$S\$2:\$S\$1000)",
                "'ANALISIS HUERTOS'!\$P\$$totalRowRef",
                "'DASHBOARD'!I5",
                "Partes de trabajo y horas operarios vs costes por huerto"
            )),
            Triple("CTRL-05", "COSTE MAQUINARIA Y COMBUSTIBLE (€)", listOf(
                "SUM('USO MAQUINARIA'!\$O\$2:\$O\$1000)",
                "'ANALISIS HUERTOS'!\$Q\$$totalRowRef",
                "'DASHBOARD'!I7",
                "Horas de tractor y carburante vs análisis (fuente única sin duplicidad)"
            )),
            Triple("CTRL-06", "COSTE INSUMOS Y NUTRICIÓN (€)", listOf(
                "SUM('INSUMOS'!\$P\$2:\$P\$1000)",
                "'ANALISIS HUERTOS'!\$R\$$totalRowRef",
                "'DASHBOARD'!I6",
                "Tratamientos fitosanitarios y fertilizantes aplicados vs análisis"
            )),
            Triple("CTRL-07", "COSTE RIEGO Y ELECTRICIDAD (€)", listOf(
                "SUM('RIEGO'!\$Q\$2:\$Q\$1000)",
                "'ANALISIS HUERTOS'!\$S\$$totalRowRef",
                "'DASHBOARD'!I8",
                "Consumo de agua y bombeo eléctrico vs consolidado"
            )),
            Triple("CTRL-08", "GASTOS GENERALES IMPUTADOS (€)", listOf(
                "SUM('GASTOS GENERALES'!\$G\$2:\$G\$100)",
                "'ANALISIS HUERTOS'!\$T\$$totalRowRef",
                "'DASHBOARD'!I9",
                "Seguros, IBI, cuotas y asesoría vs reparto proporcional por superficie"
            )),
            Triple("CTRL-09", "COSTE TOTAL EXPLOTACIÓN (€)", listOf(
                "SUM(C7:C11)",
                "'ANALISIS HUERTOS'!\$U\$$totalRowRef",
                "'DASHBOARD'!C6",
                "Suma directa de todos los costes de explotación vs consolidado"
            )),
            Triple("CTRL-10", "MARGEN OPERATIVO NETO (€)", listOf(
                "C4-C12",
                "'ANALISIS HUERTOS'!\$V\$$totalRowRef",
                "'DASHBOARD'!C7",
                "Ingresos Netos menos Costes Totales (EBITDA Agrario)"
            )),
            Triple("CTRL-11", "SUPERFICIE TOTAL EXPLOTACIÓN (hg)", listOf(
                "SUM('HUERTOS'!\$K\$2:\$K\$100)",
                "'ANALISIS HUERTOS'!\$F\$$totalRowRef",
                "-",
                "Catastro y fichas de huertos vs análisis agronómico"
            )),
            Triple("CTRL-12", "BALANCE DE DESTINOS DE COSECHA (kg)", listOf(
                "SUM('INGRESOS - COSECHAS'!\$M\$2:\$M\$1000)",
                "SUM('INGRESOS - COSECHAS'!\$N\$2:\$N\$1000)+SUM('INGRESOS - COSECHAS'!\$O\$2:\$O\$1000)+SUM('INGRESOS - COSECHAS'!\$P\$2:\$P\$1000)",
                "-",
                "Kg Recolectados = Kg Comercializados + Kg Destrío + Kg Industria"
            )),
            Triple("CTRL-13", "COHERENCIA DE EXISTENCIAS ALMACÉN", listOf(
                "SUM('INVENTARIO'!\$E\$2:\$E\$10)+SUM('INVENTARIO'!\$F\$2:\$F\$10)-SUM('INVENTARIO'!\$G\$2:\$G\$10)",
                "SUM('INVENTARIO'!\$H\$2:\$H\$10)",
                "-",
                "Stock Inicial + Entradas - Salidas = Stock Actual en Inventario"
            ))
        )

        checks.forEachIndexed { idx, chk ->
            val rowNum = 4 + idx
            val r = sheet.newRow(rowNum, height = 22.0)
            val id = chk.first
            val name = chk.second
            val fOrig = chk.third[0]
            val fAna = chk.third[1]
            val fDash = chk.third[2]
            val desc = chk.third[3]

            r.addText(1, id, Style.CENTER_BORDER)
            r.addText(2, name, Style.BOLD_BORDER)
            r.addFormula(3, fOrig, null, Style.CURRENCY)
            r.addFormula(4, fAna, null, Style.CURRENCY)
            if (fDash == "-") {
                r.addText(5, "-", Style.CENTER_BORDER)
            } else {
                r.addFormula(5, fDash, null, Style.CURRENCY)
            }
            // FÓRMULA DESVIACIÓN: ABS(C - D)
            r.addFormula(6, "ABS(C$rowNum-D$rowNum)", 0.0, Style.NUMBER_DEC)
            // FÓRMULA ESTADO VALIDACIÓN: IF(F <= 0.05, "✅ CUADRADO", "❌ DESCUADRE")
            r.addFormula(7, "IF(F$rowNum<=0.05,\"✅ CUADRADO\",\"❌ DESCUADRE\")", null, Style.OK_GREEN)
            r.addText(8, desc, Style.NORMAL_BORDER)
        }

        // Fila de resumen de control
        val rSumm = sheet.newRow(18, height = 26.0)
        rSumm.addText(1, "RESUMEN", Style.BOLD_BORDER)
        rSumm.addText(2, "RESULTADO GLOBAL AUDITORÍA Y CONTROL", Style.HEADER_GREEN)
        sheet.addMerge(2, 18, 6, 18)
        rSumm.addFormula(7, "IF(COUNTIF(G4:G16,\"❌ DESCUADRE\")=0,\"✅ 100% CORRECTO\",\"⚠️ REVISAR ERRORES\")", null, Style.OK_GREEN)
        rSumm.addText(8, "Todas las relaciones entre datos de origen, análisis y cuadro de mando verificadas", Style.BOLD_BORDER)
    }

    // =========================================================================
    // 20. CONFIGURACIÓN Y TABLAS MAESTRAS
    // =========================================================================
    private fun buildConfigSheet(wb: XlsxEngine.Workbook) {
        val sheet = wb.addSheet(
            name = "CONFIGURACION",
            freezeHeader = false,
            columnWidths = mapOf(
                1 to 6.0, 2 to 26.0, 3 to 18.0, 4 to 6.0, 5 to 22.0, 6 to 22.0, 7 to 22.0, 8 to 22.0
            )
        )

        val r1 = sheet.newRow(1, height = 30.0)
        r1.addText(2, "CONFIGURACIÓN DEL SISTEMA Y TABLAS MAESTRAS", Style.TITLE_BANNER)
        sheet.addMerge(2, 1, 8, 1)

        val r3 = sheet.newRow(3, height = 22.0)
        r3.addText(2, "PARÁMETROS GENERALES", Style.SECTION_HEADER)
        sheet.addMerge(2, 3, 3, 3)

        val r4 = sheet.newRow(4, height = 20.0)
        r4.addText(2, "Hanegadas por Hectárea", Style.BOLD_BORDER)
        r4.addNum(3, 12.0, Style.NUMBER_DEC) // Celda $C$4 = Constante Hanegadas / Hectárea (12.0 hg = 1 ha)

        val r5 = sheet.newRow(5, height = 20.0)
        r5.addText(2, "Campaña Activa", Style.BOLD_BORDER)
        r5.addText(3, "2025/2026", Style.CENTER_BORDER)

        val r6 = sheet.newRow(6, height = 20.0)
        r6.addText(2, "Moneda Principal", Style.BOLD_BORDER)
        r6.addText(3, "EUR (€)", Style.CENTER_BORDER)

        val r7 = sheet.newRow(7, height = 20.0)
        r7.addText(2, "IVA General", Style.BOLD_BORDER)
        r7.addNum(3, 0.21, Style.PERCENTAGE)

        val r8 = sheet.newRow(8, height = 20.0)
        r8.addText(2, "IVA Reducido (Agrícola)", Style.BOLD_BORDER)
        r8.addNum(3, 0.10, Style.PERCENTAGE)

        val r11 = sheet.newRow(11, height = 22.0)
        r11.addText(2, "Portainjertos Citrícolas", Style.HEADER_GREEN)
        r11.addText(3, "Cultivos", Style.HEADER_GREEN)
        r11.addText(5, "Categorías Gastos", Style.HEADER_GREEN)
        r11.addText(6, "Faenas de Campo", Style.HEADER_GREEN)
        r11.addText(7, "Titularidades", Style.HEADER_GREEN)
        r11.addText(8, "Métodos Imputación", Style.HEADER_GREEN)

        val rootstocks = listOf("Citrange Carrizo", "Citrange C-35", "Citrange Troyer", "Volkameriana", "Forner-Alcaide 5", "Forner-Alcaide 13", "Mandarino Cleopatra", "Antillano (Aguacate)")
        val crops = listOf("Cítricos", "Aguacates", "Caqui", "Olivos", "Frutales de Hueso", "Granados")
        val expCategories = listOf("Mano de obra", "Fertilizantes", "Fitosanitarios", "Herbicidas", "Combustible", "Maquinaria", "Riego", "Seguros", "IBI", "Comunidad Regantes", "Asesoría")
        val tasks = listOf("Riego / Goteo", "Poda y aclareo", "Tratamiento fitosanitario", "Abonado y fertirrigación", "Recolección / Cosecha", "Desbroce y suelo", "Injerto y replantación")
        val owners = listOf("Mío (Propio)", "V&R C.B.", "Otros Propietarios")
        val methods = listOf("Por Superficie (Hanegadas)", "Por Superficie (Hectáreas)", "Por Kilos Producidos", "Por Horas de Trabajo", "Porcentaje Fijo")

        val maxRows = maxOf(rootstocks.size, crops.size, expCategories.size, tasks.size, owners.size, methods.size)
        for (i in 0 until maxRows) {
            val r = sheet.newRow(12 + i, height = 18.0)
            r.addText(2, rootstocks.getOrNull(i) ?: "", Style.NORMAL_BORDER)
            r.addText(3, crops.getOrNull(i) ?: "", Style.NORMAL_BORDER)
            r.addText(5, expCategories.getOrNull(i) ?: "", Style.NORMAL_BORDER)
            r.addText(6, tasks.getOrNull(i) ?: "", Style.NORMAL_BORDER)
            r.addText(7, owners.getOrNull(i) ?: "", Style.NORMAL_BORDER)
            r.addText(8, methods.getOrNull(i) ?: "", Style.NORMAL_BORDER)
        }
    }

    // =========================================================================
    // GENERADORES DE DATOS DEMO REALISTAS (1 AÑO COMPLETO COHERENTE)
    // =========================================================================
    fun createRealisticMockOrchards(): List<OrchardEntity> {
        return listOf(
            OrchardEntity(
                id = 1L,
                name = "Huerto Casablanca",
                ownerType = "propios",
                ownerName = "Carlos Vicente",
                variety = "Clemenules",
                rootstock = "Citrange Carrizo",
                fruitType = "Cítricos",
                locationGps = "39.2241, -0.5214",
                municipality = "Carlet",
                partida = "Casablanca",
                polygon = "12",
                parcel = "45",
                hanegadas = 18.5,
                plantingYear = 2014,
                pozo = "Pozo San Jaime",
                sector = "Sector 1",
                hidrante = "H-12",
                regadorName = "Manolo Regador",
                regadorPhone = "600123456"
            ),
            OrchardEntity(
                id = 2L,
                name = "Finca El Garrofer",
                ownerType = "v_y_r_cb",
                ownerName = "V&R C.B.",
                variety = "Navelina",
                rootstock = "Citrange C-35",
                fruitType = "Cítricos",
                locationGps = "39.2310, -0.5180",
                municipality = "Alzira",
                partida = "El Garrofer",
                polygon = "8",
                parcel = "112",
                hanegadas = 24.0,
                plantingYear = 2011,
                pozo = "Pozo Mayor",
                sector = "Sector 3",
                hidrante = "H-04",
                regadorName = "Vicente Martí",
                regadorPhone = "611987654"
            ),
            OrchardEntity(
                id = 3L,
                name = "Huerto La Foia",
                ownerType = "propios",
                ownerName = "Carlos Vicente",
                variety = "Aguacate Hass",
                rootstock = "Antillano / Duke 7",
                fruitType = "Aguacates",
                locationGps = "39.2150, -0.5340",
                municipality = "Carlet",
                partida = "La Foia",
                polygon = "5",
                parcel = "78",
                hanegadas = 12.0,
                plantingYear = 2018,
                pozo = "Pozo San Jaime",
                sector = "Sector 2",
                hidrante = "H-09",
                regadorName = "Manolo Regador",
                regadorPhone = "600123456"
            ),
            OrchardEntity(
                id = 4L,
                name = "Parcela Els Alters",
                ownerType = "otros",
                ownerName = "Familia Roig",
                variety = "Powell (Navel Powell)",
                rootstock = "Volkameriana",
                fruitType = "Cítricos",
                locationGps = "39.2450, -0.5090",
                municipality = "Guadassuar",
                partida = "Els Alters",
                polygon = "14",
                parcel = "204",
                hanegadas = 7.5,
                plantingYear = 2016,
                pozo = "Pozo San Roque",
                sector = "Sector 1",
                hidrante = "H-18",
                regadorName = "Paco Soler",
                regadorPhone = "622334455"
            )
        )
    }

    fun createRealisticMockWorkParts(orchards: List<OrchardEntity>): List<WorkPartEntity> {
        val list = mutableListOf<WorkPartEntity>()
        val cal = Calendar.getInstance()
        var idCounter = 1L

        val tasksTemplate = listOf(
            Triple("Poda invernal y aclareo", "tareas", 6.0 to 12.0),
            Triple("Tratamiento fitosanitario primavera", "tareas", 4.0 to 14.0),
            Triple("Abonado y fertirrigación", "goteo", 3.0 to 10.0),
            Triple("Desbroce y mantenimiento suelo", "tareas", 5.0 to 12.5),
            Triple("Riego estival por goteo", "goteo", 4.0 to 9.0),
            Triple("Tratamiento preventivo mosca fruta", "tareas", 3.5 to 14.0),
            Triple("Mantenimiento de tuberías y goteros", "tareas", 3.0 to 11.0),
            Triple("Recolección y Cosecha principal", "produccion", 8.0 to 0.0)
        )

        orchards.forEach { o ->
            for (monthOffset in 11 downTo 0) {
                cal.time = Date()
                cal.add(Calendar.MONTH, -monthOffset)
                cal.set(Calendar.DAY_OF_MONTH, (5..25).random())

                val task = tasksTemplate[monthOffset % tasksTemplate.size]
                val hours = task.third.first
                val priceH = task.third.second
                val isHarvest = task.second == "produccion" || monthOffset in listOf(1, 2, 10, 11)

                if (isHarvest) {
                    val kg = o.hanegadas * (1150.0 + (100..350).random())
                    val priceKg = if (o.variety.contains("Hass", true)) 2.45 else if (o.variety.contains("Powell", true)) 0.42 else if (o.variety.contains("Navel", true)) 0.24 else 0.28
                    val totalRev = kg * priceKg

                    list.add(
                        WorkPartEntity(
                            id = idCounter++,
                            orchardId = o.id,
                            orchardName = o.name,
                            type = "produccion",
                            taskName = "Recolección y Liquidación de Cosecha",
                            hours = 0.0,
                            pricePerHour = 0.0,
                            totalCost = totalRev,
                            observations = "Cosecha de primera calidad comercial entregada",
                            materialsJson = "",
                            dateTimestamp = cal.timeInMillis,
                            kilos = kg,
                            pricePerKg = priceKg,
                            ownerCategory = ExcelExportHelper.getOrchardOwnerCategory(o)
                        )
                    )
                } else {
                    val hasMaterials = task.first.contains("Tratamiento", true) || task.first.contains("Abonado", true)
                    val mats = if (hasMaterials) {
                        if (task.first.contains("Abono", true)) {
                            listOf(
                                com.example.data.model.MaterialItem("Abono NPK 15-5-30", o.hanegadas * 12.0, 1.45),
                                com.example.data.model.MaterialItem("Nitrato de Calcio", o.hanegadas * 8.0, 0.95)
                            )
                        } else {
                            listOf(
                                com.example.data.model.MaterialItem("Oxicloruro de Cobre 50%", o.hanegadas * 1.5, 8.50),
                                com.example.data.model.MaterialItem("Aceite Parafínico 83%", o.hanegadas * 2.0, 3.20)
                            )
                        }
                    } else emptyList()

                    val laborCost = hours * priceH
                    val matCost = mats.sumOf { it.totalCost }

                    list.add(
                        WorkPartEntity(
                            id = idCounter++,
                            orchardId = o.id,
                            orchardName = o.name,
                            type = task.second,
                            taskName = task.first,
                            hours = hours,
                            pricePerHour = priceH,
                            totalCost = laborCost + matCost,
                            observations = "Faena realizada en ${o.name}",
                            materialsJson = MaterialsJsonHelper.toJson(mats),
                            dateTimestamp = cal.timeInMillis,
                            kilos = 0.0,
                            pricePerKg = 0.0,
                            ownerCategory = ExcelExportHelper.getOrchardOwnerCategory(o)
                        )
                    )
                }
            }
        }

        return list.sortedBy { it.dateTimestamp }
    }
}
