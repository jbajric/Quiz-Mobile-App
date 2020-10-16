package ba.unsa.etf.rma.ba.unsa.etf.fragmenti;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import ba.unsa.etf.rma.R;

public class RangLista extends Fragment {
    private ListView Ranglista;
    public static ArrayList<String> lista = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View vi = inflater.inflate(R.layout.ranglista_frag, container, false);
        Ranglista = vi.findViewById(R.id.lvRangLista);
        return vi;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (lista != null) {
            adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, lista);
            Ranglista.setAdapter(adapter);
        }
    }

}
