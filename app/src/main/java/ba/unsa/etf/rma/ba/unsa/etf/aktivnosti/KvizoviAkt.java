package ba.unsa.etf.rma.ba.unsa.etf.aktivnosti;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.ba.unsa.etf.fragmenti.DetailFrag;
import ba.unsa.etf.rma.ba.unsa.etf.fragmenti.ListaFrag;
import ba.unsa.etf.rma.ba.unsa.etf.AsyncResponse;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Kategorija;
import ba.unsa.etf.rma.ba.unsa.etf.adapteri.KategorijaAdapter;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Kviz;
import ba.unsa.etf.rma.ba.unsa.etf.adapteri.KvizAdapter;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Pitanje;

import static com.google.api.client.googleapis.auth.oauth2.GoogleCredential.fromStream;

public class KvizoviAkt extends AppCompatActivity  implements AdapterView.OnItemSelectedListener, ListaFrag.OnItemClick{


    public static Kategorija kategorijuPosaljiUFragment;
    public static ArrayList<Kviz> kvizovi = new ArrayList<>();
    public static ArrayList<Kategorija> kategorije = new ArrayList<>();
    public static ArrayList<Kategorija> kategorijePosalji = new ArrayList<>();
    public static ArrayList<Kviz> kvizovePosalji = new ArrayList<>();
    public static int dodavanje = 0;
    public static String TOKEN ="";

    private Spinner spinner;
    private ListView listaKvizova;
    private KvizAdapter kvizAdapter;
    private KategorijaAdapter kategorijaAdapter;
    final Comparator<Kategorija> comp = new Comparator<Kategorija>() {
        @Override
        public int compare(Kategorija c1, Kategorija c2) { return c1.getNaziv().compareTo(c2.getNaziv()); }
    };
    private String staroImeKviza;
    boolean postoji = false;

    private void napuniPodacima() {
        Kategorija svi = new Kategorija("Svi", "914");
        kategorije.add(svi);
        ArrayList<Pitanje> pitanja = new ArrayList<>();
        ArrayList<String> odgovori = new ArrayList<>();
        odgovori.add("tacan");
        odgovori.add("netacan");
        pitanja.add(new Pitanje("Pitanje 1", "tekst 1", odgovori, "tacan"));
        pitanja.add(new Pitanje("Pitanje 2","tekst 2", odgovori, "tacan" ));
        Kviz k = new Kviz("Kviz 1", pitanja, kategorije.get(0));
        kvizovi.add(k);
    }
    private ArrayList<Kviz> istaKategorija(Kategorija kategorija) {
        ArrayList<Kviz> result = new ArrayList<>();
        if (kategorija.getNaziv().equals("Svi"))
            result = kvizovi;
        else {
            Kategorija x = (Kategorija) spinner.getSelectedItem();
            for (Kviz y : kvizovi)
                if (y.getKategorija().getNaziv().equals(x.getNaziv()))
                    result.add(y);
        }
        return result;
    }

    //spirala 3
    public static Map<Kviz, String> spisakKvizovaFirestore = new HashMap<>();
    public static Map<Kategorija, String> spisakKategorijaFirestore = new HashMap<>();
    public static Map<Pitanje, String> spisakPitanjaFirestore = new HashMap<>();
    public static ArrayList<Pitanje> tempPitanja = new ArrayList<>();
    public static ArrayList<Pitanje> svaPitanjaBaze = new ArrayList<>();
    private static boolean radiUPozadini(Context ctx) {
        ActivityManager.RunningAppProcessInfo myProcess = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(myProcess);
        Boolean isInBackground = myProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
        if(isInBackground)
            return true;
        return false;
    }
    private Kviz posaljiKviz = null;

    //spirala 4
    private ArrayList<Pair<String, String>> dogadjajiDatumVrijeme = new ArrayList<>();
    private int MY_CAL_REQ = 0;
    boolean moguceIgrati = true;
    public boolean imalVajerlesailiTriGea() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }


    //      ******************ASYNC KLASE******************

    public class KreirajDokumentTask extends AsyncTask<String,Void,Void> {
        @Override
        protected Void doInBackground(String... strings) {
            GoogleCredential credentials;
            try {
                InputStream is = getResources().openRawResource(R.raw.secret);
                credentials = GoogleCredential.fromStream(is).createScoped(Lists.<String>newArrayList("https://www.googleapis.com/auth/datastore"));
                credentials.refreshToken();
                TOKEN = credentials.getAccessToken();
                Log.d("TOKEN", TOKEN);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public class PreuzmiKategorijeizBaze extends AsyncTask<String, Void, String> {
        AsyncResponse delegate = null;
        public PreuzmiKategorijeizBaze(AsyncResponse delegate) { this.delegate = delegate; }
        @Override
        protected String doInBackground(String... params) {
            InputStream is = getResources().openRawResource(R.raw.secret);
            try {
                GoogleCredential credentials = fromStream(is).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/datastore"));
                credentials.refreshToken();
                TOKEN = credentials.getAccessToken();
                String urll = "https://firestore.googleapis.com/v1/projects/rma19bajricjasmin/databases/(default)/documents/Kategorije?access_token=";
                URL url = new URL(urll + URLEncoder.encode(TOKEN, "utf-8"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream ins = connection.getInputStream();
                String rezultatListaKategorija = convertStreamToString(ins);
                //nije null
                if (!rezultatListaKategorija.equals("{}")) {
                    JSONObject jsKategorije = new JSONObject(rezultatListaKategorija);
                    JSONArray documentsKategorije = jsKategorije.getJSONArray("documents");
                    for (int i = 0; i < documentsKategorije.length(); i++) {
                        JSONObject obj = documentsKategorije.getJSONObject(i);
                        String nameJSON = obj.getString("name"); //id
                        JSONObject fields = obj.getJSONObject("fields");
                        //naziv kategorije
                        JSONObject stringValue = fields.getJSONObject("naziv");
                        String nazivKategorije = stringValue.getString("stringValue");
                        //idKategorije
                        JSONObject integerValue = fields.getJSONObject("idIkonice");
                        String idIkonice = String.valueOf(integerValue.getString("stringValue"));
                        Kategorija k = new Kategorija(nazivKategorije,idIkonice);
                        spisakKategorijaFirestore.put(k, nameJSON);
                    }
                }
            }
            catch (IOException e) { }
            catch (JSONException e) { e.printStackTrace(); }
            return null;
        }
        @Override
        protected void onPostExecute(String s) { delegate.processFinish(s); }
    }

    public class PreuzmiPitanjaizBaze extends AsyncTask<String, Void, String> {
        AsyncResponse delegate = null;
        public PreuzmiPitanjaizBaze(AsyncResponse delegate) { this.delegate = delegate; }
        @Override
        protected String doInBackground(String... params) {
            InputStream is = getResources().openRawResource(R.raw.secret);
            try {
                GoogleCredential credentials = GoogleCredential.fromStream(is).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/datastore"));
                credentials.refreshToken();
                TOKEN = credentials.getAccessToken();
                String urll = "https://firestore.googleapis.com/v1/projects/rma19bajricjasmin/databases/(default)/documents/Pitanja?access_token=";
                URL url = new URL(urll + URLEncoder.encode(TOKEN, "utf-8"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream ins = connection.getInputStream();
                String rezultatListaPitanja = convertStreamToString(ins);
                // nije null

                if (!rezultatListaPitanja.equals("{}")) {
                    JSONObject jsPitanja = new JSONObject(rezultatListaPitanja);
                    JSONArray documentsPitanja = jsPitanja.getJSONArray("documents");
                    for (int i = 0; i < documentsPitanja.length(); i++) {
                        JSONObject obj = documentsPitanja.getJSONObject(i);
                        String nameJSON = obj.getString("name"); //id
                        JSONObject fields = obj.getJSONObject("fields");
                        JSONObject stringValue = fields.getJSONObject("naziv");
                        String nazivPitanja = stringValue.getString("stringValue");
                        JSONObject integerValue = fields.getJSONObject("indexTacnog");
                        int indexTacnogPitanja = integerValue.getInt("integerValue");
                        JSONObject odgovori = fields.getJSONObject("odgovori");
                        JSONObject arrayValue = odgovori.getJSONObject("arrayValue");
                        JSONArray values = arrayValue.getJSONArray("values");
                        ArrayList<String> odgovoriPitanja = new ArrayList<>();
                        for (int j = 0; j < values.length(); j++) {
                            JSONObject x = values.getJSONObject(j);
                            String odgovorPitanje = x.getString("stringValue");
                            odgovoriPitanja.add(odgovorPitanje);
                        }
                        String tacanOdgovorPitanje = "";
                        for (int j = 0; j < odgovoriPitanja.size(); j++) {
                            if (j == indexTacnogPitanja) {
                                tacanOdgovorPitanje = odgovoriPitanja.get(j);
                                break;
                            }
                        }
                        Pitanje p = new Pitanje(nazivPitanja, nazivPitanja, odgovoriPitanja, tacanOdgovorPitanje);
                        spisakPitanjaFirestore.put(p, nameJSON);
                        svaPitanjaBaze.add(p);
                    }
                }
            }
            catch (IOException e) { }
            catch (JSONException e) { e.printStackTrace(); }
            return null;
        }
        @Override
        protected void onPostExecute(String s) {
            Iterator it = spisakPitanjaFirestore.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry mapa = (Map.Entry) it.next();
                Pitanje x = (Pitanje) mapa.getKey();
                tempPitanja.add(x);
            }
            delegate.processFinish(s);
        }
    }

    public class PreuzmiKvizoveizBaze extends AsyncTask<String, Void, String> {
        AsyncResponse delegate = null;
        public PreuzmiKvizoveizBaze(AsyncResponse delegate) { this.delegate = delegate; }
        @Override
        protected String doInBackground(String... strings) {
            InputStream is = getResources().openRawResource(R.raw.secret);
            try {
                GoogleCredential credentials = GoogleCredential.fromStream(is).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/datastore"));
                credentials.refreshToken();
                TOKEN = credentials.getAccessToken();
                String urll = "https://firestore.googleapis.com/v1/projects/rma19bajricjasmin/databases/(default)/documents/Kvizovi?access_token=";
                URL url = new URL(urll + URLEncoder.encode(TOKEN, "utf-8"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream ins = connection.getInputStream();
                String rezultatListaKvizova = convertStreamToString(ins);
                //nije null
                if (!rezultatListaKvizova.equals("{}")) {
                    JSONObject jsKvizovi = new JSONObject(rezultatListaKvizova);
                    JSONArray documentsKvizovi = jsKvizovi.getJSONArray("documents");
                    for (int i = 0; i < documentsKvizovi.length(); i++) {
                        JSONObject obj = documentsKvizovi.getJSONObject(i);
                        String nameJSON = obj.getString("name"); //id
                        JSONObject fields = obj.getJSONObject("fields");
                        //naziv kviza
                        JSONObject stringValue = fields.getJSONObject("naziv");
                        String nazivKviza = stringValue.getString("stringValue");
                        //idKategorije kviza
                        JSONObject stringValue1 = fields.getJSONObject("idKategorije");
                        String idKategorije = stringValue1.getString("stringValue");
                        //pitanja kviza
                        ArrayList<Pitanje> listaPitanja = new ArrayList<>();
                        JSONObject pitanjaKviza = fields.getJSONObject("pitanja");
                        JSONObject arrayValue = pitanjaKviza.getJSONObject("arrayValue");
                        if (arrayValue != null) {
                            JSONArray values = arrayValue.getJSONArray("values");
                            for (int j = 0; j < values.length(); j++) {
                                JSONObject x = values.getJSONObject(j);
                                String nazivPitanja = x.getString("stringValue");
                                for (Pitanje p : tempPitanja) {
                                    if (p.getNaziv().equals(nazivPitanja)) {
                                        listaPitanja.add(p);
                                        break;
                                    }
                                }
                            }
                        }
                        else
                            listaPitanja = new ArrayList<>();
                        Kviz k = new Kviz(nazivKviza, listaPitanja, new Kategorija ("naziv " + idKategorije, idKategorije));
                        spisakKvizovaFirestore.put(k, nameJSON);
                    }
                }
            }
            catch (IOException e) { }
            catch (JSONException e) { e.printStackTrace(); }
            return null;
        }
        @Override
        protected void onPostExecute(String s) {
            delegate.processFinish(s);
        }
    }

    @NonNull
    public static String convertStreamToString(InputStream in) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null)
                sb.append(line + "\n");
        }
        catch (IOException e) {}
        finally {
            try {
                in.close();
            }
            catch (IOException e) {}
        }
        return sb.toString();
    }

    //      ******************ASYNC KLASE******************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_kvizovi_akt);
            new KreirajDokumentTask().execute("proba");
        if (imalVajerlesailiTriGea()) {
            if (!radiUPozadini(this)) {
                Kategorija svi = new Kategorija("Svi", "914");
                kategorije.add(svi);
                new PreuzmiPitanjaizBaze(new AsyncResponse() {
                    @Override
                    public void processFinish(String output) {
                    }
                }).execute("preuzmi pitanja");
                new PreuzmiKategorijeizBaze(new AsyncResponse() {
                    @Override
                    public void processFinish(String output) {
                        Iterator it = KvizoviAkt.spisakKategorijaFirestore.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry mapa = (Map.Entry) it.next();
                            Kategorija x = (Kategorija) mapa.getKey();
                            postoji = false;
                            for (Kategorija y : kategorije)
                                if (y.getNaziv().equals(x.getNaziv())) {
                                    postoji = true;
                                    AlertDialog alertDialog = new AlertDialog.Builder(KvizoviAkt.this).create();
                                    alertDialog.setTitle("Greška u ažuriranju lokalnog stanja");
                                    alertDialog.setMessage(x.getNaziv() + " već postoji lokalno u kategorijama!");
                                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            });
                                    alertDialog.show();
                                    break;
                                }
                            if (!postoji) {
                                kategorije.add(x);
                                kategorijaAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }).execute("preuzmi kategorije");
                new PreuzmiKvizoveizBaze(new AsyncResponse() {
                    @Override
                    public void processFinish(String output) {
                        Iterator it = KvizoviAkt.spisakKvizovaFirestore.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry mapa = (Map.Entry) it.next();
                            Kviz x = (Kviz) mapa.getKey();
                            postoji = false;
                            for (Kviz y : kvizovi)
                                if (y.getNaziv().equals(x.getNaziv())) {
                                    postoji = true;
                                    AlertDialog alertDialog = new AlertDialog.Builder(KvizoviAkt.this).create();
                                    alertDialog.setTitle("Greška u ažuriranju lokalnog stanja");
                                    alertDialog.setMessage(x.getNaziv() + " već postoji lokalno u kvizovima!");
                                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            });
                                    alertDialog.show();
                                    break;
                                }
                            if (!postoji) {
                                kvizovi.add(x);
                                kvizAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }).execute("preuzmi kvizove");
            }

            spinner = findViewById(R.id.spPostojeceKategorije);
            if (spinner == null) {
                kategorijuPosaljiUFragment = kategorije.get(0);
                kategorijePosalji = kategorije;

                FragmentManager fm = getSupportFragmentManager();
                Configuration configuration = getResources().getConfiguration();
                FragmentTransaction fragmentTransaction = fm.beginTransaction();

                DetailFrag detailFrag = new DetailFrag();
                ListaFrag listaFrag = new ListaFrag();

                fragmentTransaction.add(R.id.listPlace, listaFrag);
                fragmentTransaction.add(R.id.detailPlace, detailFrag);

                Bundle argumenti = new Bundle();
                ArrayList<Kviz> kvizovi_iste_kategorije = new ArrayList<>();
                kvizovi_iste_kategorije = istaKategorija(kategorijuPosaljiUFragment);
                argumenti.putSerializable("kvizovi_iste_kategorije_bundle", kvizovi_iste_kategorije);
                argumenti.putSerializable("svekategorije_bundle", kategorijePosalji);

                listaFrag.setArguments(argumenti);
                detailFrag.setArguments(argumenti);

                fragmentTransaction.commit();
            } else {
                kategorijaAdapter = new KategorijaAdapter(this, android.R.layout.simple_spinner_dropdown_item, kategorije);
                kategorijaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(kategorijaAdapter);
                spinner.setOnItemSelectedListener(this);

                listaKvizova = findViewById(R.id.lvKvizovi);
                kvizAdapter = new KvizAdapter(this, kvizovi);
                listaKvizova.setAdapter(kvizAdapter);

                //AŽURIRANJE KVIZA
                listaKvizova.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        if (imalVajerlesailiTriGea()) {
                            if (kvizovi != null && position >= 0 && position < kvizovi.size()) {
                                Intent myIntent = new Intent(KvizoviAkt.this, DodajKvizAkt.class);
                                staroImeKviza = kvizovi.get(position).getNaziv();
                                posaljiKviz = kvizovi.get(position);
                                DodajKvizAkt.mogucapitanja.clear();
                                myIntent.putExtra("kviz", posaljiKviz);
                                ArrayList<Kviz> kvizoviIsteKategorije = istaKategorija((Kategorija) spinner.getSelectedItem());
                                myIntent.putExtra("svekategorije", kategorije);
                                myIntent.putExtra("kategorijakviza", posaljiKviz.getKategorija());
                                myIntent.putExtra("kvizovi_iste_kategorije", kvizoviIsteKategorije);
                                startActivityForResult(myIntent, 1);
                            }
                        } else {
                            Toast toast = Toast.makeText(getApplicationContext(), "Niste konektovani na internet!", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        return true;
                    }
                });


                //IGRANJE KVIZA
                listaKvizova.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Kviz igrajovajkviz = kvizovi.get(position);
                        if (igrajovajkviz.getPitanja().size() == 0)
                            Toast.makeText(KvizoviAkt.this, "Kviz nema pitanja.", Toast.LENGTH_SHORT).show();
                        else {
                            //PROVJERA DA LI SE MOŽE IGRATI KVIZ
                            dobaviDogadjajeizKalendara(view);
                            provjeriIgranjeiZaigraj(igrajovajkviz);
                        }
                    }
                });

                View dodajKvizElement = getLayoutInflater().inflate(R.layout.listview_footer, null);
                listaKvizova.addFooterView(dodajKvizElement);
                //new FiltrirajKvizove().execute(queryDodajKviz);
                dodajKvizElement.setOnLongClickListener(new AdapterView.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (imalVajerlesailiTriGea()) {
                            DodajKvizAkt.mogucapitanja.clear();
                            Intent myIntent = new Intent(KvizoviAkt.this, DodajKvizAkt.class);
                            myIntent.putExtra("svekategorije", kategorije);
                            myIntent.putExtra("kvizovi_iste_kategorije", kvizovi);
                            myIntent.putExtra("kategorijakviza", (Kategorija) spinner.getSelectedItem());
                            Kviz kviz = null;
                            myIntent.putExtra("kviz", kviz);
                            startActivityForResult(myIntent, 1);
                        } else {
                            Toast toast = Toast.makeText(getApplicationContext(), "Niste konektovani na internet!", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        return true;
                    }
                });
            }
        }
        else {
            Toast toast = Toast.makeText(getApplicationContext(), "Niste konektovani na internet! Pokrenite opet aplikaciju.", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @SuppressLint("NewApi")
    private void provjeriIgranjeiZaigraj(Kviz igrajovajkviz) {
        int brojPitanjaKviza = igrajovajkviz.getPitanja().size();
        int minute = 0;
        int sekunde = 0;
        int sati = 0;
        if (brojPitanjaKviza % 2 == 1)
            minute = brojPitanjaKviza/2 + 1;
        else
            minute = brojPitanjaKviza/2;
        LocalTime time = LocalTime.now();
        sekunde = time.getSecond();
        while(sekunde > 59) {
            minute++;
            sekunde -= 60;
        }
        while (minute > 59 && minute % 60 != 0) {
            sati++;
            minute -= 60;
        }
        int satiAlarma = (time.getHour()+sati);
        int minuteAlarma = time.getMinute()+minute;
        LocalDate date = LocalDate.now();
        for(int i = 0; i < dogadjajiDatumVrijeme.size(); i++) {
            moguceIgrati = true;
            String datumDogadjaja = dogadjajiDatumVrijeme.get(i).first;
            String[] datum = datumDogadjaja.split("/");
            String vrijemeDogadjaja = dogadjajiDatumVrijeme.get(i).second;
            String[] vrijeme = vrijemeDogadjaja.split(":");
            if (date.getYear() > Integer.valueOf(datum[2]) || date.getYear() < Integer.valueOf(datum[2]) || !((date.getMonthValue() == Integer.valueOf(datum[1]) && date.getDayOfMonth() == Integer.valueOf(datum[0]))))
                continue;
            else {
                if (date.getMonthValue() == Integer.valueOf(datum[1]) && date.getDayOfMonth() == Integer.valueOf(datum[0])) {
                    System.out.println("ALARM " + satiAlarma + ":" + minuteAlarma + "         ");
                    if (satiAlarma >= Integer.valueOf(vrijeme[0]) && minuteAlarma >= Integer.valueOf(vrijeme[1]) && time.getHour() <= Integer.valueOf(vrijeme[0]) && time.getMinute() <= Integer.valueOf(vrijeme[1])) {
                        moguceIgrati = false;
                        AlertDialog alertDialog = new AlertDialog.Builder(KvizoviAkt.this).create();
                        alertDialog.setTitle("PODSJETNIK");
                        int mA = minuteAlarma + satiAlarma*60;
                        int minuteDogadjaja = Integer.valueOf(vrijeme[0]) * 60 + Integer.valueOf(vrijeme[1]);
                        Log.d("RAZLIKA", String.valueOf(mA) + " - "  + String.valueOf(minuteDogadjaja));
                        int razlika = minuteDogadjaja - (time.getMinute()+time.getHour()*60);
                        if (razlika > 0) {
                            alertDialog.setMessage("Imate dogadjaj za " + String.valueOf(razlika) + " minuta!");
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                            break;
                        }
                        else if (razlika == 0) {
                            alertDialog.setMessage("Trenutno traje događaj iz kalendara!");
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            alertDialog.show();
                            break;
                        }
                    }
                }
            }
        }
        if (moguceIgrati) {
            Intent myIntent = new Intent(KvizoviAkt.this, IgrajKvizAkt.class);
            myIntent.putExtra("kvizkojiseigra", igrajovajkviz);
            startActivityForResult(myIntent, 1);
        }
    }

    @SuppressLint("NewApi")
    public void dobaviDogadjajeizKalendara(View v) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, MY_CAL_REQ);
        }
        Cursor cur = null;
        ContentResolver cr = getContentResolver();

        String[] mProjection =
                {
                        "_id",
                        CalendarContract.Events.TITLE,
                        CalendarContract.Events.EVENT_LOCATION,
                        CalendarContract.Events.DTSTART,
                        CalendarContract.Events.DTEND,
                };

        Uri uri = CalendarContract.Events.CONTENT_URI;
        String[] selectionArgs = new String[]{"London"};

        cur = (Cursor) cr.query(uri,mProjection,null,null);

        while (cur.moveToNext()) {
            String title = cur.getString(cur.getColumnIndex(CalendarContract.Events.TITLE));
            Timestamp ts=new Timestamp(cur.getLong(cur.getColumnIndex(CalendarContract.Events.DTSTART)));
            Pair p = vratiParImeDatumDogadjaja(ts);
            dogadjajiDatumVrijeme.add(p);
        }
    }

    private Pair<String,String> vratiParImeDatumDogadjaja(Timestamp ts) {
        Date date=new Date(ts.getTime());
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        String datum = formatter.format(date);
        formatter = new SimpleDateFormat("HH:mm");
        String vrijeme = formatter.format(date);
        Log.d("dogadjaj", datum + " " + vrijeme);
        Pair p = new Pair(datum, vrijeme);
        return p;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (kategorijaAdapter.getItem(position).getNaziv().equals("Svi")) {
            kvizAdapter = new KvizAdapter(this, kvizovi);
            listaKvizova.setAdapter(kvizAdapter);
        }
        else {
            Kategorija kat = kategorijaAdapter.getItem(position);
            kvizAdapter.getFilter().filter(kat.getNaziv(), new Filter.FilterListener() {
                @Override
                public void onFilterComplete(int count) {

                }
            });
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }

    @Override
    public void onItemClicked (int pos) {
        Configuration configuration = getResources().getConfiguration();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        DetailFrag detailFrag = new DetailFrag();

        Bundle argumenti = new Bundle();
        argumenti.putSerializable("listakategorijaBUNDLE",kategorije);
        argumenti.putSerializable("listakvizovaBUNDLE", kvizovePosalji);

        detailFrag.setArguments(argumenti);
        fragmentTransaction.replace(R.id.detailPlace, detailFrag);
        fragmentTransaction.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Kviz x;
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                DodajKvizAkt.mogucapitanja.clear();
                if (spinner != null) {
                    super.onActivityResult(requestCode, resultCode, data);
                    kategorije = (ArrayList<Kategorija>) data.getSerializableExtra("vracam_kategorije");
                    kategorijaAdapter = new KategorijaAdapter(this, android.R.layout.simple_spinner_dropdown_item, kategorije);
                    spinner.setAdapter(kategorijaAdapter);
                    kategorijaAdapter.notifyDataSetChanged();
                    for (int i = 0; i < kategorije.size(); i++)
                        if (kategorije.get(i).getNaziv().equalsIgnoreCase("Dodaj Kategoriju"))
                            kategorije.remove(i);
                    kategorijaAdapter.notifyDataSetChanged();
                    spinner.setSelection(0);

                    kategorijuPosaljiUFragment = (Kategorija) data.getSerializableExtra("kojajekategorija");
                    staroImeKviza = (String) data.getSerializableExtra("staroImeKviza");
                    dodavanje = (int) data.getSerializableExtra("dodavanje");

                    x = (Kviz) data.getSerializableExtra("kviz");

                    if (dodavanje == 1) {
                        kvizovi.remove(posaljiKviz);
                        kvizAdapter.notifyDataSetChanged();
                        kvizovi.add(x);
                        kvizAdapter.notifyDataSetChanged();
                        System.out.print("");
                    }
                    /*else if (dodavanje == 10){
                        int i = 0;
                        for (Kviz k : kvizovi) {
                            if (k.getNaziv().equals(staroImeKviza)) {
                                kvizovi.remove(i);
                                kvizAdapter.notifyDataSetChanged();
                                kvizovi.add(x);
                                kvizAdapter.notifyDataSetChanged();
                                break;
                            }
                            i++;
                        }
                        kvizAdapter.notifyDataSetChanged();
                    }*/
                }
                //spinner == null
                else {
                    super.onActivityResult(requestCode,resultCode, data);
                    kategorije = (ArrayList<Kategorija>) data.getSerializableExtra("vracam_kategorije");
                    int i = 0;
                    for (Kategorija k : kategorije) {
                        if (k.getNaziv().equals("Dodaj Kategoriju"))
                            break;
                        i++;
                    }
                    kategorije.remove(i);
                    kategorijePosalji = kategorije;
                    kategorijuPosaljiUFragment = null;
                    x = (Kviz) data.getSerializableExtra("kviz");

                    staroImeKviza = (String) data.getSerializableExtra("staroImeKviza");
                    dodavanje = (int) data.getSerializableExtra("dodavanje");
                    if (dodavanje == 1) {
                        kvizovi.add(x);
                        kvizAdapter.notifyDataSetChanged();
                    }
                    else if (dodavanje == 0){
                        i = 0;
                        for (Kviz k : kvizovi) {
                            if (k.getNaziv().equals(staroImeKviza)) {
                                kvizovi.remove(i);
                                kvizAdapter.notifyDataSetChanged();
                                kvizovi.add(x);
                                kvizAdapter.notifyDataSetChanged();
                                break;
                            }
                            i++;
                        }
                        kvizAdapter.notifyDataSetChanged();
                    }
                }
            }
        }

        //kada je kliknuto BACK
        if (requestCode == 8) {
            if (resultCode == RESULT_OK) {
                kategorije = (ArrayList<Kategorija>) data.getSerializableExtra("vracam_kategorije");
                kategorijaAdapter = new KategorijaAdapter(this, android.R.layout.simple_spinner_dropdown_item, kategorije);
                spinner.setAdapter(kategorijaAdapter);
                kategorijaAdapter.notifyDataSetChanged();
                int i = 0;
                for (Kategorija k : kategorije) {
                    if (k.getNaziv().equals("Dodaj Kategoriju"))
                        break;
                    i++;
                }
                kategorije.remove(i);
                kategorijaAdapter.notifyDataSetChanged();
                spinner.setSelection(0);
            }
        }
    }
}
