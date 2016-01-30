package com.alelk.pws.pwapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.alelk.pws.database.data.PsalmPart;
import com.alelk.pws.pwapp.data.PwsPsalmParcelable;

import java.util.SortedMap;

/**
 * Created by Alex on 18.04.2015.
 */
public class PsalmActivity extends Activity {
    private TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_psalm);

        //textView = (TextView) findViewById(R.id.txt_psalm_details);

        PwsPsalmParcelable psalmParcelable = getIntent().getParcelableExtra("psalm");
    }
}
