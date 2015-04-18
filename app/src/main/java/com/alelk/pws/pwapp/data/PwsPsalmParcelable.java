package com.alelk.pws.pwapp.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.PsalmPart;
import com.alelk.pws.database.data.PsalmPartType;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Alex on 18.04.2015.
 */
public class PwsPsalmParcelable implements PwsParcelableObject {
    private String name;
    private String version;
    private String author;
    private String translator;
    private String composer;
    private Date year;
    private List<String> tonalities = new ArrayList<>();
    private HashMap<BookEdition, Integer> numbers = new HashMap<>();
    private HashMap<Integer, PsalmPart> psalmParts = new HashMap<>();

    public PwsPsalmParcelable(Psalm psalm) {
        name = psalm.getName();
        version = psalm.getVersion();
        author = psalm.getAuthor();
        translator = psalm.getTranslator();
        composer = psalm.getComposer();
        year = psalm.getYear();
        tonalities = psalm.getTonalities();
        numbers.putAll(psalm.getNumbers());
        psalmParts.putAll(psalm.getPsalmParts());
    }

    public static final Creator<PwsPsalmParcelable> CREATOR = new Creator<PwsPsalmParcelable>() {

        @Override
        public PwsPsalmParcelable createFromParcel(Parcel source) {
            return new PwsPsalmParcelable(source);
        }

        @Override
        public PwsPsalmParcelable[] newArray(int size) {
            return new PwsPsalmParcelable[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getName());
        dest.writeString(getVersion());
        dest.writeString(getAuthor());
        dest.writeString(getTranslator());
        dest.writeString(getComposer());
        dest.writeSerializable(getYear());
        dest.writeStringList(getTonalities());
        dest.writeSerializable(getNumbers());
        dest.writeSerializable(psalmParts);
    }

    private PwsPsalmParcelable(Parcel parcel) {
        name = parcel.readString();
        version = parcel.readString();
        author = parcel.readString();
        translator = parcel.readString();
        composer = parcel.readString();
        year = (Date) parcel.readSerializable();
        parcel.readStringList(tonalities);
        numbers = (HashMap<BookEdition, Integer>) parcel.readSerializable();
        psalmParts = (HashMap<Integer, PsalmPart>) parcel.readSerializable();
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public String getTranslator() {
        return translator;
    }

    public String getComposer() {
        return composer;
    }

    public Date getYear() {
        return year;
    }

    public List<String> getTonalities() {
        return tonalities;
    }

    public HashMap<BookEdition, Integer> getNumbers() {
        return numbers;
    }

    public SortedMap<Integer, PsalmPart> getPsalmParts() {
        SortedMap<Integer, PsalmPart> psalmParts = new TreeMap<>();
        psalmParts.putAll(this.psalmParts);
        return  psalmParts;
    }
}
