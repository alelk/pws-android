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

        textView = (TextView) findViewById(R.id.txt_psalm_activity);

        PwsPsalmParcelable psalmParcelable = getIntent().getParcelableExtra("psalm");

        String text = "Name: " + psalmParcelable.getName();
        text += "\nVersion: " + psalmParcelable.getVersion();
        text += "\nAuthor: " + psalmParcelable.getAuthor();
        text += "\nTranslator: " + psalmParcelable.getTranslator();
        text += "\nComposer: " + psalmParcelable.getComposer();
        text += "\nYear: " + psalmParcelable.getYear();
        text += "\nTonalities: " + psalmParcelable.getTonalities();
        text += "\nNumbers: " + psalmParcelable.getNumbers();
        text += "\nAnnotation: " + psalmParcelable.getAnnotation();
        text += "\n___________________________\n";
        SortedMap<Integer, PsalmPart> psalmParts = psalmParcelable.getPsalmParts();
        for (int k : psalmParts.keySet()) {
            text += "#" + k + "(" + psalmParts.get(k).getNumbers() + ")";
            text += " Type: " + psalmParcelable.getPsalmParts().get(k).getPsalmType() + "\n";
            text += psalmParts.get(k).getText() + "\n";
        }

        textView.setText(text);

    }
}
