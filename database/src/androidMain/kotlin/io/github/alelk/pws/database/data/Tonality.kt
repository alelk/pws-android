package io.github.alelk.pws.database.data

import android.content.Context
import io.github.alelk.pws.database.R

/**
 * Tonalities signature map to resources
 *
 * Created by Alex Elkin on 12.05.2016.
 */
@Deprecated("")
enum class Tonality(val signature: String, private val labelId: Int) {
  A_MAJOR("A major", R.string.a_major),
  A_MINOR("A minor", R.string.a_minor),
  A_FLAT_MAJOR("A-flat major", R.string.a_flat_major),
  A_FLAT_MINOR("A-flat minor", R.string.a_flat_minor),
  A_SHARP_MINOR("A-sharp minor", R.string.a_sharp_minor),

  B_MAJOR("B major", R.string.b_major),
  B_MINOR("B minor", R.string.b_minor),
  B_FLAT_MAJOR("B-flat major", R.string.b_flat_major),
  B_FLAT_MINOR("B-flat minor", R.string.b_flat_minor),

  C_MAJOR("C major", R.string.c_major),
  C_MINOR("C minor", R.string.c_minor),
  C_FLAT_MAJOR("C-flat major", R.string.c_flat_major),
  C_SHARP_MAJOR("C-sharp major", R.string.c_sharp_major),
  C_SHARP_MINOR("C-sharp minor", R.string.c_sharp_minor),

  D_MAJOR("D major", R.string.d_major),
  D_MINOR("D minor", R.string.d_minor),
  D_FLAT_MAJOR("D-flat major", R.string.d_flat_major),
  D_SHARP_MINOR("D-sharp minor", R.string.d_sharp_minor),

  E_MAJOR("E major", R.string.e_major),
  E_MINOR("E minor", R.string.e_minor),
  E_FLAT_MAJOR("E-flat major", R.string.e_flat_major),
  E_FLAT_MINOR("E-flat minor", R.string.e_flat_minor),

  F_MAJOR("F major", R.string.f_major),
  F_MINOR("F minor", R.string.f_minor),
  F_SHARP_MAJOR("F-sharp major", R.string.f_sharp_major),
  F_SHARP_MINOR("F-sharp minor", R.string.f_sharp_minor),

  G_MAJOR("G major", R.string.g_major),
  G_MINOR("G minor", R.string.g_minor),
  G_FLAT_MAJOR("G-flat major", R.string.g_flat_major),
  G_SHARP_MINOR("G-sharp minor", R.string.g_sharp_minor);

  fun getLabel(context: Context): String {
    return context.applicationContext.getString(labelId)
  }

  companion object {
    fun getInstanceBySignature(signature: String): Tonality? = values().firstOrNull { it.signature.equals(signature, ignoreCase = true) }
  }
}