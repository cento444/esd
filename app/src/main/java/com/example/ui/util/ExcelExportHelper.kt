package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.OrchardEntity
import com.example.data.model.WorkPartEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gestor de exportación a Excel agrícola (.xlsx) profesional multichoja.
 * Genera libros completos estructurados para contabilidad analítica, trazabilidad,
 * gestión de rendimientos y control de costes.
 */
object ExcelExportHelper {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
    private val dateFilenameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Resuelve de forma normalizada la categoría de propietario ("Mío", "V&R", "Otros", "Varios")
     * de cualquier parte de trabajo.
     */
    fun getPartOwnerCategory(
        part: WorkPartEntity,
        orchard: OrchardEntity? = null,
        titularPropios: String = "",
        titularVr: String = "",
        titularOtros: String = ""
    ): String {
        if (part.type.equals("varios", ignoreCase = true) ||
            part.ownerCategory.equals("Varios", ignoreCase = true) ||
            part.taskName.equals("Gastos Varios", ignoreCase = true) ||
            part.taskName.equals("Gasto Varios", ignoreCase = true) ||
            part.orchardName.equals("Gasto General", ignoreCase = true) ||
            part.orchardId <= 0L
        ) {
            return "Varios"
        }

        if (orchard != null) {
            return getOrchardOwnerCategory(orchard)
        }

        val category = part.ownerCategory.trim()
        if (category.equals("V&R", ignoreCase = true) ||
            category.equals("V&R C.B.", ignoreCase = true) ||
            category.contains("V&R", ignoreCase = true) ||
            category.equals("v_y_r_cb", ignoreCase = true) ||
            category.equals("vr", ignoreCase = true) ||
            (titularVr.isNotBlank() && category.equals(titularVr.trim(), ignoreCase = true))
        ) {
            return "V&R"
        }
        if (category.equals("Otros", ignoreCase = true) ||
            category.equals("Otro", ignoreCase = true) ||
            (titularOtros.isNotBlank() && category.equals(titularOtros.trim(), ignoreCase = true))
        ) {
            return "Otros"
        }
        if (category.equals("Mío", ignoreCase = true) ||
            category.equals("Mio", ignoreCase = true) ||
            category.equals("Propios", ignoreCase = true) ||
            category.equals("Propio", ignoreCase = true) ||
            category.equals("propiedad", ignoreCase = true) ||
            (titularPropios.isNotBlank() && category.equals(titularPropios.trim(), ignoreCase = true))
        ) {
            return "Mío"
        }

        return if (category.isNotBlank()) category else "Mío"
    }

    /**
     * Resuelve de forma normalizada la categoría de propietario de un huerto ("Mío", "V&R", "Otros")
     */
    fun getOrchardOwnerCategory(orchard: OrchardEntity?): String {
        if (orchard == null) return "Mío"
        return when (orchard.ownerType.lowercase().trim()) {
            "v_y_r_cb", "vr", "v&r" -> "V&R"
            "otros" -> "Otros"
            else -> "Mío"
        }
    }

    /**
     * Exporta los datos reales de la aplicación a un archivo .xlsx profesional multichoja con fórmulas.
     */
    fun exportOperationsComprehensiveCsv(
        context: Context,
        parts: List<WorkPartEntity>,
        orchards: List<OrchardEntity>,
        huertoFilter: String = "Todos los Huertos",
        varietyFilter: String = "Todas las Variedades",
        taskFilter: String = "Todas las Tareas",
        ownerFilter: String = "Todos",
        startDateStr: String = "",
        endDateStr: String = ""
    ) {
        try {
            val currentYear = yearFormat.format(Date()).toInt()
            val campaign = "$currentYear/${currentYear + 1}"
            val campaignClean = "${currentYear}-${currentYear + 1}"
            val dateStamp = dateFilenameFormat.format(Date())

            val fileName = "VRAgro_Exportacion_${campaignClean}_$dateStamp.xlsx"

            // Directorio temporal / caché para compartir vía FileProvider
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val file = File(exportDir, fileName)

            // Generar libro de trabajo multichoja
            val workbook = AgriculturalExcelGenerator.buildWorkbook(
                parts = parts,
                orchards = orchards,
                campaignName = campaign,
                isDemoMock = false
            )

            workbook.writeToFile(file)

            shareXlsxFile(context, file, fileName, "Exportación Agrícola Completa ($campaign)")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar el archivo Excel: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Exporta una campaña completa de 1 año con datos realistas y densos de prueba en formato .xlsx.
     */
    fun exportMock1YearExcel(context: Context) {
        try {
            val currentYear = yearFormat.format(Date()).toInt()
            val campaign = "$currentYear/${currentYear + 1}"
            val campaignClean = "${currentYear}-${currentYear + 1}"
            val dateStamp = dateFilenameFormat.format(Date())

            val fileName = "VRAgro_Muestra_1Anio_${campaignClean}_$dateStamp.xlsx"

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val file = File(exportDir, fileName)

            val workbook = AgriculturalExcelGenerator.buildWorkbook(
                parts = emptyList(),
                orchards = emptyList(),
                campaignName = campaign,
                isDemoMock = true
            )

            workbook.writeToFile(file)

            shareXlsxFile(context, file, fileName, "Muestra Agrícola Completa 1 Año ($campaign)")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar el ejemplo Excel: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
            putExtra(Intent.EXTRA_TEXT, "Adjunto informe agrícola profesional generado por V&R Agro: $fileName")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Abrir o Compartir Excel Agrícola").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(chooser)
        Toast.makeText(context, "Excel generado con éxito (${file.length() / 1024} KB)", Toast.LENGTH_SHORT).show()
    }
}
