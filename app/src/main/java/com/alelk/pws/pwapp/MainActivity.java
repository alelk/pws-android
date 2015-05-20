package com.alelk.pws.pwapp;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.MatrixCursor;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.alelk.pws.database.data.Book;
import com.alelk.pws.database.data.BookEdition;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
            List<String> bookNames = Arrays.asList("pwsbooks/pv3055.pwsbk",
                    "pwsbooks/chymns.pwsbk",
                    "pwsbooks/cpsalms.pwsbk",
                    "pwsbooks/gusli.pwsbk",
                    "pwsbooks/kimval.pwsbk",
                    "pwsbooks/sdp.pwsbk",
                    "pwsbooks/tympan.pwsbk",
                    "pwsbooks/slavit\'_bez_zapinki.pwsbk",
                    "pwsbooks/fcpsalms.pwsbk", "pwsbooks/PVNL.pwsbk");

            Map<BookEdition, Book> books = new HashMap();

            for (String bookName : bookNames) {
                Book book = parser.parseBook(bookName);
                books.put(book.getEdition(), book);
            }

            PwsDataSource pwsDataSource = new PwsDataSourceImpl(this, "pws.db", 5);
            pwsDataSource.open();
            for (Book book : books.values()) {
                pwsDataSource.addBook(book);
            }

            Book book = books.get(BookEdition.PV3055);
            for (Psalm psalm : book.getPsalms().values()) {
                try {
                    pwsDataSource.addPsalm(psalm);
                } catch (PwsDatabaseSourceIdExistsException e) {
                } catch (PwsDatabaseIncorrectValueException e) {
                }
            }



            List<Psalm> psalms = new ArrayList<>();
            try {
                psalms.addAll(pwsDataSource.getPsalms(BookEdition.PV3055).values());
            } catch (PwsDatabaseIncorrectValueException e) {
                e.printStackTrace();
            }
            pwsDataSource.close();

            Collections.sort(psalms, Psalm.getNumberComparator(BookEdition.PV3055));

            psalmListAdapter = new PsalmListAdapter(this, R.layout.layout_psalms_list, psalms);
            listView.setAdapter(psalmListAdapter);
            listView.setOnItemClickListener(psalmListClickHandler);
        } catch (PwsXmlParserIncorrectSourceFormatException e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                List<Psalm> psalms = loadData("");
                psalmListAdapter = new PsalmListAdapter(getApplicationContext(), R.layout.layout_psalms_list, psalms.subList(2,4));
                listView.setAdapter(psalmListAdapter);
                listView.setOnItemClickListener(psalmListClickHandler);

                return true;
            }
        });
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

    private List<Psalm> loadData(String query) {
        PwsDataSource pwsDataSource = new PwsDataSourceImpl(this, "pws.db", 5);
        pwsDataSource.open();
        List<Psalm> psalms = new ArrayList<>();
        try {
            psalms.addAll(pwsDataSource.getPsalms(BookEdition.PV3055).values());
        } catch (PwsDatabaseIncorrectValueException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        pwsDataSource.close();

        Collections.sort(psalms, Psalm.getNumberComparator(BookEdition.PV3055));
        return psalms;
    }

    private AdapterView.OnItemClickListener psalmListClickHandler = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Psalm psalm = (Psalm) parent.getItemAtPosition(position);

            String text = psalm.toString() + "\n";
            if (psalm.getPsalmParts() != null) {
                for (PsalmPart part : psalm.getPsalmParts().values()) {
                    text += part + "\n";
                }
            }

            Intent intent = new Intent(getApplicationContext(), PsalmActivity.class);
            intent.putExtra("psalm", new PwsPsalmParcelable(psalm));
            intent.putExtra("text", text);
            startActivity(intent);
        }
    };
}
