package ba.unsa.etf.rma.ba.unsa.etf.aktivnosti;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.ba.unsa.etf.AsyncResponse;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Kategorija;
import ba.unsa.etf.rma.ba.unsa.etf.adapteri.KategorijaAdapter;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Kviz;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Pitanje;
import ba.unsa.etf.rma.ba.unsa.etf.adapteri.PitanjeAdapter;

import static ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.KvizoviAkt.TOKEN;
import static ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.KvizoviAkt.spisakKvizovaFirestore;
import static ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.KvizoviAkt.svaPitanjaBaze;

public class DodajKvizAkt extends AppCompatActivity {


    static final int READ_REQUEST_CODE = 44;
    private ArrayList<Pitanje> listaPitanja = new ArrayList<>();
    public static ArrayList<Pitanje> mogucapitanja = new ArrayList<>();
    final Kategorija dodaj_kategoriju = new Kategorija("Dodaj Kategoriju", " ");
    final Comparator<Pitanje> comp = new Comparator<Pitanje>() {
        @Override
        public int compare(Pitanje c1, Pitanje c2) { return c1.getNaziv().compareTo(c2.getNaziv());
        }
    };
    boolean postojiKategorija = false;
    private ListView ListaDodanaPitanja;
    private ListView ListaMogucaPitanja;
    private Spinner spinner;
    private EditText nazivKviza;
    private Button dodajKviz;
    private Button importujKviz;
    private Kviz primljeniKviz = null;
    private KategorijaAdapter kategorijaAdapter;
    private Kviz kvizDatoteka = null;
    private Kategorija kategorijaDatoteka = null;
    public static ArrayList<Kategorija> listaKategorija = new ArrayList<>();
    private ArrayList<Kviz> listaKvizova = new ArrayList<>();
    private PitanjeAdapter pitanjeAdapter;
    private PitanjeAdapter mogucapitanjaAdapter;
    private Kviz vrati_kviz = null;
    private boolean postoji = false;

    private String readTextFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            stringBuilder.append(line + "\n");
        inputStream.close();
        return stringBuilder.toString();
    }
    private Kviz citajDatoteku(String sadrzajDatoteke) {
        Kviz kvizVrati = null;
        String[] uvod = sadrzajDatoteke.split("\n");
        if (uvod.length == 0)
            return kvizVrati;
        String[] sadrzaj = uvod[0].split(",");

        //naziv kviza
        String naziv_kviza = sadrzaj[0];
        for (Kviz k : listaKvizova) {
            if (k.getNaziv().equals(naziv_kviza)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage("Kviz kojeg importujete već postoji!");
                builder.show();
                return kvizVrati;
            }
        }

        //pitanja u kvizu!!!!!!
        int brojPitanja = Integer.parseInt(sadrzaj[2]);
        System.out.println("BROJ PITANJAAAAAAAAAAAAAAAAAAAAAAA: " + brojPitanja);
        if (brojPitanja != uvod.length - 1) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage("Kviz kojeg importujete ima neispravan broj pitanja!");
            builder.show();
            return kvizVrati;
        }

        int trenutniRed = 1;
        while (trenutniRed < uvod.length) {
            //potrebni podaci:
            String nazivPitanja = " ";
            int brojOdgovora;
            int indexTacnogOdgovora;
            String tacanOdgovor = " ";
            ArrayList<String> odgovoriNaPitanja = new ArrayList<>();
            Pitanje pitanje = null;

            //analogno za prvi red, gledamo ostale redove datoteke
            String[] redPodataka = uvod[trenutniRed].split(",");
            //provjera broja odgovora
            if (Integer.parseInt(redPodataka[1]) == redPodataka.length - 3) {
                if (Integer.parseInt(redPodataka[2]) >= redPodataka.length - 3 || Integer.parseInt(redPodataka[2]) < 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage("Kviz kojeg importujete ima neispravan broj odgovora!");
                    builder.show();
                    return kvizVrati;
                }
                nazivPitanja = redPodataka[0];
                try {
                    brojOdgovora = Integer.parseInt(redPodataka[1]);
                    indexTacnogOdgovora = Integer.parseInt(redPodataka[2]);
                } catch (NumberFormatException e) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage("Kviz kojeg importujete ima neispravan index tačnog odgovora!");
                    builder.show();
                    return kvizVrati;
                }

                //crpimo odgovore iz datoteke
                int i = 3;
                while (i < redPodataka.length) {
                    if (i - 3 == indexTacnogOdgovora)
                        tacanOdgovor = redPodataka[i];
                    odgovoriNaPitanja.add(redPodataka[i]);
                    i++;
                }

                pitanje = new Pitanje(nazivPitanja, nazivPitanja, odgovoriNaPitanja, tacanOdgovor);
                listaPitanja.add(pitanje);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage("Kviz kojeg importujete ima neispravan index tačnog odgovora!");
                builder.show();
            }
            trenutniRed++;
        }

        for (int i = 0; i < listaPitanja.size(); i++)
            for (int j = i + 1; j < listaPitanja.size(); j++) {
                String prvi = listaPitanja.get(i).getNaziv();
                String drugi = listaPitanja.get(j).getNaziv();
                if (prvi.equals(drugi)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage("Kviz nije ispravan, postoje dva pitanja sa istim nazivom!");
                    builder.show();
                    return kvizVrati;
                }
            }
        //naziv kategorije
        String naziv_kategorije = sadrzaj[1];
        kategorijaDatoteka = new Kategorija(naziv_kategorije, "99");
        for (Kategorija k : listaKategorija) {
            if (k.getNaziv().matches(kategorijaDatoteka.getNaziv())) {
                postojiKategorija = true;
                break;
            }
        }
        kvizVrati = new Kviz(naziv_kviza, listaPitanja, kategorijaDatoteka);
        return kvizVrati;
    }

    public class DodajKviznaBazu extends AsyncTask<String, Void, String> {
        AsyncResponse delegate = null;

        public DodajKviznaBazu(AsyncResponse delegate) { this.delegate = delegate; }
        @Override
        protected String doInBackground(String... strings) {
            GoogleCredential credentials;
            try {
                InputStream tajnaStream = getResources().openRawResource(R.raw.secret);
                credentials = GoogleCredential.fromStream(tajnaStream).createScoped(Lists.<String>newArrayList("https://www.googleapis.com/auth/datastore"));
                credentials.refreshToken();
                TOKEN = credentials.getAccessToken();
                System.out.print("NAZIV VRATI KVIZ " + vrati_kviz.getNaziv());
                String imeKvizaUTF = URLEncoder.encode(nazivKviza.getText().toString().trim(), "utf-8");
                String urll = "https://firestore.googleapis.com/v1/projects/rma19bajricjasmin/databases/(default)/documents/Kvizovi?documentId=" + imeKvizaUTF;
                URL url = new URL(urll);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", "Bearer " + TOKEN);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                String dokument = "{\"fields\": {\"naziv\": {\"stringValue\": \"" + nazivKviza.getText().toString().trim() +
                        "\"},\"idKategorije\": {\"stringValue\": \"" + vrati_kviz.getKategorija().getId()+ "\"}," +
                        "\"pitanja\": {\"arrayValue\": {\"values\": [";
                if (vrati_kviz.getPitanja().size() != 0) {
                    for (int i = 0; i < vrati_kviz.getPitanja().size(); i++) {
                        dokument += "{\"stringValue\": \"" + vrati_kviz.getPitanja().get(i).getNaziv().trim() + "\"}";
                        if (i != vrati_kviz.getPitanja().size() - 1)
                            dokument += ",";
                    }
                }
                else
                    dokument += "{\"stringValue\": \"\"}";
                dokument += "]}}}}";
                try(OutputStream os = connection.getOutputStream()) {
                    byte[] input = dokument.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int code = connection.getResponseCode(); //response kod
                InputStream odgovor = connection.getInputStream();
                System.out.println("KOLEKCIJA KVIZOVA: \n" + odgovor.toString());
                try (BufferedReader br = new BufferedReader(new InputStreamReader(odgovor, "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null)
                        response.append(responseLine.trim());
                    Log.d("ODGOVOR", response.toString());
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            delegate.processFinish(s);
        }
    }

    public class ObrisiKvizsaBaze extends AsyncTask<String, Void, String> {
        AsyncResponse delegate = null;

        public ObrisiKvizsaBaze(AsyncResponse delegate) { this.delegate = delegate; }
        @Override
        protected String doInBackground(String... strings) {
            GoogleCredential credentials;
            try {
                InputStream tajnaStream = getResources().openRawResource(R.raw.secret);
                credentials = GoogleCredential.fromStream(tajnaStream).createScoped(Lists.<String>newArrayList("https://www.googleapis.com/auth/datastore"));
                credentials.refreshToken();
                TOKEN = credentials.getAccessToken();
                System.out.print("NAZIV VRATI KVIZ " + primljeniKviz.getNaziv());
                String imeKvizaUTF = URLEncoder.encode(primljeniKviz.getNaziv().trim(), "utf-8");
                String urll = "https://firestore.googleapis.com/v1/projects/rma19bajricjasmin/databases/(default)/documents/Kvizovi/" + imeKvizaUTF.trim() + "?access_token=" + TOKEN;
                URL url = new URL(urll);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", TOKEN);
                connection.setRequestMethod("DELETE");
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                /*String dokument = "{\"fields\": {\"naziv\": {\"stringValue\": \"" + primljeniKviz.getNaziv().trim() +
                        "\"},\"idKategorije\": {\"stringValue\": \"" + vrati_kviz.getKategorija().getId()+ "\"}," +
                        "\"pitanja\": {\"arrayValue\": {\"values\": [";
                if (vrati_kviz.getPitanja().size() != 0) {
                    for (int i = 0; i < vrati_kviz.getPitanja().size(); i++) {
                        dokument += "{\"stringValue\": \"" + vrati_kviz.getPitanja().get(i).getNaziv().trim() + "\"}";
                        if (i != vrati_kviz.getPitanja().size() - 1)
                            dokument += ",";
                    }
                }
                else
                    dokument += "{\"stringValue\": \"\"}";
                dokument += "]}}}}";*/
                String dokument = "";
                try(OutputStream os = connection.getOutputStream()) {
                    byte[] input = dokument.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int code = connection.getResponseCode(); //response kod
                InputStream odgovor = connection.getInputStream();
                System.out.println("KOLEKCIJA KVIZOVA: \n" + odgovor.toString());
                try (BufferedReader br = new BufferedReader(new InputStreamReader(odgovor, "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null)
                        response.append(responseLine.trim());
                    Log.d("ODGOVOR", response.toString());
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            delegate.processFinish(s);
        }
    }

    public class AzurirajKviznaBazu extends AsyncTask<String, Void, String> {
        AsyncResponse delegate = null;

        public AzurirajKviznaBazu(AsyncResponse delegate) { this.delegate = delegate; }
        @Override
        protected String doInBackground(String... strings) {
            GoogleCredential credentials;
            try {
                InputStream tajnaStream = getResources().openRawResource(R.raw.secret);
                credentials = GoogleCredential.fromStream(tajnaStream).createScoped(Lists.<String>newArrayList("https://www.googleapis.com/auth/datastore"));
                credentials.refreshToken();
                TOKEN = credentials.getAccessToken();
                System.out.print("NAZIV VRATI KVIZ " + vrati_kviz.getNaziv());
                String imeKvizaUTF = URLEncoder.encode(vrati_kviz.getNaziv().trim(), "utf-8");
                String urll = "https://firestore.googleapis.com/v1/projects/rma19bajricjasmin/databases/(default)/documents/Kvizovi?documentId=" + imeKvizaUTF;
                URL url = new URL(urll);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", "Bearer " + TOKEN);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                String dokument = "{\"fields\": {\"naziv\": {\"stringValue\": \"" + vrati_kviz.getNaziv().trim() +
                        "\"},\"idKategorije\": {\"stringValue\": \"" + vrati_kviz.getKategorija().getId()+ "\"}," +
                        "\"pitanja\": {\"arrayValue\": {\"values\": [";
                if (vrati_kviz.getPitanja().size() != 0) {
                    for (int i = 0; i < vrati_kviz.getPitanja().size(); i++) {
                        dokument += "{\"stringValue\": \"" + vrati_kviz.getPitanja().get(i).getNaziv().trim() + "\"}";
                        if (i != vrati_kviz.getPitanja().size() - 1)
                            dokument += ",";
                    }
                }
                else
                    dokument += "{\"stringValue\": \"\"}";
                dokument += "]}}}}";
                try(OutputStream os = connection.getOutputStream()) {
                    byte[] input = dokument.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int code = connection.getResponseCode(); //response kod
                InputStream odgovor = connection.getInputStream();
                System.out.println("KOLEKCIJA KVIZOVA: \n" + odgovor.toString());
                try (BufferedReader br = new BufferedReader(new InputStreamReader(odgovor, "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null)
                        response.append(responseLine.trim());
                    Log.d("ODGOVOR", response.toString());
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            delegate.processFinish(s);
        }
    }

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj_kviz_akt);
        nazivKviza = findViewById(R.id.etNaziv);
        spinner = findViewById(R.id.spKategorije);
        ListaDodanaPitanja = findViewById(R.id.lvDodanaPitanja);
        ListaMogucaPitanja = findViewById(R.id.lvMogucaPitanja);
        dodajKviz = findViewById(R.id.btnDodajKviz);
        importujKviz = findViewById(R.id.btnImportKviz);
        listaKategorija = (ArrayList<Kategorija>) getIntent().getSerializableExtra("svekategorije");
        listaKvizova = (ArrayList<Kviz>) getIntent().getSerializableExtra("kvizovi_iste_kategorije");

        //adapter za spinner
        kategorijaAdapter = new KategorijaAdapter(this, android.R.layout.simple_spinner_dropdown_item, listaKategorija);
        kategorijaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(kategorijaAdapter);

        //postavljanje kategorije u spinneru
        Kategorija spinnerKategorija = (Kategorija) getIntent().getSerializableExtra("kategorijakviza");
        int i = 0;
        for (Kategorija k : listaKategorija) {
            if (k.getNaziv().equals(spinnerKategorija.getNaziv()))
                break;
            i++;
        }
        spinner.setSelection(i-1);

        //adapter za dodana pitanja
        pitanjeAdapter = new PitanjeAdapter(this, listaPitanja);
        ListaDodanaPitanja.setAdapter(pitanjeAdapter);

        //adapter za moguca pitanja
        mogucapitanjaAdapter = new PitanjeAdapter(this, mogucapitanja);
        ListaMogucaPitanja.setAdapter(mogucapitanjaAdapter);
        mogucapitanjaAdapter.notifyDataSetChanged();

        //dohvatanje kviza iz Kvizovi.akt
        primljeniKviz = (Kviz) getIntent().getSerializableExtra("kviz");
        nazivKviza.setText(" ");

        //novi kviz se kreira
        if (primljeniKviz == null) {
            mogucapitanja.clear();
            mogucapitanjaAdapter.notifyDataSetChanged();
            mogucapitanja.addAll(svaPitanjaBaze);
            mogucapitanjaAdapter.notifyDataSetChanged();
            pitanjeAdapter = new PitanjeAdapter(this, listaPitanja);
            ListaDodanaPitanja.setAdapter(pitanjeAdapter);
        }
        //ažurira se postojeći kviz
        else {
            if (!primljeniKviz.getNaziv().equals("Dodaj kviz"))
                nazivKviza.setText(primljeniKviz.getNaziv());
            listaPitanja = primljeniKviz.getPitanja();
            pitanjeAdapter = new PitanjeAdapter(this, listaPitanja);
            ListaDodanaPitanja.setAdapter(pitanjeAdapter);
            mogucapitanja.clear();
            mogucapitanjaAdapter.notifyDataSetChanged();
            if (svaPitanjaBaze != null && svaPitanjaBaze.size() != 0) {
                mogucapitanja.clear();
                if (primljeniKviz.getPitanja() != null) {
                    for (Pitanje pBaza : svaPitanjaBaze) {
                        boolean pitanjeUKvizu = false;
                        for (Pitanje p : primljeniKviz.getPitanja()) {
                            if (pBaza.getNaziv().equals(p.getNaziv())) {
                                pitanjeUKvizu = true;
                                break;
                            }
                        }
                        if (!pitanjeUKvizu)
                            mogucapitanja.add(pBaza);
                    }
                }
                else
                    mogucapitanja.addAll(svaPitanjaBaze);
            }
            mogucapitanjaAdapter.notifyDataSetChanged();
        }

        listaKategorija.add(dodaj_kategoriju);
        kategorijaAdapter.notifyDataSetChanged();


        //DODAJ PITANJE NA DNU LISTVIEWA
        View dodajPitanjeElement = getLayoutInflater().inflate(R.layout.dodajpitanje_listviewfooter, null);
        ListaDodanaPitanja.addFooterView(dodajPitanjeElement);
        ListaDodanaPitanja.setAdapter(pitanjeAdapter);

        //šaltanje pitanja iz dodanih u moguća pitanja i obratno
        ListaDodanaPitanja.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Pitanje pitanje = listaPitanja.get(position);
                mogucapitanja.add(pitanje);
                mogucapitanjaAdapter.notifyDataSetChanged();
                listaPitanja.remove(pitanje);
                pitanjeAdapter.notifyDataSetChanged();
            }
        });

        ListaMogucaPitanja.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Pitanje pitanje = mogucapitanja.get(position);
                listaPitanja.add(pitanje);
                pitanjeAdapter.notifyDataSetChanged();
                mogucapitanja.remove(pitanje);
                mogucapitanjaAdapter.notifyDataSetChanged();

            }
        });

        //klikanje na spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Kategorija kat = (Kategorija) spinner.getSelectedItem();
                if (kat.getNaziv().equals("Dodaj Kategoriju")) {
                    if (imalVajerlesailiTriGea()) {
                        Intent myIntent = new Intent(DodajKvizAkt.this, DodajKategorijuAkt.class);
                        startActivityForResult(myIntent, 4);
                    }
                    else {
                        Toast toast = Toast.makeText(getApplicationContext(), "Niste konektovani na internet!", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        //kliknuto na dodaj pitanje
        dodajPitanjeElement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (imalVajerlesailiTriGea()) {
                    Intent myIntent = new Intent(DodajKvizAkt.this, DodajPitanjeAkt.class);
                    String imeKviza = nazivKviza.getText().toString();
                    myIntent.putExtra("pitanja_kviza", listaPitanja);
                    myIntent.putExtra("naziv_kviza", imeKviza);
                    startActivityForResult(myIntent, 3);
                }
                else {
                    Toast toast = Toast.makeText(getApplicationContext(), "Niste konektovani na internet!", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });


        //kliknuto na dodaj kviz
        dodajKviz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imalVajerlesailiTriGea()) {
                    final Intent myIntent = new Intent();
                    myIntent.putExtra("vracam_kategorije", listaKategorija);
                    final Kategorija vrati_kat = (Kategorija) spinner.getSelectedItem();
                    final String vrati_imeKviza = nazivKviza.getText().toString().trim();
                    vrati_kviz = new Kviz(vrati_imeKviza, listaPitanja, vrati_kat);
                    String imePrimljenogKviza = "";
                    nazivKviza.setBackgroundColor(android.R.color.background_light);
                    if (!vrati_imeKviza.trim().isEmpty()) {
                        if (primljeniKviz != null) {
                            vrati_kviz.setNaziv(nazivKviza.getText().toString());
                            vrati_kviz.setPitanja(listaPitanja);
                            mogucapitanja.clear();
                            new ObrisiKvizsaBaze(new AsyncResponse() {
                                @Override
                                public void processFinish(String output) {
                                }
                            }).execute("obriši kviz");
                            new AzurirajKviznaBazu(new AsyncResponse() {
                                @Override
                                public void processFinish(String output) {
                                    myIntent.putExtra("dodavanje", 1);
                                    myIntent.putExtra("staroImeKviza", vrati_imeKviza);
                                    myIntent.putExtra("kviz", vrati_kviz);
                                    myIntent.putExtra("kojajekategorija", vrati_kat);
                                    setResult(RESULT_OK, myIntent);
                                    mogucapitanja.clear();
                                    finish();
                                }
                            }).execute("ažuriranje postojećeg kviza");

                        } else {
                            postoji = false;
                            for (Kviz x : KvizoviAkt.kvizovi) {
                                if (nazivKviza.getText().toString().equals(x.getNaziv())) {
                                    postoji = true;
                                    android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(DodajKvizAkt.this).create();
                                    alertDialog.setTitle("Greška u dodavanju novog kviza");
                                    alertDialog.setMessage("Uneseni kviz već postoji");
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
                            Iterator it = spisakKvizovaFirestore.entrySet().iterator();
                            while (it.hasNext()) {
                                Map.Entry mapa = (Map.Entry) it.next();
                                Kviz x = (Kviz) mapa.getKey();
                                if (x.getNaziv().trim().equals(nazivKviza.getText().toString().trim())) {
                                    postoji = true;
                                    android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(DodajKvizAkt.this).create();
                                    alertDialog.setTitle("Greška u dodavanju novog kviza");
                                    alertDialog.setMessage("Uneseni kviz već postoji");
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
                            if (!postoji) {
                                vrati_kviz.setNaziv(nazivKviza.getText().toString());
                                vrati_kviz.setPitanja(listaPitanja);
                                mogucapitanja.clear();
                                new DodajKviznaBazu(new AsyncResponse() {
                                    @Override
                                    public void processFinish(String output) {
                                        myIntent.putExtra("dodavanje", 1);
                                        myIntent.putExtra("staroImeKviza", nazivKviza.getText().toString());
                                        myIntent.putExtra("kviz", vrati_kviz);
                                        setResult(RESULT_OK, myIntent);
                                        mogucapitanja.clear();
                                        finish();
                                    }
                                }).execute("dodavanje starog kviza");
                            }
                        }
                    } else {
                        nazivKviza.setBackgroundColor(R.color.crvena);
                    }
                }
                else {
                    Toast toast = Toast.makeText(getApplicationContext(), "Niste konektovani na internet!", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        //IMPORTUJ KVIZ
        importujKviz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imalVajerlesailiTriGea()) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("text/*");
                    if (intent.resolveActivity(getPackageManager()) != null)
                        startActivityForResult(intent, READ_REQUEST_CODE);
                }
                else {
                    Toast toast = Toast.makeText(getApplicationContext(), "Niste konektovani na internet!", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //importovanje kviza putem csv fajla
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3) { //dodaj pitanje povratak
            if (resultCode == RESULT_OK) {
                Pitanje primljenoPitanje = (Pitanje) data.getSerializableExtra("pitanje");
                listaPitanja.add(primljenoPitanje);
                pitanjeAdapter.notifyDataSetChanged();
                ListaDodanaPitanja.setAdapter(pitanjeAdapter);
            }
        } else if (requestCode == 4) { //dodaj kategorija povratak
            if (resultCode == RESULT_OK) {
                Kategorija primljenaKategorija = (Kategorija) data.getSerializableExtra("kategorija");
                listaKategorija.add(primljenaKategorija);
                kategorijaAdapter.notifyDataSetChanged();
                spinner.setSelection(listaKategorija.size() - 1);
                kategorijaAdapter.notifyDataSetChanged();
            }
        }
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
                try {
                    String dat = readTextFromUri(uri);
                    kvizDatoteka = citajDatoteku(dat);
                    if (kvizDatoteka != null) {
                        nazivKviza.setText(kvizDatoteka.getNaziv());
                        listaPitanja = kvizDatoteka.getPitanja();
                        pitanjeAdapter = new PitanjeAdapter(this, listaPitanja);
                        ListaDodanaPitanja.setAdapter(pitanjeAdapter);
                    }
                    if (postojiKategorija) {
                        int brojac = 0;
                        for (Kategorija k : listaKategorija) {
                            if (k.getNaziv().matches(kategorijaDatoteka.getNaziv()))
                                break;
                            brojac++;
                        }
                        spinner.setSelection(brojac);
                        kategorijaAdapter.notifyDataSetChanged();
                    } else {
                        listaKategorija.add(kategorijaDatoteka);
                        kategorijaAdapter = new KategorijaAdapter(this, android.R.layout.simple_spinner_dropdown_item, listaKategorija);
                        spinner.setAdapter(kategorijaAdapter);
                        spinner.setSelection(listaKategorija.size() - 1);
                        kategorijaAdapter.notifyDataSetChanged();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent myIntent = new Intent(DodajKvizAkt.this, KvizoviAkt.class);
        myIntent.putExtra("vracam_kategorije", listaKategorija);
        Kviz vrati_kviz = new Kviz(nazivKviza.getText().toString(), listaPitanja, (Kategorija) spinner.getSelectedItem());
        myIntent.putExtra("kviz", vrati_kviz);
        myIntent.putExtra("dodavanje", -1);
        setResult(8);
        setResult(RESULT_OK, myIntent);
        super.onBackPressed();
        finish(); ///provjeriti da li treba
    }


}
