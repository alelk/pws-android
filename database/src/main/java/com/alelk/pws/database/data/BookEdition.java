package com.alelk.pws.database.data;

/**
 * Created by alelkin on 26.03.2015.
 */
public enum BookEdition {
    GUSLI ("Gusli"),
    PV2000 ("PV2000");
    private String signature;

    BookEdition(String signature) {
        this.signature = signature;
    }

    public String getSignature() {
        return signature;
    }
}
