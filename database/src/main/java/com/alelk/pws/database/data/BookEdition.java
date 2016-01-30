package com.alelk.pws.database.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by alelkin on 26.03.2015.
 */
public enum BookEdition implements Parcelable {
    PV3055 ("PV3055"),
    GUSLI ("Gusli"),
    CHYMNS ("CHymns"),
    CPsalms ("CPsalms"),
    SDP ("SDP"),
    Tympan ("Tympan"),
    Kimval ("Kimval"),
    FCPSALMS ("FCPsalms"),
    SLAVIT_BEZ_ZAPINKI ("SlavitBezZapinki"),
    PVNL ("PVNL"),
    ZARYA ("Zarja"),
    SVIREL ("Svirel"),
    NNAPEVY ("NNapevy"),
    CHUDNY_KRAY ("ChudnyKray"),
    NPE ("NPE");


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

    @Override
    public String toString() {
        return getSignature();
    }

    public static final Parcelable.Creator<BookEdition> CREATOR
            = new Creator<BookEdition>() {
        @Override
        public BookEdition createFromParcel(Parcel source) {
            return getInstanceBySignature(source.readString());
        }

        @Override
        public BookEdition[] newArray(int size) {
            return new BookEdition[size];
        }
    };


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getSignature());
    }
}
