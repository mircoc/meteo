package com.buongiorno.mcipriani.meteo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import meteo.mcipriani.com.buongiorno.meteo.R;

public class Settings extends Activity {

    private CityListAdapter m_list_adapter;
    protected String selectedCity = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final String[] cities = getResources().getStringArray(R.array.cities);
        final List<String> citiesList = new ArrayList<String>(Arrays.asList(cities));

        m_list_adapter = new CityListAdapter(citiesList);
        ListView list1 = (ListView)findViewById(R.id.listViewSettings);
        list1.setAdapter(m_list_adapter);
        list1.setChoiceMode(ListView.CHOICE_MODE_SINGLE);



        Intent callingIntent = getIntent();
        boolean found = false;
        if (callingIntent != null) {
            selectedCity = callingIntent.getStringExtra(Meteo.VALUE_SEL_CITY);

            for (int i=0; i<citiesList.size(); i++) {

                String itemx = citiesList.get(i);
                if (itemx.equalsIgnoreCase(selectedCity)) {
                    list1.setItemChecked(i, true);
                    found = true;
                }
                else {
                    list1.setItemChecked(i, false);
                }
            }
        }
        if (!found) {
            // select first
            list1.setItemChecked(0, true);
        }

        list1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setSelected(true);
                selectedCity = citiesList.get(i);
                m_list_adapter.notifyDataSetChanged();
            }
        });

        Button saveSettings = (Button)findViewById(R.id.buttonSaveSettings);
        saveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedCity == "") {
                    Toast.makeText(Settings.this, "Select a city first!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent result = new Intent();
                result.putExtra(Meteo.VALUE_SEL_CITY, selectedCity);
                setResult(RESULT_OK, result);
                finish();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class CityListAdapter extends ArrayAdapter<String> {
        private List<String> data = null;

        CityListAdapter(List<String> mydata) {
            super(Settings.this, R.layout.settings_city_list_row, mydata.size() );
            this.data = mydata;
        }

        public View getView(int i, View convertView, ViewGroup parent) {
            View row = getLayoutInflater().inflate(R.layout.settings_city_list_row, parent, false);
            TextView t = (TextView) row.findViewById(R.id.textView);
            t.setText("" + data.get(i));
            if (data.get(i).equals(selectedCity)) {
                t.setBackgroundColor(R.color.pressed_color);
            }
            return(row);
        }

        @Override public int getCount() { return this.data.size(); }
    }
}
