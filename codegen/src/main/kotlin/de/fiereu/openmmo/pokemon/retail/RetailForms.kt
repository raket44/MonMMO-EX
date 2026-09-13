package de.fiereu.openmmo.pokemon.retail

/**
 * The retail client's own form catalogue (launcher :stageRetailData -> monmmo/retail-forms.csv, from
 * the dex dump): per species, each form number, the record it uses, its name and whether it is a
 * costume. A form whose record is the species itself is appearance-only (Unown B is 201 form 1);
 * one with its own record carries its own data (Rotom Heat is record 657).
 */
object RetailForms {
  data class Row(
      val speciesId: Int,
      val formId: Int,
      val recordId: Int,
      val name: String,
      val isCostume: Boolean,
      val isReleased: Boolean,
  )

  private val rows: List<Row> by lazy {
    RetailForms::class.java.getResourceAsStream("/monmmo/retail-forms.csv")?.bufferedReader()?.useLines { lines ->
      lines
          .mapNotNull { line ->
            val cells = line.split(';')
            if (cells.size < 6) return@mapNotNull null
            Row(
                speciesId = cells[0].toIntOrNull() ?: return@mapNotNull null,
                formId = cells[1].toIntOrNull() ?: return@mapNotNull null,
                recordId = cells[2].toIntOrNull() ?: return@mapNotNull null,
                name = cells[3],
                isCostume = cells[4] == "true",
                isReleased = cells[5] == "true",
            )
          }
          .toList()
    }.orEmpty()
  }

  private val bySpeciesForm: Map<Pair<Int, Int>, Row> by lazy {
    rows.associateBy { it.speciesId to it.formId }
  }

  fun all(): List<Row> = rows

  fun get(speciesId: Int, formId: Int): Row? = bySpeciesForm[speciesId to formId]

  /**
   * The record a form with data of its own uses (Deoxys 386 form 3 -> 652), or null when the form
   * is appearance-only, a costume, or not a form the client knows.
   */
  fun recordOf(speciesId: Int, formId: Int): Int? =
      get(speciesId, formId)?.takeIf { !it.isCostume && it.recordId != speciesId }?.recordId
}
