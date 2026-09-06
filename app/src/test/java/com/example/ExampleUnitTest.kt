package com.example

import com.example.data.model.SocialPostEntity
import com.example.ui.util.AppNotificationHelper
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun actionSummary_forNewTask_returnsAppropriateText() {
    val post = SocialPostEntity(
      authorName = "Ramón Ripoll",
      orchardName = "Hort de Baix",
      timestampText = "Ahora",
      content = "📅 Nueva tarea programada: Poda en 'Hort de Baix'"
    )
    val summary = AppNotificationHelper.getActionSummary(post)
    assertTrue("Debe indicar nueva tarea", summary.contains("nueva tarea", ignoreCase = true))
    assertEquals("new_task", AppNotificationHelper.getActionIconType(post))
  }

  @Test
  fun actionSummary_forCompletedTask_returnsAppropriateText() {
    val post = SocialPostEntity(
      authorName = "María Valero",
      orchardName = "La Plana",
      timestampText = "Ahora",
      content = "✅ Tarea completada: Desbroce en 'La Plana'"
    )
    val summary = AppNotificationHelper.getActionSummary(post)
    assertTrue("Debe indicar actividad finalizada", summary.contains("finalizó una actividad", ignoreCase = true))
    assertEquals("task_completed", AppNotificationHelper.getActionIconType(post))
  }

  @Test
  fun actionSummary_forModifiedOrchard_returnsAppropriateText() {
    val post = SocialPostEntity(
      authorName = "Vicente Martí",
      orchardName = "El Campillo",
      timestampText = "Ahora",
      content = "✏️ Huerto modificado: Actualización de datos de riego en 'El Campillo'"
    )
    val summary = AppNotificationHelper.getActionSummary(post)
    assertTrue("Debe indicar modificación de huerto", summary.contains("modificado un huerto", ignoreCase = true))
    assertEquals("orchard_modified", AppNotificationHelper.getActionIconType(post))
  }

  @Test
  fun actionSummary_forNewOrchard_returnsAppropriateText() {
    val post = SocialPostEntity(
      authorName = "Ramón Ripoll",
      orchardName = "Nova Parcela",
      timestampText = "Ahora",
      content = "🌱 Nueva parcela registrada: 'Nova Parcela'"
    )
    val summary = AppNotificationHelper.getActionSummary(post)
    assertTrue("Debe indicar alta de nuevo huerto", summary.contains("nuevo huerto", ignoreCase = true))
    assertEquals("new_orchard", AppNotificationHelper.getActionIconType(post))
  }

  @Test
  fun actionSummary_forFrostAlerts_returnsAppropriateTextAndIcon() {
    // Etapa 1: 7 días
    val post7d = SocialPostEntity(
      authorName = "Alerta de Helada (7 Días)",
      orchardName = "Hort de Baix",
      timestampText = "Ahora",
      content = "❄️📅 AVISO PREVENTIVO A 7 DÍAS: Riesgo de helada en 'Hort de Baix'",
      isAlert = true
    )
    val summary7d = AppNotificationHelper.getActionSummary(post7d)
    assertTrue("Debe indicar 7 días", summary7d.contains("7 días", ignoreCase = true))
    assertEquals("frost", AppNotificationHelper.getActionIconType(post7d))

    // Etapa 2: 48h / 2 días
    val post2d = SocialPostEntity(
      authorName = "Alerta de Helada (48 Horas)",
      orchardName = "Hort de Baix",
      timestampText = "Ahora",
      content = "❄️🚨 AVISO A 48 HORAS (Alta Fiabilidad): Previsión confirmada de helada en 'Hort de Baix'",
      isAlert = true
    )
    val summary2d = AppNotificationHelper.getActionSummary(post2d)
    assertTrue("Debe indicar 48h o 2 días", summary2d.contains("48h", ignoreCase = true) || summary2d.contains("2 días", ignoreCase = true))
    assertEquals("frost", AppNotificationHelper.getActionIconType(post2d))

    // Etapa 3: Tiempo Real
    val postRt = SocialPostEntity(
      authorName = "Alerta de Helada (Tiempo Real)",
      orchardName = "Hort de Baix",
      timestampText = "Ahora",
      content = "❄️⚠️ ¡ALERTA EN TIEMPO REAL!: El huerto 'Hort de Baix' ha alcanzado los 2 °C",
      isAlert = true
    )
    val summaryRt = AppNotificationHelper.getActionSummary(postRt)
    assertTrue("Debe indicar alerta inmediata", summaryRt.contains("Alerta Inmediata", ignoreCase = true))
    assertEquals("frost", AppNotificationHelper.getActionIconType(postRt))
  }

  @Test
  fun lonjaNotification_containsOnlyUserOrchardsAndNoFooterPhrase() {
    val post = SocialPostEntity(
      authorName = "Lonja de Cítricos de Valencia",
      orchardName = "Mercado y Lonjas",
      timestampText = "Semana 36 (Septiembre)",
      content = "• Hort de Baix (Clemenules): 0.28€/kg (+2.0% ↗)\n• La Plana (Navelina): 0.24€/kg (-1.5% ↘)"
    )
    assertFalse(
      "No debe contener la frase inferior del desglose en inicio",
      post.content.contains("consulte el desglose completo de cotizaciones por variedad en el panel de inicio", ignoreCase = true)
    )
    assertFalse(
      "No debe contener texto de preámbulo antes del precio",
      post.content.contains("NUEVA COTIZACIÓN SEMANAL", ignoreCase = true) ||
      post.content.contains("Cotizaciones oficiales para tus huertos", ignoreCase = true)
    )
    assertTrue("Debe contener el huerto Hort de Baix", post.content.contains("Hort de Baix"))
    assertTrue("Debe contener la cotización de Clemenules", post.content.contains("Clemenules"))
    assertTrue("Debe contener el huerto La Plana", post.content.contains("La Plana"))
    assertTrue("Debe contener la cotización de Navelina", post.content.contains("Navelina"))

    val summary = AppNotificationHelper.getActionSummary(post)
    assertTrue("El resumen debe mostrar directamente el precio sin texto superfluo", summary.contains("0.28€/kg"))
  }

  @Test
  fun doseCalculator_calculatesCorrectly_forMochilaAndTurbo() {
    val dose = 4.0 // 4 ml/L
    val mochila16 = dose * 16.0
    val turbo1000 = dose * 1000.0
    val custom200 = dose * 200.0

    assertEquals(64.0, mochila16, 0.001)
    assertEquals(4000.0, turbo1000, 0.001)
    assertEquals(800.0, custom200, 0.001)
  }

  @Test
  fun doseCalculator_handlesDecimalDoses() {
    val dose = 1.5 // 1.5 ml/L or g/L
    val mochila16 = dose * 16.0
    val turbo1000 = dose * 1000.0
    val custom200 = dose * 200.0

    assertEquals(24.0, mochila16, 0.001)
    assertEquals(1500.0, turbo1000, 0.001)
    assertEquals(300.0, custom200, 0.001)
  }
}

