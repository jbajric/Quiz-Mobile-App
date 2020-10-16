package ba.unsa.etf.rma.ba.unsa.etf.fragmenti;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;


import java.util.ArrayList;
import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.ba.unsa.etf.adapteri.KategorijaAdapter;
import ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.KvizoviAkt;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Kategorija;

public class ListaFrag extends Fragment {
    private ArrayList<Kategorija> listaKategorija = new ArrayList<>();
    private ListView listViewKategorije;
    private OnItemClick oic;
    private KategorijaAdapter kategorijaAdapter;


    public ListaFrag() {}

    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState);}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_place_frag, container, false);
        listViewKategorije = (ListView) view.findViewById(R.id.listaKategorija);
        kategorijaAdapter = new KategorijaAdapter(getContext(), android.R.layout.simple_list_item_1, listaKategorija);
        listViewKategorije.setAdapter(kategorijaAdapter);
        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey("svekategorije_bundle")) {
            listaKategorija = (ArrayList<Kategorija>) getArguments().getSerializable("svekategorije_bundle");
            kategorijaAdapter = new KategorijaAdapter(getContext(), android.R.layout.simple_list_item_1, listaKategorija);
            listViewKategorije.setAdapter(kategorijaAdapter);
            listViewKategorije.deferNotifyDataSetChanged();

            listViewKategorije.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Kategorija x = listaKategorija.get(position);
                    KvizoviAkt.kategorijuPosaljiUFragment = x;
                    try {
                        oic = (ListaFrag.OnItemClick) getActivity();
                    }
                    catch (ClassCastException e) {}
                    oic.onItemClicked(position);
                }
            });
        }
    }


    public interface OnItemClick{
        void onItemClicked(int pos);
    }

}
