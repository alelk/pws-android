package com.alelk.pws.pwapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.alelk.pws.database.data.BookEdition;
import com.alelk.pws.database.data.Psalm;

import java.util.List;

/**
 * Created by Alex Elkin on 17.04.2015.
 */
public class PsalmListAdapter extends ArrayAdapter<Psalm> {

    List<Psalm> objects;
    public PsalmListAdapter(Context context, int resource, List<Psalm> objects) {
        super(context, resource, objects);
        this.objects = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // todo rewrite this method
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.layout_psalms_list, null);
        }

        Psalm psalm = objects.get(position);
        if (psalm != null) {
            TextView psalmNumber = (TextView) v.findViewById(R.id.txt_psalm_number);
            TextView psalmName = (TextView) v.findViewById(R.id.txt_psalm_name);
            TextView bookEdition = (TextView) v.findViewById(R.id.txt_book_edition);
            TextView anotherBookEditions = (TextView) v.findViewById(R.id.txt_another_book_editions);
            psalmNumber.setText(Integer.toString(psalm.getNumber(BookEdition.PV3055)));
            psalmName.setText(psalm.getName());
            bookEdition.setText(BookEdition.PV3055.getSignature());
            String anotherEditions = "";
            for (BookEdition be : psalm.getBookEditions()) {
                if (!be.equals(BookEdition.PV3055)) {
                    if (anotherEditions.length() > 0) {
                        anotherEditions += "; ";
                    }
                    anotherEditions += psalm.getNumber(be) + ": " + be.getSignature();
                }
            }
            anotherBookEditions.setText(anotherEditions);
        }
        return v;
    }


}
