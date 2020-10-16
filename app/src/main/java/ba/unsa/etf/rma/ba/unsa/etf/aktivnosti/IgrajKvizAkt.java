package ba.unsa.etf.rma.ba.unsa.etf.aktivnosti;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.AlarmClock;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;


import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.ba.unsa.etf.AsyncResponse;
import ba.unsa.etf.rma.ba.unsa.etf.fragmenti.InformacijeFrag;
import ba.unsa.etf.rma.ba.unsa.etf.fragmenti.PitanjeFrag;
import ba.unsa.etf.rma.ba.unsa.etf.fragmenti.RangLista;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Kviz;
import ba.unsa.etf.rma.ba.unsa.etf.klase.Pitanje;

import static ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.KvizoviAkt.TOKEN;
import static ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.KvizoviAkt.convertStreamToString;


public class IgrajKvizAkt extends AppCompatActivity implements PitanjeFrag.OnItemClick {
    public static Kviz k;
    public static int brojTacnihOdgovora = 0;
    public static int brojPreostalihPitanja = 0;
    public static double procenatTacnih = 0;
    public static int brojacPitanja = 0;
    private EditText nazivIgraca;

    /**********************************ASYNC KLASE**********************************/
    public class DodajRanglistunaBazu extends AsyncTask<String, Void, String> {
        AsyncResponse delegate = null;

        public DodajRanglistunaBazu(AsyncResponse delegate) { this.delegate = delegate; }

        @Override
        protected String doInBackground(String... strings) {
            GoogleCredential credentials;
            try {
                InputStream tajnaStream = getResources().openRawResource(R.raw.secret);
                credentials = GoogleCredential.fromStream(tajnaStream).createScoped(Lists.<String>newArrayList("https://www.googleapis.com/auth/datastore"));
                credentials.refreshToken();
                TOKEN = credentials.getAccessToken();
                String imeKvizaUTF = URLEncoder.encode(k.getNaziv().trim(), "utf-8");
                String urll = "https://firestore.googleapis.com/v1/projects/rma19bajricjasmin/databases/(default)/documents/Rangliste?documentId=" + imeKvizaUTF+"&access_token=";
                URL url = new URL(urll + URLEncoder.encode(TOKEN, "utf-8"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                String dokument = "{ \"fields\": { \"nazivKviza\": {\"stringValue\":\"" + k.getNaziv() + "\"}, " +
                        "\"lista\": {\"mapValue\": {\"fields\": {\"1\": {\"mapValue\": {\"fields\": {\"" + strings[0] + "\": {\"doubleValue\": " + procenatTacnih + "}}}}";
                dokument += "}}}}}";
                try(OutputStream os = connection.getOutputStream()) {
                    byte[] input = dokument.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                int code = connection.getResponseCode(); //response kod
                InputStream odgovor = connection.getInputStream();
                System.out.println("KOLEKCIJA RANGLISTA: \n" + odgovor.toString());
                try (BufferedReader br = new BufferedReader(new InputStreamReader(odgovor, StandardCharsets.UTF_8))) {
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
        public void onPostExecute(String res) {
            delegate.processFinish(res);
        }
    }

    public class AzurirajRanglistu extends AsyncTask<String, Void, String> {
        AsyncResponse delegate = null;

        public AzurirajRanglistu(AsyncResponse delegate) { this.delegate = delegate; }

        @Override
        protected String doInBackground(String... strings) {
            GoogleCredential credentials;
            try {
                InputStream tajnaStream = getResources().openRawResource(R.raw.secret);
                credentials = GoogleCredential.fromStream(tajnaStream).createScoped(Lists.<String>newArrayList("https://www.googleapis.com/auth/datastore"));
                credentials.refreshToken();
                TOKEN = credentials.getAccessToken();
                String imeKvizaUTF = URLEncoder.encode(k.getNaziv().trim(), "utf-8");
                String urll = "https://firestore.googleapis.com/v1/projects/rma19bajricjasmin/databases/(default)/documents/Rangliste/" + imeKvizaUTF + "?currentDocument.exists=true&access_token=";
                URL url = new URL(urll + URLEncoder.encode(TOKEN, "utf-8"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("PATCH");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                String dokument = "{ \"fields\": { \"nazivKviza\": {\"stringValue\":\"" + k.getNaziv() + "\"}, " +
                                    "\"lista\": {\"mapValue\": {\"fields\": {\"";
                //dohvatamo podatke za prikaz
                for (int i = 0; i < RangLista.lista.size(); i++) {
                    String igrac =  RangLista.lista.get(i);
                    dokument += igrac.charAt(0) + "\": {\"mapValue\": {\"fields\": {\"";
                    int j;
                    for (j = 3; j < igrac.length(); j++)  // broj.*space*_ (naziv igrača počinje na crtici, 3. karakter)
                        if (igrac.charAt(j) == ':') break;
                    dokument += igrac.substring(3, j) +  "\": {\"doubleValue\": ";  //naziv igrača
                    j += 2;
                    Double procenat = Double.valueOf(igrac.substring(j, igrac.length()-2));
                    dokument += procenat + "}}}}, \"";
                }
                dokument += String.valueOf(RangLista.lista.size()+1) + "\": {\"mapValue\": {\"fields\": {\"" + strings[0] + "\": {\"doubleValue\": " + procenatTacnih + "}}}}";
                dokument += "}}}}}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = dokument.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                int code = connection.getResponseCode();
                InputStream odgovor = connection.getInputStream();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(odgovor, StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        public void onPostExecute(String res) {
            delegate.processFinish(res);
        }
    }

    public class PreuzmiRanglisteizBaze extends AsyncTask<String, Void, String> {
        AsyncResponse delegate = null;

        public PreuzmiRanglisteizBaze(AsyncResponse delegate) { this.delegate = delegate; }

        @Override
        protected String doInBackground(String... strings) {
            InputStream is = getResources().openRawResource(R.raw.secret);
            try {
                GoogleCredential credentials = GoogleCredential.fromStream(is).createScoped(Lists.newArrayList("https://www.googleapis.com/auth/datastore"));
                credentials.refreshToken();
                TOKEN = credentials.getAccessToken();
                String urll = "https://firestore.googleapis.com/v1/projects/rma19bajricjasmin/databases/(default)/documents/Rangliste?access_token=";
                URL url = new URL(urll + URLEncoder.encode(TOKEN, "utf-8"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", "Bearer"+TOKEN);
                InputStream ins = connection.getInputStream();
                String rezultatRanglista = convertStreamToString(ins);
                // nije null
                System.out.println("KOLEKCIJA RANGLISTA: ");
                System.out.println(rezultatRanglista);
                JSONObject jo = new JSONObject(rezultatRanglista);
                String naziv;
                JSONObject fields;
                JSONArray joRanglista = null;
                RangLista.lista.clear();
                if (jo.has("documents")) {
                    joRanglista = jo.getJSONArray("documents");
                    for (int i = 0; i < joRanglista.length(); i++) {
                        JSONObject jedanKviz = joRanglista.getJSONObject(i);
                        if (jedanKviz.has("fields")) {
                            fields = jedanKviz.getJSONObject("fields");
                            JSONObject nazivKvizaObjekat;
                            if (fields.has("nazivKviza")) {
                                nazivKvizaObjekat = fields.getJSONObject("nazivKviza");
                                String nazivKviza = nazivKvizaObjekat.getString("stringValue");
                                if (nazivKviza.equals(k.getNaziv())) {
                                    JSONObject lista;
                                    if (fields.has("lista")) {
                                        lista = fields.getJSONObject("lista");
                                        JSONObject mapValue;
                                        if (lista.has("mapValue")) {
                                            mapValue = lista.getJSONObject("mapValue");
                                            JSONObject fields2;
                                            if (mapValue.has("fields")) {
                                                fields2 = mapValue.getJSONObject("fields");
                                                int j = 1;
                                                for (; ; ) {
                                                    String elementListe = "";
                                                    if (fields2.has(String.valueOf(j))) { // uzimamo prvu poziciju u kvizu
                                                        elementListe = String.valueOf(j) + ". ";
                                                        JSONObject kljuc = fields2.getJSONObject(String.valueOf(j));
                                                        if (kljuc.has("mapValue")) {
                                                            JSONObject mapValueVrijednost = kljuc.getJSONObject("mapValue");
                                                            if (mapValueVrijednost.has("fields")) {
                                                                JSONObject fields3 = mapValueVrijednost.getJSONObject("fields");
                                                                String imeIgraca = fields3.names().toString();
                                                                imeIgraca = imeIgraca.replace("[", "");
                                                                imeIgraca = imeIgraca.replace("\"", "");
                                                                imeIgraca = imeIgraca.replace("]", "");
                                                                elementListe += imeIgraca + ": ";
                                                                JSONObject vrijednostObjekat = fields3.getJSONObject(imeIgraca);
                                                                double procenat = vrijednostObjekat.getDouble("doubleValue");
                                                                elementListe += procenat + "%";
                                                            }
                                                        }
                                                    } else break;
                                                    RangLista.lista.add(elementListe);
                                                    ++j;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void onPostExecute (String res) {
            delegate.processFinish(res);
        }
    }

    /**********************************ASYNC KLASE**********************************/



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_igraj_kviz_akt);
        k = (Kviz) getIntent().getSerializableExtra("kvizkojiseigra");
        ArrayList<Pitanje> listaPitanjaKviza = new ArrayList<>();
        System.out.print("NAZIV" + k.getNaziv());
        Log.d("NAZIV KVIZA", k.getNaziv());
        listaPitanjaKviza = k.getPitanja();
        Log.d("BROJ", String.valueOf(k.getPitanja().size()));
        brojPreostalihPitanja = k.getPitanja().size();
        postaviAlarm(k);
        Collections.shuffle(listaPitanjaKviza);
        k.setPitanja(listaPitanjaKviza);

        Configuration config = getResources().getConfiguration();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        Bundle b = new Bundle();
        b.putSerializable("trenutnikviz",k);
        b.putSerializable("pitanjaizkviza", k.getPitanja());

        InformacijeFrag informacijeFrag = new InformacijeFrag();
        PitanjeFrag pitanjeFrag = new PitanjeFrag();
        //informacije
        fragmentTransaction.add(R.id.informacijePlace, informacijeFrag);
        informacijeFrag.setArguments(b);

        //pitanja
        fragmentTransaction.add(R.id.pitanjePlace, pitanjeFrag);
        pitanjeFrag.setArguments(b);

        fragmentTransaction.commit();
    }

    @SuppressLint("NewApi")
    private void postaviAlarm(Kviz k) {
        int brojPitanjaKviza = k.getPitanja().size();
        int minute = 0;
        int sati = 0;
        int sekunde = 0;
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
        int satiAlarma = time.getHour()+sati;
        int minuteAlarma = time.getMinute()+minute;
        Log.d("ALARM POSTAVLJEN: ", String.valueOf(satiAlarma) + ":" + String.valueOf(minuteAlarma));
        Intent intentAlarm = new Intent (AlarmClock.ACTION_SET_ALARM);
        intentAlarm.putExtra(AlarmClock.EXTRA_HOUR, satiAlarma);
        intentAlarm.putExtra(AlarmClock.EXTRA_MINUTES, minuteAlarma);
        intentAlarm.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        startActivity(intentAlarm);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK)
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    //kliknuto na neki odgovor
    @Override
    public void onItemClicked(final int pos) {
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                k = (Kviz) getIntent().getSerializableExtra("kvizkojiseigra");

                Configuration config = getResources().getConfiguration();
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fm.beginTransaction();

                Bundle b = new Bundle();
                b.putSerializable("trenutnikviz", k);
                b.putSerializable("pitanjaizkviza", k.getPitanja());

                InformacijeFrag informacijeFrag = new InformacijeFrag();
                PitanjeFrag pitanjeFrag = new PitanjeFrag();

                fragmentTransaction.replace(R.id.informacijePlace, informacijeFrag);
                fragmentTransaction.replace(R.id.pitanjePlace, pitanjeFrag);

                informacijeFrag.setArguments(b);
                pitanjeFrag.setArguments(b);

                brojacPitanja++;
                brojPreostalihPitanja = k.getPitanja().size() - brojacPitanja;
                if (brojacPitanja == 0)
                    procenatTacnih = 0;
                else
                    procenatTacnih = (double) brojTacnihOdgovora / brojacPitanja;
                fragmentTransaction.commit();
                if (brojPreostalihPitanja == 0)
                    dodajImeRanglista();
            }
        }, 2000);
    }

//rangliste
    public void dodajImeRanglista() {
        LayoutInflater li = LayoutInflater.from(IgrajKvizAkt.this);
        View view = li.inflate(R.layout.alert_dialog_ranglista, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(IgrajKvizAkt.this).setView(view);
        nazivIgraca = view.findViewById(R.id.imeIgraca);

        alertDialog.setMessage("Vaše ime: ").setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String ime = String.valueOf(nazivIgraca.getText().toString());
                new PreuzmiRanglisteizBaze(new AsyncResponse() {
                    @Override
                    public void processFinish(String output) {
                        if (RangLista.lista != null && RangLista.lista .size() != 0)
                            new AzurirajRanglistu(new AsyncResponse() {
                                @Override
                                public void processFinish(String output) { }
                            }).execute(String.valueOf(nazivIgraca.getText().toString().trim()));
                        else
                            new DodajRanglistunaBazu(new AsyncResponse() {
                                @Override
                                public void processFinish(String output) { }
                            }).execute(String.valueOf(nazivIgraca.getText().toString().trim()));
                        new PreuzmiRanglisteizBaze(new AsyncResponse() {
                            @Override
                            public void processFinish(String output) {
                                FragmentManager fm = getSupportFragmentManager();
                                FragmentTransaction ft = fm.beginTransaction();
                                RangLista rangLista = new RangLista();
                                ft.replace(R.id.pitanjePlace, rangLista).commit();
                            }
                        }).execute("proba");
                    }
                }).execute("operacije rangliste");
            }
        }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog al = alertDialog.create();
        al.show();
    }
}
