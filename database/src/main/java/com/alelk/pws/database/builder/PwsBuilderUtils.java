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
        List<Integer> nums = new ArrayList<>();
        String[] numList = null;
        if (numbers != null && !numbers.isEmpty()) {
            numList = numbers.split("\\D");
        }
        for (String num : numList) {
            num = num.trim();
            if (!TextUtils.isEmpty(num)) {
                try {
                    nums.add(Integer.parseInt(num));
                } catch (NumberFormatException e) {
                }
            }
        }
        return nums;
    }
}
