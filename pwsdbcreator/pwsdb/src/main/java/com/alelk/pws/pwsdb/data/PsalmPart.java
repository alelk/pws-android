package com.alelk.pws.pwsdb.data;

import com.alelk.pws.pwsdb.exception.PwsDatabaseIncorrectValueException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alelkin on 25.03.2015.
 */
public abstract class PsalmPart implements PwsObject, Serializable{
    private List<Integer> numbers;
    private String text;

    public PsalmPart() {
        this.numbers = new ArrayList<>(5);
        this.text = null;
    }

    public abstract PsalmPartType getPsalmType();

    public PsalmPart(List<Integer> numbers, String text) throws PwsDatabaseIncorrectValueException {
        setNumbers(numbers);
        setText(text);
    }

    public List<Integer> getNumbers() {
        return numbers;
    }

    public void setNumbers(List<Integer> numbers){
        this.numbers = numbers;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "PsalmPart{" +
                "numbers=" + numbers == null ? "": numbers +
                ", text='" + text == null ? "": text.replaceAll("\\W", "") + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof PsalmPart)) return false;
        if (((PsalmPart) o).getPsalmType() != getPsalmType()) return false;
        if (!((PsalmPart) o).getText().equals(getText())) return false;
        if (!((PsalmPart) o).getNumbers().equals(getNumbers())) return false;
        return true;
    }
}
