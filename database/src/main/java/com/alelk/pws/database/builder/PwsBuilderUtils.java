package com.alelk.pws.database.builder;

import android.text.TextUtils;

import com.alelk.pws.database.query.PwsDatabaseQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Alex Elkin on 06.05.2015.
 */
public abstract class PwsBuilderUtils {
    protected List<Integer> parseNumbersFromString(String numbers) {
        List<Integer> nums = null;
        List<String> numList = Arrays.asList(TextUtils.split(numbers, PwsDatabaseQuery.MULTIVALUE_DELIMITER));
        if(!numList.isEmpty()) {
            nums = new ArrayList<>(numList.size());
            for (String num : numList) {
                num = num.trim();
                if (TextUtils.isEmpty(num)) {
                    try {
                        nums.add(Integer.parseInt(num));
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        return nums;
    }
}
