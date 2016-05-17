package com.alelk.pws.pwsdb.data;

/**
 * Created by alelk on 20.02.2016.
 */
public class FavoritePsalm extends Psalm {

    private int position;
    private BookEdition bookEdition;
    private int number;

    public FavoritePsalm(Psalm psalm, BookEdition bookEdition,  int position) {
        super.setName(psalm.getName());
        super.setVersion(psalm.getVersion());
        super.setAuthor(psalm.getAuthor());
        super.setTranslator(psalm.getTranslator());
        super.setComposer(psalm.getComposer());
        super.setYear(psalm.getYear());
        super.setAnnotation(psalm.getAnnotation());
        super.setTonalities(psalm.getTonalities());
        super.setPsalmParts(psalm.getPsalmParts());
        super.setNumbers(psalm.getNumbers());
        this.bookEdition = bookEdition;
        this.number = psalm.getNumber(bookEdition);
        this.position = position;
    }

    public Psalm getPsalm() {
        return super.getInstance();
    }

    public int getNumber() {
        return number;
    }

    public BookEdition getBookEdition() {
        return bookEdition;
    }

    public int getPosition() {
        return position;
    }

}
