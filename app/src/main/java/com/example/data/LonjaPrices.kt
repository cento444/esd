package com.example.data

data class VarietyPrice(
    val varietyName: String,
    val category: String, // "Mandarinas / Clementinas", "Naranjas", "Aguacates", "Híbridos"
    val priceText: String, // "0.28€/kg", "2.45€/kg"
    val currentPrice: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val variationPercent: Double, // +2.0, -1.5
    val source: String = "Lonja de Cítricos de Valencia",
    val updateDate: String = "Semana actual"
)

object LonjaPriceDatabase {
    val varieties: List<VarietyPrice> = listOf(
        // Satsumas
        VarietyPrice("Clausellina", "Mandarinas / Satsuma", "0.28€/kg", 0.28, 0.24, 0.32, 0.0),
        VarietyPrice("Iwasaki", "Mandarinas / Satsuma", "0.34€/kg", 0.34, 0.30, 0.38, 0.0),
        VarietyPrice("Okitsu", "Mandarinas / Satsuma", "0.32€/kg", 0.32, 0.28, 0.36, 1.0),
        VarietyPrice("Owari", "Mandarinas / Satsuma", "0.27€/kg", 0.27, 0.24, 0.30, 0.0),

        // Clementinas y Mandarinas
        VarietyPrice("Arrufatina", "Mandarinas / Clementinas", "0.42€/kg", 0.42, 0.38, 0.46, 0.0),
        VarietyPrice("Basol", "Mandarinas / Clementinas", "0.55€/kg", 0.55, 0.50, 0.60, 2.0),
        VarietyPrice("Clemenpons", "Mandarinas / Clementinas", "0.32€/kg", 0.32, 0.28, 0.36, 0.5),
        VarietyPrice("Clemenrubi", "Mandarinas / Clementinas", "0.57€/kg", 0.57, 0.52, 0.62, 4.0),
        VarietyPrice("Clemenules", "Mandarinas / Clementinas", "0.50€/kg", 0.50, 0.45, 0.55, 2.0),
        VarietyPrice("Esbal", "Mandarinas / Clementinas", "0.30€/kg", 0.30, 0.26, 0.34, 0.0),
        VarietyPrice("Fina", "Mandarinas / Clementinas", "0.35€/kg", 0.35, 0.30, 0.40, 0.0),
        VarietyPrice("Hernandina", "Mandarinas / Clementinas", "0.45€/kg", 0.45, 0.40, 0.50, 1.8),
        VarietyPrice("Loretina", "Mandarinas / Clementinas", "0.48€/kg", 0.48, 0.42, 0.54, 2.0),
        VarietyPrice("Marisol", "Mandarinas / Clementinas", "0.70€/kg", 0.70, 0.64, 0.76, -2.0),
        VarietyPrice("Mioro", "Mandarinas / Clementinas", "0.46€/kg", 0.46, 0.40, 0.52, 0.0),
        VarietyPrice("Oronules", "Mandarinas / Clementinas", "0.57€/kg", 0.57, 0.52, 0.62, 3.5),
        VarietyPrice("Orogrande", "Mandarinas / Clementinas", "0.50€/kg", 0.50, 0.45, 0.55, 1.5),
        VarietyPrice("Orogrós", "Mandarinas / Clementinas", "0.57€/kg", 0.57, 0.52, 0.62, 2.5),
        VarietyPrice("Oroval", "Mandarinas / Clementinas", "0.32€/kg", 0.32, 0.28, 0.36, 0.0),
        VarietyPrice("Sando", "Mandarinas / Clementinas", "0.50€/kg", 0.50, 0.45, 0.55, 1.0),
        VarietyPrice("Tomatera", "Mandarinas / Clementinas", "0.30€/kg", 0.30, 0.26, 0.34, 0.0),

        // Híbridos y Mandarinas Tardías
        VarietyPrice("Afourer", "Mandarinas / Híbridos", "0.68€/kg", 0.68, 0.60, 0.76, 0.0),
        VarietyPrice("Clemenvilla (Nova)", "Mandarinas / Híbridos", "0.35€/kg", 0.35, 0.30, 0.40, 1.0),
        VarietyPrice("Garbí", "Mandarinas / Híbridos", "0.45€/kg", 0.45, 0.40, 0.50, 0.0),
        VarietyPrice("Leanri", "Mandarinas / Híbridos", "0.85€/kg", 0.85, 0.75, 0.95, 3.0),
        VarietyPrice("Murcott", "Mandarinas / Híbridos", "0.55€/kg", 0.55, 0.48, 0.62, 0.0),
        VarietyPrice("Nadorcott", "Mandarinas / Híbridos", "0.82€/kg", 0.82, 0.75, 0.90, -1.0),
        VarietyPrice("Orri", "Mandarinas / Híbridos", "1.35€/kg", 1.35, 1.20, 1.50, 4.5),
        VarietyPrice("Ortanique", "Mandarinas / Híbridos", "0.27€/kg", 0.27, 0.24, 0.30, 0.0),
        VarietyPrice("Safor", "Mandarinas / Híbridos", "0.48€/kg", 0.48, 0.42, 0.54, 0.0),
        VarietyPrice("Spring Sunshine", "Mandarinas / Híbridos", "0.90€/kg", 0.90, 0.80, 1.00, 2.0),
        VarietyPrice("Tango", "Mandarinas / Híbridos", "0.82€/kg", 0.82, 0.74, 0.90, 2.2),

        // Naranjas - Grupo Navel
        VarietyPrice("Barnfield", "Naranjas - Navel", "0.39€/kg", 0.39, 0.35, 0.44, 1.8),
        VarietyPrice("Chislett", "Naranjas - Navel", "0.39€/kg", 0.39, 0.35, 0.44, 2.0),
        VarietyPrice("Lane Late", "Naranjas - Navel", "0.35€/kg", 0.35, 0.31, 0.40, 3.0),
        VarietyPrice("Navel", "Naranjas - Navel", "0.28€/kg", 0.28, 0.24, 0.32, 0.0),
        VarietyPrice("Navelate", "Naranjas - Navel", "0.40€/kg", 0.40, 0.36, 0.44, 1.5),
        VarietyPrice("Navelina", "Naranjas - Navel", "0.30€/kg", 0.30, 0.26, 0.34, 0.0),
        VarietyPrice("Newhall", "Naranjas - Navel", "0.25€/kg", 0.25, 0.22, 0.28, 0.0),
        VarietyPrice("Powell (Navel Powell)", "Naranjas - Navel", "0.39€/kg", 0.39, 0.35, 0.44, 2.5),
        VarietyPrice("Rohde", "Naranjas - Navel", "0.36€/kg", 0.36, 0.32, 0.40, 1.0),
        VarietyPrice("Washington Navel", "Naranjas - Navel", "0.28€/kg", 0.28, 0.24, 0.32, 0.0),

        // Naranjas - Grupo Blancas / Tardías
        VarietyPrice("Barberina", "Naranjas - Blancas", "0.47€/kg", 0.47, 0.42, 0.52, 1.5),
        VarietyPrice("Delta Seedless", "Naranjas - Blancas", "0.35€/kg", 0.35, 0.30, 0.40, 1.0),
        VarietyPrice("Midknight", "Naranjas - Blancas", "0.47€/kg", 0.47, 0.42, 0.52, 1.0),
        VarietyPrice("Salustiana", "Naranjas - Blancas", "0.31€/kg", 0.31, 0.27, 0.35, 0.0),
        VarietyPrice("Valencia Late", "Naranjas - Blancas", "0.35€/kg", 0.35, 0.30, 0.40, 1.2),

        // Naranjas - Sanguinas
        VarietyPrice("Sanguinelli", "Naranjas - Sanguinas", "0.45€/kg", 0.45, 0.40, 0.50, 2.0),

        // Limones y Pomelos
        VarietyPrice("Eureka", "Limones", "0.38€/kg", 0.38, 0.32, 0.44, 0.0),
        VarietyPrice("Limón Fino", "Limones", "0.35€/kg", 0.35, 0.30, 0.40, 1.5),
        VarietyPrice("Limón Verna", "Limones", "0.40€/kg", 0.40, 0.35, 0.45, 0.0),
        VarietyPrice("Pomelo Rio Red", "Pomelos", "0.32€/kg", 0.32, 0.28, 0.36, 0.0),
        VarietyPrice("Pomelo Star Ruby", "Pomelos", "0.30€/kg", 0.30, 0.26, 0.34, 0.0),

        // Aguacates
        VarietyPrice("Aguacate Bacon", "Aguacates", "1.70€/kg", 1.70, 1.50, 1.90, 0.0),
        VarietyPrice("Aguacate Fuerte", "Aguacates", "1.80€/kg", 1.80, 1.60, 2.00, 0.0),
        VarietyPrice("Aguacate Hass", "Aguacates", "2.45€/kg", 2.45, 2.35, 2.60, -1.5),
        VarietyPrice("Aguacate Lamb Hass", "Aguacates", "2.15€/kg", 2.15, 2.00, 2.30, 1.0)
    )

    fun getPriceForVariety(variety: String): VarietyPrice {
        val clean = variety.trim().lowercase()
        return varieties.firstOrNull { it.varietyName.lowercase() == clean }
            ?: varieties.firstOrNull { it.varietyName.lowercase().contains(clean) || clean.contains(it.varietyName.lowercase()) }
            ?: VarietyPrice(variety, "Cítricos", "0.30€/kg", 0.30, 0.25, 0.35, 0.0)
    }

    /**
     * Lista completa oficial de todas las frutas de la Lonja de Cítricos de Valencia
     * en estricto orden alfabético.
     */
    val allVarietyNamesAlphabetical: List<String> by lazy {
        varieties.map { it.varietyName }
            .distinct()
            .sortedWith(java.text.Collator.getInstance(java.util.Locale("es", "ES")))
    }

    val allVarietyNames: List<String>
        get() = allVarietyNamesAlphabetical
}
