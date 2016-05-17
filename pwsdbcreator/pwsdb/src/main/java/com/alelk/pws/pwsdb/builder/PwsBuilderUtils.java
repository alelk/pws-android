package com.alelk.pws.pwsdb.builder;

import com.alelk.pws.pwsdb.query.PwsDatabaseQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alex Elkin on 06.05.2015.
 */
public abstract class PwsBuilderUtils {

    /**
     * Parse string with numbers separated by non-digit characters
     * @param numbers String with numbers separated by non-digit characters
     * @return Numbers list
     */
    protected List<Integer> parseNumbersFromString(String numbers) {
        if (numbers == null || numbers.isEmpty()) return null;
        List<Integer> nums = new ArrayList<>();
        for (String num : numbers.split(PwsDatabaseQuery.MULTIVALUE_DELIMITER)) {
            try {
                nums.add(Integer.parseInt(num.trim()));
            } catch (NumberFormatException e) {
            }
        }
        if (!nums.isEmpty()) return nums;
        return null;
    }
}
