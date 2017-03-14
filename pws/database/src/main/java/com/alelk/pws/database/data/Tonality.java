package com.alelk.pws.database.data;

import android.content.Context;

import com.alelk.pws.database.R;

/**
 * Tonalities signature map to resources
 *
 * Created by Alex Elkin on 12.05.2016.
 */
public enum Tonality {

    C_FLAT_MAJOR("C-flat major", R.string.c_flat_major),
    A_FLAT_MINOR("A-flat minor", R.string.a_flat_minor),
    G_FLAT_MAJOR("G-flat major", R.string.g_flat_major),
    E_FLAT_MINOR("E-flat minor", R.string.e_flat_minor),
    D_FLAT_MAJOR("D-flat major", R.string.d_flat_major),
    B_FLAT_MINOR("B-flat minor", R.string.b_flat_minor),
    A_FLAT_MAJOR("A-flat major", R.string.a_flat_major),
    F_MINOR("F minor", R.string.f_minor),
    E_FLAT_MAJOR("E-flat major", R.string.e_flat_major),
    C_MINOR("C minor", R.string.c_minor),
    B_FLAT_MAJOR("B-flat major", R.string.b_flat_major),
    G_MINOR("G minor", R.string.g_minor),
    F_MAJOR("F major", R.string.f_major),
    D_MINOR("D minor", R.string.d_minor),
    C_MAJOR("C major", R.string.c_major),
    A_MINOR("A minor", R.string.a_minor),
    G_MAJOR("G major", R.string.g_major),
    E_MINOR("E minor", R.string.e_minor),
    D_MAJOR("D major", R.string.d_major),
    B_MINOR("B minor", R.string.b_minor),
    A_MAJOR("A major", R.string.a_major),
    F_SHARP_MINOR("F-sharp minor", R.string.f_sharp_minor),
    E_MAJOR("E major", R.string.e_major),
    C_SHARP_MINOR("C-sharp minor", R.string.c_sharp_minor),
    B_MAJOR("B major", R.string.b_major),
    G_SHARP_MINOR("G-sharp minor", R.string.g_sharp_minor),
    F_SHARP_MAJOR("F-sharp major", R.string.f_sharp_major),
    D_SHARP_MINOR("D-sharp minor", R.string.d_sharp_minor),
    C_SHARP_MAJOR("C-sharp major", R.string.c_sharp_major),
    A_SHARP_MINOR("A-sharp minor", R.string.a_sharp_minor);

    private String signature;
    private int labelId;

    Tonality(String signature, int labelId) {
        this.signature = signature;
        this.labelId = labelId;
    }

    public String getSignature() {
        return signature;
    }

    public String getLabel(Context context) {
        return context.getApplicationContext().getString(labelId);
    }

    public static Tonality getInstanceBySignature(String signature) {
        for (Tonality tonality : Tonality.values()) {
            if (tonality.getSignature().equalsIgnoreCase(signature)) {
                return tonality;
            }
        }
        return null;
    }
}
