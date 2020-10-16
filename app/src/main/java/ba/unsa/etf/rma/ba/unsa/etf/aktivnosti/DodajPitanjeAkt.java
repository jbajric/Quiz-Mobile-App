package ba.unsa.etf.rma.ba.unsa.etf.aktivnosti;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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
import java.util.Iterator;
import java.util.Map;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.ba.unsa.etf.AsyncResponse;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Kviz;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Pitanje;

import static ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.KvizoviAkt.TOKEN;
import static ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.KvizoviAkt.svaPitanjaBaze;

public class DodajPitanjeAkt extends AppCompatActivity {
    private ListView listaOdgovora;
    private EditText nazivPitanja;
    private EditText Odgovor;
    private Button dodajOdgovor;
    private Button dodajTacanOdgovor;
    private Button dodajPitanje;

    private ArrayAdapter<String> odgovoriAdapter;
    private ArrayList<String> odgovori = new ArrayList<>();
    private String tacanOdg = "";
    private String odgovor = "";
    private int pozicijaTacnog = -1;
    private boolean postoji = false;
    private ArrayList<Pitanje> pitanja = new ArrayList<>();
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


    public class DodajPitanjenaBazu extends AsyncTask<String, Void, String> {
        AsyncResponse delegate = null;

        public DodajPitanjenaBazu(AsyncResponse delegate) { this.delegate = delegate; }
        @Override
        protected String doInBackground(String... strings) {
            GoogleCredential credentials;
            try {
                InputStream tajnaStream = getResources().openRawResource(R.raw.secret);
                credentials = GoogleCredential.fromStream(tajnaStream).createScoped(Lists.<String>newArrayList("https://www.googleapis.com/auth/datastore"));
                credentials.refreshToken();
                TOKEN = credentials.getAccessToken();
                String imeKvizaUTF = URLEncoder.encode(nazivPitanja.getText().toString().trim(), "utf-8");
                String urll = "https://firestore.googleapis.com/v1/projects/rma19bajricjasmin/databases/(default)/documents/Pitanja?documentId=" + imeKvizaUTF;
                URL url = new URL(urll);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", "Bearer " + TOKEN);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                String dokument = "{ \"fields\": { \"indexTacnog\": {\"integerValue\": \"" + String.valueOf(pozicijaTacnog) + "\"}," +
                                "\"naziv\": {\"stringValue\":\"" + nazivPitanja.getText().toString() + "\"}," +
                                "\"odgovori\": {\"arrayValue\": {\"values\": [";
                if (odgovori.size() != 0) {
                    for (int i = 0; i < odgovori.size(); i++) {
                        dokument += "{\"stringValue\": \"" + odgovori.get(i).trim() + "\"}";
                        if (i != odgovori.size() - 1)
                            dokument += ",";
                    }
                }
                else
                    dokument += "{\"stringValue\": \"\"}";
                dokument += "]}}}}";
                System.out.println("DOKUMENT " + dokument);
                try(OutputStream os = connection.getOutputStream()) {
                    byte[] input = dokument.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int code = connection.getResponseCode(); //response kod
                InputStream odgovor = connection.getInputStream();
                System.out.println("KOLEKCIJA PITANJA: \n" + odgovor.toString());
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dodaj_pitanje_akt);
        listaOdgovora = findViewById(R.id.lvOdgovori);
        nazivPitanja = findViewById(R.id.etNaziv);
        Odgovor = findViewById(R.id.etOdgovor);
        dodajOdgovor = findViewById(R.id.btnDodajOdgovor);
        dodajTacanOdgovor = findViewById(R.id.btnDodajTacan);
        dodajPitanje = findViewById(R.id.btnDodajPitanje);
        odgovoriAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, odgovori) {
            @Override
            public View getView ( int position, View convertView, ViewGroup parent){
                View v = super.getView(position, convertView, parent);
                if (position == pozicijaTacnog)
                    v.setBackgroundColor(Color.GREEN);
                return v;
            }
        };
        listaOdgovora.setAdapter(odgovoriAdapter);
        pitanja = (ArrayList<Pitanje>) getIntent().getSerializableExtra("pitanja_kviza");

        //kliknuto na odgovor --briše se isti
        listaOdgovora.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String x = (String) parent.getItemAtPosition(position);
                odgovori.remove(x);
                odgovoriAdapter.notifyDataSetChanged();
                listaOdgovora.setAdapter(odgovoriAdapter);
            }
        });


        //DODAVANJE PITANJA => mora imati jedan tačan odgovor!

        dodajOdgovor.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onClick(View v) {
                boolean ispravno = true;
                odgovor = Odgovor.getText().toString();
                if (!odgovor.trim().isEmpty()) {
                    for (String x : odgovori)
                        if (x.equalsIgnoreCase(odgovor))
                            ispravno = false;
                    if (ispravno){
                        odgovori.add(odgovor);
                        odgovoriAdapter.notifyDataSetChanged();
                        Odgovor.setText("");
                        Odgovor.setBackgroundColor(android.R.color.white);
                    }
                    else
                        Odgovor.setBackgroundColor(R.color.crvena); //crvena
                }
                else
                    Odgovor.setBackgroundColor(R.color.crvena); //crvena
            }
        });

        dodajTacanOdgovor.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onClick(View v) {
                boolean ispravno = true;
                odgovor = Odgovor.getText().toString();
                Odgovor.setBackgroundColor(android.R.color.white); //bijela
                if (!odgovor.trim().isEmpty()) {
                    for (String x : odgovori)
                        if (x.equalsIgnoreCase(odgovor))
                            ispravno = false;
                    if (ispravno){
                        tacanOdg = odgovor;
                        odgovori.add(odgovor);
                        odgovoriAdapter.notifyDataSetChanged();
                        Odgovor.setText("");
                        dodajTacanOdgovor.setClickable(false);
                        pozicijaTacnog = odgovori.size() - 1;
                        listaOdgovora.setAdapter(odgovoriAdapter);
                    }
                    else
                        Odgovor.setBackgroundColor(R.color.crvena); //crvena
                }
                else
                    Odgovor.setBackgroundColor(R.color.crvena); //crvena
            }
        });

        dodajPitanje.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onClick(View v) {
                if (imalVajerlesailiTriGea()) {
                    final Intent myIntent = new Intent(DodajPitanjeAkt.this, DodajKvizAkt.class);
                    final Pitanje pitanje;
                    nazivPitanja.setBackgroundColor(android.R.color.white);
                    if (nazivPitanja.getText().toString().trim().isEmpty()) {
                        nazivPitanja.setBackgroundColor(R.color.crvena);
                        return;
                    }
                    if (!tacanOdg.trim().isEmpty()) {
                        postoji = false;
                        for (Pitanje x : pitanja)
                            if (x.getNaziv().equals(nazivPitanja.getText().toString())) {
                                postoji = true;
                                AlertDialog alertDialog = new AlertDialog.Builder(DodajPitanjeAkt.this).create();
                                alertDialog.setTitle("Greška u dodavanju novog pitanja");
                                alertDialog.setMessage("Uneseno pitanje već postoji");
                                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                                break;
                            }
                        Iterator it = KvizoviAkt.spisakPitanjaFirestore.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry mapa = (Map.Entry) it.next();
                            Pitanje x = (Pitanje) mapa.getKey();
                            System.out.println("PITANJE MAPA : " + x.getNaziv() + " POREDI SA " + nazivPitanja.getText().toString());
                            if (x.getNaziv().trim().equals(nazivPitanja.getText().toString().trim())) {
                                postoji = true;
                                AlertDialog alertDialog = new AlertDialog.Builder(DodajPitanjeAkt.this).create();
                                alertDialog.setTitle("Greška u dodavanju novog pitanja");
                                alertDialog.setMessage("Uneseno pitanje već postoji");
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
                            pitanje = new Pitanje(nazivPitanja.getText().toString().trim(), nazivPitanja.getText().toString().trim(), odgovori, tacanOdg);
                            svaPitanjaBaze.add(pitanje);
                            new DodajPitanjenaBazu(new AsyncResponse() {
                                @Override
                                public void processFinish(String output) {
                                    myIntent.putExtra("pitanje", pitanje);
                                    setResult(3); //u dodajkviz.akt je request code 3 za dodaj pitanje
                                    setResult(RESULT_OK, myIntent);
                                    finish();
                                }
                            }).execute("dodavanje novog pitanja");
                        }
                    }
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), "Niste konektovani na internet!", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }
}
