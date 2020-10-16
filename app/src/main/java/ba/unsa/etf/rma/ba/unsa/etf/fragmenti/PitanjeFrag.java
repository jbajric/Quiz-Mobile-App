package ba.unsa.etf.rma.ba.unsa.etf.fragmenti;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;


import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.IgrajKvizAkt;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Kviz;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Pitanje;

public class PitanjeFrag extends Fragment {
    private Kviz k = null;
    private TextView textPitanja;
    private ListView listaOdgovora;
    private ArrayList<Pitanje> listaPitanja = new ArrayList<>();
    private ArrayList<String> odgovori = new ArrayList<>();
    private ArrayAdapter<String> adapter; //pošto je odgovor tipa String, može obični arrayadapter
    private OnItemClick oic;
    private Pitanje trenutnoPitanje;

    public PitanjeFrag() {}

    public interface OnItemClick { void onItemClicked(int pos);}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View vi = inflater.inflate(R.layout.pitanje_place_frag, container, false);
        textPitanja = vi.findViewById(R.id.tekstPitanja);
        listaOdgovora = vi.findViewById(R.id.odgovoriPitanja);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, odgovori);
        listaOdgovora.setAdapter(adapter);
        return vi;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey("trenutnikviz")){
            k = (Kviz) getArguments().getSerializable("trenutnikviz");
            if (IgrajKvizAkt.brojacPitanja < k.getPitanja().size()) {
                listaPitanja = k.getPitanja();
                trenutnoPitanje = listaPitanja.get(IgrajKvizAkt.brojacPitanja);
                textPitanja.setText(trenutnoPitanje.getNaziv());
                odgovori = trenutnoPitanje.getOdgovori();

                adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, odgovori);
                listaOdgovora.setAdapter(adapter);
                listaOdgovora.deferNotifyDataSetChanged();
                try {
                    oic = (OnItemClick) getActivity();
                } catch (ClassCastException e) {
                    throw new ClassCastException(getActivity().toString() + "Treba implementirati OnItemClick");
                }
                listaOdgovora.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, odgovori) {
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View v = super.getView(position, convertView, parent);
                                if (trenutnoPitanje.getTacan().equals(odgovori.get(position)))
                                    v.setBackgroundColor(Color.parseColor("#7FFF00"));
                                else
                                    v.setBackgroundColor(Color.parseColor("#FF3030"));
                                return v;
                            }
                        };
                        int indexKliknutog = 0;
                        for (String s : odgovori) {
                            if (s.equalsIgnoreCase(odgovori.get(position)))
                                break;
                            indexKliknutog++;
                        }
                        if (odgovori.get(indexKliknutog).equalsIgnoreCase(trenutnoPitanje.getTacan()))
                            IgrajKvizAkt.brojTacnihOdgovora++;

                        listaOdgovora.setAdapter(adapter);
                        oic.onItemClicked(position);
                    }
                });
            }
            else {
                textPitanja.setText("Kviz je završen!");
                odgovori = new ArrayList<>();
                adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, odgovori);
                listaOdgovora.setAdapter(adapter);
                listaOdgovora.deferNotifyDataSetChanged();
            }
        }
        else { }
    }

}
