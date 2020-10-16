package ba.unsa.etf.rma.ba.unsa.etf.adapteri;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import ba.unsa.etf.rma.ba.unsa.etf.klase.Kategorija;


public class KategorijaAdapter extends ArrayAdapter<Kategorija> {

    private Context context;
    private ArrayList<Kategorija> listaKategorija = new ArrayList<>();

    public KategorijaAdapter (Context context, int id, ArrayList<Kategorija> values) {
        super(context, id, values);
        this.context = context;
        this.listaKategorija = values;
    }

    @Override
    public int getCount(){ return listaKategorija.size(); }

    @Override
    public Kategorija getItem(int position){
        return listaKategorija.get(position);
    }

    @Override
    public long getItemId(int position){
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView view = new TextView(context);
        view.setTextColor(Color.BLACK);
        view.setGravity(Gravity.CENTER);
        if (listaKategorija.get(position) != null)
            view.setText(listaKategorija.get(position).getNaziv());
        else
            view.setText(" ");
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView view = new TextView(context);
        view.setTextColor(Color.BLACK);
        view.setText(listaKategorija.get(position).getNaziv());
        return view;
    }

}

