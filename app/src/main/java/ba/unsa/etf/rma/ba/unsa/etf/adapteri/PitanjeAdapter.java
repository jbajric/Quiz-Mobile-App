package ba.unsa.etf.rma.ba.unsa.etf.adapteri;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Pitanje;


public class PitanjeAdapter extends BaseAdapter {

    private LayoutInflater mLayoutInflater;
    private ArrayList<Pitanje> listaPitanja;
    private Context context;

    public PitanjeAdapter(Context context, ArrayList data){
        listaPitanja = data;
        mLayoutInflater = LayoutInflater.from(context);
        this.context = context;
    }

    @Override
    public int getCount() {
        return listaPitanja.size();
    }

    @Override
    public Pitanje getItem(int position) {
        return listaPitanja.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder{ TextView nazivPitanja; }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        View updateView;
        ViewHolder viewHolder;
        if (view == null) {
            updateView = mLayoutInflater.inflate(R.layout.element_liste_dodajkviz_pitanje, null);
            viewHolder = new ViewHolder();
            viewHolder.nazivPitanja = updateView.findViewById(R.id.nazivPitanja);
            updateView.setTag(viewHolder);
        } else {
            updateView = view;
            viewHolder = (ViewHolder) updateView.getTag();
        }

        final Pitanje item = getItem(position);
        viewHolder.nazivPitanja.setText(item.getNaziv());
        return updateView;
    }
}

