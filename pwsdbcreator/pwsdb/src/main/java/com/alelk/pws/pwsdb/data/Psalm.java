package com.alelk.pws.pwsdb.data;

import com.alelk.pws.pwsdb.exception.PwsDatabaseIncorrectValueException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by Alex Elkin on 25.03.2015.
 */
public class Psalm extends PwsPsalmObject implements PwsObject {

    private Map<BookEdition, Integer> numbers;

    public void addNumber(BookEdition bookEdition, int psalmNumber) throws PwsDatabaseIncorrectValueException {
        if (bookEdition == null || psalmNumber <= 0) {
            throw new PwsDatabaseIncorrectValueException();
        }
        if (numbers == null) {
            numbers = new HashMap<>();
        }
        numbers.put(bookEdition, psalmNumber);
    }

    /**
     * Get psalm number of specified book edition
     * @param bookEdition book edition
     * @return psalm number if book edition is found for this psalm. Returns null otherwise.
     */
    public Integer getNumber(BookEdition bookEdition) {
        if (numbers == null || !numbers.containsKey(bookEdition)) {
            return null;
        }
        return numbers.get(bookEdition);
    }

    public SortedSet<BookEdition> getBookEditions() {
        if (getNumbers() == null || getNumbers().isEmpty()) {
            return null;
        }
        SortedSet<BookEdition> bookEditions = new TreeSet<>();
        bookEditions.addAll(numbers.keySet());
        return bookEditions;
    }

    public Map<BookEdition, Integer> getNumbers() {
        return numbers;
    }

    public void setNumbers(Map<BookEdition, Integer> numbers) {
        this.numbers = numbers;
    }

    private static class NumberComparator implements Comparator<Psalm> {
        private BookEdition bookEdition;
        public NumberComparator(BookEdition bookEdition) {
            this.bookEdition = bookEdition;
        }
        @Override
        public int compare(Psalm psalm1, Psalm psalm2) {
            Integer psalmNumber1 = psalm1.getNumber(bookEdition);
            Integer psalmNumber2 = psalm2.getNumber(bookEdition);
            if (psalmNumber1 == null && psalmNumber2 == null) return 0;
            else if (psalmNumber1 == null) return -1;
            else if (psalmNumber2 == null) return 1;
            return psalmNumber1.compareTo(psalmNumber2);
        }
    }

    public static Comparator<Psalm> getNumberComparator(BookEdition bookEdition) {
        return new NumberComparator(bookEdition);
    }

    public String toString() {
        String s = "Psalm v" + super.getVersion() + " {" +
                "name='" + super.getName() +
                "' author='" + super.getAuthor() +
                "' translator='" + super.getTranslator() +
                "' composer='" + super.getComposer() +
                "' year='" + super.getYear() +
                "' annotation='" + super.getAnnotation() +
                "' locale='" + super.getLocale() +
                "' number=[ ";
        if (numbers != null && !numbers.isEmpty()) {
            for (BookEdition bookEdition : this.numbers.keySet()) {
                s += this.numbers.get(bookEdition) + "->" + bookEdition + " ";
            }
        }
        s += "] " +
                "psalmPartsTypes=[ ";
        if (super.getPsalmParts() != null && !super.getPsalmParts().isEmpty()) {
            for (int i : super.getPsalmParts().keySet()) {
                s += "#" + i + "-" + super.getPsalmParts().get(i).getPsalmType() + " ";
            }
        }
        s += "] " +
                "tonalities=[ ";
        for (String tonality : super.getTonalities()) {
            s += tonality + " ";
        }
        s += "]}";
        return s;
    }

    protected Psalm getInstance() {
        return this;
    }
}
