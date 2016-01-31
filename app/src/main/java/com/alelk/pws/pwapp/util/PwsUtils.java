package com.alelk.pws.pwapp.util;

import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.LeadingMarginSpan;

import com.alelk.pws.database.data.BookEdition;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alelk on 30.01.2016.
 */
public class PwsUtils {
    public static final String PSALM_NUMBER_REGEX = "(\\w{1,30})=(\\d{1,6})";
    public static final String DELIMETER = ",";
    public static String convertPsalmNumbersToString(Map<BookEdition, Integer> numbers) {
        if (numbers == null) return null;
        String s = "";
        for (BookEdition bookEdition : numbers.keySet()) {
            s += (s == "" ? "" : DELIMETER) + bookEdition.getSignature() + "=" + numbers.get(bookEdition);
        }
        return s;
    }
    public static HashMap<BookEdition, Integer> parsePsalmNumbers(String s) {
        if (s == null) return  null;
        HashMap<BookEdition, Integer> numbers = new HashMap<>();
        Pattern pattern = Pattern.compile(PSALM_NUMBER_REGEX);
        for (String numberTxt : s.split(DELIMETER)) {
            Matcher matcher = pattern.matcher(numberTxt);
            if (matcher.find()) {
                BookEdition bookEdition = BookEdition.getInstanceBySignature(matcher.group(1));
                Integer number = null;
                try {
                    number = Integer.parseInt(matcher.group(2));
                } catch (NumberFormatException e) {}
                if (bookEdition != null && number != null) {
                    numbers.put(bookEdition, number);
                }
            }
        }
        if (numbers.size() == 0) return null;
        return  numbers;
    }

    public static SpannableString buildIndentedText(String text, int marginFirstLine, int marginNextLine) {
        SpannableString s = new SpannableString(text);
        s.setSpan(new LeadingMarginSpan.Standard(marginFirstLine, marginNextLine), 0, text.length(), 0);
        return s;
    }
}
