package com.alelk.pws.database.data;

/**
 * Created by alelkin on 26.03.2015.
 */
public enum BookEdition {
    PV3055 ("PV3055"),
    GUSLI ("Gusli"),
    CHYMNS ("CHymns"),
    CPsalms ("CPsalms"),
    SDP ("SDP"),
    Tympan ("Tympan"),
    Kimval ("Kimval");
    private String signature;

    BookEdition(String signature) {
        this.signature = signature;
    }

    public String getSignature() {
        return signature;
    }

    public static BookEdition getInstanceBySignature(String signature) {
        for (BookEdition bookEdition : BookEdition.values()) {
            if (bookEdition.getSignature().equalsIgnoreCase(signature)) {
                return bookEdition;
            }
        }
        return null;
    }
}
