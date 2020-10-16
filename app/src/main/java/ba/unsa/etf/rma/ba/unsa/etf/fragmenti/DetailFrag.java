package ba.unsa.etf.rma.ba.unsa.etf.fragmenti;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.ArrayList;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.DodajKvizAkt;
import ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.IgrajKvizAkt;
import ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.KvizoviAkt;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Kategorija;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Kviz;
import ba.unsa.etf.rma.ba.unsa.etf.adapteri.KvizAdapter;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Pitanje;

public class DetailFrag  extends Fragment {
    private Kategorija kategorija;
    private String imeKviza;
    private ArrayList<Kategorija> listaKategorija = new ArrayList<>();
    private ArrayList<Kviz> listaKvizova = new ArrayList<>();
    private GridView kvizGridView;
    private KvizAdapter kvizAdapter;
    ArrayList<Pitanje> listaPitanja = new ArrayList<>();
    private Kategorija kategorijaSvi = new Kategorija("Svi", "99");
    private Kviz dodajKviz = new Kviz("Dodaj kviz", listaPitanja, kategorijaSvi);

    private ArrayList<Kviz> filterKvizove(Kategorija posaljiUFragment) {
        ArrayList<Kviz> result = new ArrayList<>();
        if (posaljiUFragment.getNaziv().equals("Svi"))
            result = KvizoviAkt.kvizovi;
        else {
            for (Kviz kv : KvizoviAkt.kvizovi)
                if (kv.getKategorija().getNaziv().equals(KvizoviAkt.kategorijuPosaljiUFragment.getNaziv()))
                    result.add(kv);
        }
        return result;
    }


    public DetailFrag() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View iv = inflater.inflate(R.layout.detail_place_frag, container, false);
        kvizGridView = iv.findViewById(R.id.gridKvizovi);
        kvizAdapter = new KvizAdapter(getContext(), listaKvizova);
        kvizGridView.setAdapter(kvizAdapter);
        return iv;
    }
        
    @Override
    public void onActivityCreated (@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey("kvizovi_iste_kategorije_bundle")) {
            listaKvizova = (ArrayList<Kviz>) getArguments().getSerializable("kvizovi_iste_kategorije_bundle");
            listaKategorija = (ArrayList<Kategorija>) getArguments().getSerializable("svekategorije_bundle");

            listaKvizova = filterKvizove(KvizoviAkt.kategorijuPosaljiUFragment);
            kvizAdapter = new KvizAdapter(getContext(), listaKvizova);
            kvizGridView.setAdapter(kvizAdapter);
            kvizGridView.deferNotifyDataSetChanged();

            boolean postojiDodajKviz = false;
            for(Kviz x : KvizoviAkt.kvizovi)
                if(x.getNaziv().equals("Dodaj kviz")){
                    postojiDodajKviz = true;
                    break;
                }
            if(!postojiDodajKviz)
                KvizoviAkt.kvizovi.add(dodajKviz);
            kvizAdapter.notifyDataSetChanged();

            kvizGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    if (listaKvizova != null && position < listaKvizova.size() && position > -1) {
                        Intent myIntent = new Intent (getContext(), DodajKvizAkt.class);
                        imeKviza = listaKvizova.get(position).getNaziv();
                        myIntent.putExtra("kviz", listaKvizova.get(position));

                        ArrayList<Kviz> kvizoviSlanje = new ArrayList<>();
                        kvizoviSlanje = KvizoviAkt.kvizovi;
                        Kategorija kat = KvizoviAkt.kategorijuPosaljiUFragment;

                        myIntent.putExtra("svekategorije", listaKategorija);
                        myIntent.putExtra("kvizovi_iste_kategorije", kvizoviSlanje);
                        startActivityForResult(myIntent,1);
                    }
                    return true;
                }
            });

            kvizGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (!listaKvizova.get(position).getNaziv().equals("Dodaj kviz")) {
                        Intent myIntent = new Intent(getContext(), IgrajKvizAkt.class);
                        if (position > -1 && position <= listaKvizova.size())
                            myIntent.putExtra("kvizkojiseigra", listaKvizova.get(position));
                        startActivityForResult(myIntent, 1);
                    }
                }
            });
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        listaKategorija = KvizoviAkt.kategorije;
        int brojac = 0;
        for (Kategorija k : listaKategorija) {
            if (k.getNaziv().equals("Dodaj Kategoriju"))
                break;
            brojac++;
        }
        listaKategorija.remove(brojac);
        listaKvizova.remove(dodajKviz);
        KvizoviAkt.kategorijePosalji = listaKategorija;
        Kategorija x = (Kategorija) data.getSerializableExtra("kojajekategorija");
        KvizoviAkt.kategorijuPosaljiUFragment = x;
        Kviz y = (Kviz) data.getSerializableExtra("kviz");
        int i = 0;
        for (Kviz k : KvizoviAkt.kvizovi) {
            if (k.getNaziv().equals(imeKviza)) {
                KvizoviAkt.kvizovi.remove(i);
                KvizoviAkt.kvizovi.add(y);
                break;
            }
            i++;
        }
    }
}

