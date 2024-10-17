package io.github.alelk.pws.database.common.model

enum class Tonality(val identifier: String) {
  A_MAJOR("a major"),
  A_MINOR("a minor"),
  A_FLAT_MAJOR("a-flat major"),
  A_FLAT_MINOR("a-flat minor"),
  A_SHARP_MINOR("a-sharp minor"),

  B_MAJOR("b major"),
  B_MINOR("b minor"),
  B_FLAT_MAJOR("b-flat major"),
  B_FLAT_MINOR("b-flat minor"),

  C_MAJOR("c major"),
  C_MINOR("c minor"),
  C_FLAT_MAJOR("c-flat major"),
  C_SHARP_MAJOR("c-sharp major"),
  C_SHARP_MINOR("c-sharp minor"),

  D_MAJOR("d major"),
  D_MINOR("d minor"),
  D_FLAT_MAJOR("d-flat major"),
  D_SHARP_MINOR("d-sharp minor"),

  E_MAJOR("e major"),
  E_MINOR("e minor"),
  E_FLAT_MAJOR("e-flat major"),
  E_FLAT_MINOR("e-flat minor"),

  F_MAJOR("f major"),
  F_MINOR("f minor"),
  F_SHARP_MAJOR("f-sharp major"),
  F_SHARP_MINOR("f-sharp minor"),

  G_MAJOR("g major"),
  G_MINOR("g minor"),
  G_FLAT_MAJOR("g-flat major"),
  G_SHARP_MINOR("g-sharp minor");

  companion object {
    fun fromIdentifier(identifier: String): Tonality =
      checkNotNull(entries.firstOrNull { it.identifier == identifier.lowercase() }) { "Unknown tonality: $identifier" }
  }
}