package com.alelk.pws.database.data;

import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alelkin on 25.03.2015.
 */
public abstract class PsalmPart implements PwsObject{
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

    public void setNumbers(List<Integer> numbers) throws PwsDatabaseIncorrectValueException{
        this.numbers = new ArrayList<>(5);
        if (numbers != null && numbers.size() > 0) {
            for (int number : numbers) {
                if (number <= 0) {
                    throw new PwsDatabaseIncorrectValueException();
                }
            }
        }
        this.numbers = numbers;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
