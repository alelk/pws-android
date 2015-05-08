package com.alelk.pws.database.builder;

import android.text.TextUtils;

import com.alelk.pws.database.helper.PwsDatabaseHelper;
import com.alelk.pws.database.query.PwsDatabaseQuery;
import com.alelk.pws.database.query.PwsDatabaseQueryUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
