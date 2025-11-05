package io.github.alelk.pws.android.app.feature.songs

import io.github.alelk.pws.android.app.R
import io.github.alelk.pws.domain.tonality.Tonality

val Tonality.labelId: Int
  get() =
    when (this) {
      Tonality.A_MAJOR -> R.string.a_major
      Tonality.A_MINOR -> R.string.a_minor
      Tonality.A_FLAT_MAJOR -> R.string.a_flat_major
      Tonality.A_FLAT_MINOR -> R.string.a_flat_minor
      Tonality.A_SHARP_MINOR -> R.string.a_sharp_minor
      Tonality.B_MAJOR -> R.string.b_major
      Tonality.B_MINOR -> R.string.b_minor
      Tonality.B_FLAT_MAJOR -> R.string.b_flat_major
      Tonality.B_FLAT_MINOR -> R.string.b_flat_minor
      Tonality.C_MAJOR -> R.string.c_major
      Tonality.C_MINOR -> R.string.c_minor
      Tonality.C_FLAT_MAJOR -> R.string.c_flat_major
      Tonality.C_SHARP_MAJOR -> R.string.c_sharp_major
      Tonality.C_SHARP_MINOR -> R.string.c_sharp_minor
      Tonality.D_MAJOR -> R.string.d_major
      Tonality.D_MINOR -> R.string.d_minor
      Tonality.D_FLAT_MAJOR -> R.string.d_flat_major
      Tonality.D_SHARP_MINOR -> R.string.d_sharp_minor
      Tonality.E_MAJOR -> R.string.e_major
      Tonality.E_MINOR -> R.string.e_minor
      Tonality.E_FLAT_MAJOR -> R.string.e_flat_major
      Tonality.E_FLAT_MINOR -> R.string.e_flat_minor
      Tonality.F_MAJOR -> R.string.f_major
      Tonality.F_MINOR -> R.string.f_minor
      Tonality.F_SHARP_MAJOR -> R.string.f_sharp_major
      Tonality.F_SHARP_MINOR -> R.string.f_sharp_minor
      Tonality.G_MAJOR -> R.string.g_major
      Tonality.G_MINOR -> R.string.g_minor
      Tonality.G_FLAT_MAJOR -> R.string.g_flat_major
      Tonality.G_SHARP_MINOR -> R.string.g_sharp_minor
    }