package com.alelk.pws.pwapp;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.alelk.pws.database.data.PsalmPart;
import com.alelk.pws.pwapp.data.PwsPsalmParcelable;

import java.util.SortedMap;

/**
 * Created by Alex Elkin on 18.04.2015.
 */
public class PsalmActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_psalm);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_psalm);
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_psalm);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //PwsPsalmParcelable psalmParcelable = getIntent().getParcelableExtra("psalm");
        //setTitle(psalmParcelable.getName());
    }
}
