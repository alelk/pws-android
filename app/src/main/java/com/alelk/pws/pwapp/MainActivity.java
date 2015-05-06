package com.alelk.pws.pwapp;

import android.content.Intent;
import android.content.res.AssetManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.Psalm;
import com.alelk.pws.database.data.PsalmPart;
import com.alelk.pws.database.exception.PwsDatabaseIncorrectValueException;
import com.alelk.pws.database.exception.PwsDatabaseSourceIdExistsException;
import com.alelk.pws.database.source.PwsDataSource;
import com.alelk.pws.database.source.PwsDataSourceImpl;
import com.alelk.pws.pwapp.data.PwsPsalmParcelable;
import com.alelk.pws.xmlengine.PwsXmlParser;
import com.alelk.pws.xmlengine.exception.PwsXmlParserIncorrectSourceFormatException;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private TextView textView;
    private ListView listView;
    private ArrayAdapter<Psalm> psalmListAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.listView);


        AssetManager am = this.getAssets();

        PwsXmlParser parser = new PwsXmlParser(am);

        try {
            Book book = parser.parseBook("pwsbooks/testBook.pws");
            List<Psalm> psalms = new ArrayList<>();
            for (int num : book.getPsalms().keySet()) {
                psalms.add(book.getPsalm(num));
            }
            psalmListAdapter = new PsalmListAdapter(this, R.layout.layout_psalms_list, psalms);
            listView.setAdapter(psalmListAdapter);
            listView.setOnItemClickListener(psalmListClickHandler);

            PwsDataSource pwsDataSource = new PwsDataSourceImpl(this, "pws.db", 2);
            pwsDataSource.open();

            pwsDataSource.addBook(book);
            for (Psalm psalm : book.getPsalms().values()) {
                try {
                    pwsDataSource.addPsalm(psalm);
                } catch (PwsDatabaseSourceIdExistsException e) {
                } catch (PwsDatabaseIncorrectValueException e) {
                }
            }


            pwsDataSource.close();
        } catch (PwsXmlParserIncorrectSourceFormatException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private AdapterView.OnItemClickListener psalmListClickHandler = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Psalm psalm = (Psalm) parent.getItemAtPosition(position);

            String text = psalm.toString() + "\n";
            for (PsalmPart part : psalm.getPsalmParts().values()) {
                text += part + "\n";
            }

            Intent intent = new Intent(getApplicationContext(), PsalmActivity.class);
            intent.putExtra("psalm", new PwsPsalmParcelable(psalm));
            intent.putExtra("text", text);
            startActivity(intent);
        }
    };
}
