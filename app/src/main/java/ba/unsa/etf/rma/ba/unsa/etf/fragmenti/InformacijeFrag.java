package ba.unsa.etf.rma.ba.unsa.etf.fragmenti;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.IgrajKvizAkt;
import ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.KvizoviAkt;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Kviz;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Pitanje;

public class InformacijeFrag extends Fragment {
    private Kviz kviz = null;
    private TextView infNazivKviza;
    private TextView infBrojTacnih;
    private TextView infBrojPreostalih;
    private TextView infProcenatTacnih;
    private Button btnzavrsiKviz;

    public InformacijeFrag()  {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.informacije_place_frag, container, false);
        infNazivKviza = view.findViewById(R.id.infNazivKviza);
        infBrojTacnih = view.findViewById(R.id.infBrojTacnihPitanja);
        infBrojPreostalih = view.findViewById(R.id.infBrojPreostalihPitanja);
        infProcenatTacnih = view.findViewById(R.id.infProcenatTacni);
        btnzavrsiKviz = view.findViewById(R.id.btnKraj);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(getArguments()!=null && getArguments().containsKey("trenutnikviz")) {
            kviz = (Kviz) getArguments().getSerializable("trenutnikviz");
            if (kviz != null) {
                infNazivKviza.setText(kviz.getNaziv());
                String str = String.valueOf(IgrajKvizAkt.brojTacnihOdgovora);
                infBrojTacnih.setText(str);
                int brPreostalih = kviz.getPitanja().size() - IgrajKvizAkt.brojacPitanja;
                String brPreost = String.valueOf(brPreostalih);
                infBrojPreostalih.setText(brPreost);
                if (IgrajKvizAkt.brojacPitanja == 0)
                    IgrajKvizAkt.procenatTacnih = 0;
                else
                    IgrajKvizAkt.procenatTacnih = ((double) IgrajKvizAkt.brojTacnihOdgovora / IgrajKvizAkt.brojacPitanja) * 100;
                String procenat = String.valueOf(IgrajKvizAkt.procenatTacnih) + "%";
                infProcenatTacnih.setText(procenat);
            }
        }
        btnzavrsiKviz.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent (getActivity(), KvizoviAkt.class);
                IgrajKvizAkt.brojPreostalihPitanja = 0;
                IgrajKvizAkt.procenatTacnih = 0;
                IgrajKvizAkt.brojacPitanja = 0;
                IgrajKvizAkt.brojTacnihOdgovora = 0;
                getActivity().finish();
            }
        });

    }
}
