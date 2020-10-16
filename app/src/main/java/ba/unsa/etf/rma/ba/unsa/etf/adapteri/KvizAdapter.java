package ba.unsa.etf.rma.ba.unsa.etf.adapteri;

import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.maltaisn.icondialog.IconHelper;

import java.util.ArrayList;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Kviz;

public class KvizAdapter extends BaseAdapter implements Filterable {

    private LayoutInflater mLayoutInflater;
    private ArrayList<Kviz> kvizList;
    private ArrayList<Kviz> kvizFilterList;
    private KvizFilter kvizFilter;
    private Context context;

    public KvizAdapter(Context context, ArrayList data){
        kvizList = data;
        kvizFilterList =data;
        mLayoutInflater = LayoutInflater.from(context);
        this.context = context;
    }

    @Override
    public int getCount() {
        return kvizList.size();
    }

    @Override
    public Kviz getItem(int position) {
        return kvizList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    public Kviz getItemAtPosition(int position) {
        return kvizFilterList.get(position);
    }

    static class ViewHolder{
        TextView nazivKviza;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final Kviz item = getItem(position);
        View updateView;
        ViewHolder viewHolder;
        if (view == null)
            view = LayoutInflater.from(mLayoutInflater.getContext()).inflate(R.layout.element_liste,parent,false);
        TextView text = view.findViewById(R.id.nazivKvizaKolona);
        text.setText(item.getNaziv());
        final ImageView imageView = view.findViewById(R.id.ikona);
        final IconHelper iconHelper = IconHelper.getInstance(view.getContext());
        iconHelper.addLoadCallback(new IconHelper.LoadCallback() {
            @Override
            public void onDataLoaded() {
                imageView.setImageDrawable(iconHelper.getIcon(Integer.parseInt(item.getKategorija().getId())).getDrawable(mLayoutInflater.getContext()));
            }
        });
        return view;
    }

    @Override
    public Filter getFilter() {
        if (kvizFilter == null) {
            kvizFilter = new KvizFilter();
        }
        return kvizFilter;
    }


// InnerClass for enabling Search feature by implementing the methods

    private class KvizFilter extends Filter
    {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            String naziv = constraint.toString();
            FilterResults results = new FilterResults();

            if (!naziv.isEmpty()) {
                ArrayList<Kviz> filterList = new ArrayList<Kviz>();
                for (int i = 0; i < kvizFilterList.size(); i++) {

                    if ((kvizFilterList.get(i).getKategorija().getNaziv()).equals( naziv)) {

                        Kviz address = kvizFilterList.get(i);
                        filterList.add(address);
                    }
                }
                results.count = filterList.size();
                results.values = filterList;
            } else {
                results.count = kvizFilterList.size();
                results.values = kvizFilterList;
            }
            return results;
        }
        //Publishes the matches found, i.e., the selected cityids
        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {

            kvizList = (ArrayList<Kviz>)results.values;
            notifyDataSetChanged();
        }
    }
}
