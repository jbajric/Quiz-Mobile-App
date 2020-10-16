package ba.unsa.etf.rma.ba.unsa.etf.aktivnosti;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.Lists;
import com.maltaisn.icondialog.*;

import java.io.BufferedInputStream;
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
import java.util.Iterator;
import java.util.Map;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.ba.unsa.etf.AsyncResponse;
import ba.unsa.etf.rma.ba.unsa.etf.klase.*;

import static ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.KvizoviAkt.TOKEN;
import static ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.KvizoviAkt.kategorije;
import static ba.unsa.etf.rma.ba.unsa.etf.aktivnosti.KvizoviAkt.spisakKategorijaFirestore;

public class DodajKategorijuAkt extends AppCompatActivity implements IconDialog.Callback {
    private EditText nazivKategorije;
    private EditText idIkone;
    private Button dodajIkonu;
    private Button dodajKategoriju;
    private Icon[] selectedIcons;
    private String ID = "";
    private Kategorija kat = new Kategorija(" ", " ");
    private String nazivkat = "";
    private IconDialog iconDialog = new IconDialog();
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

    public class DodajKategorijunaBazu extends AsyncTask<String, Void, String> {
        AsyncResponse delegate = null;

        public DodajKategorijunaBazu(AsyncResponse delegate) { this.delegate = delegate; }
        @Override
        protected String doInBackground(String... strings) {
            GoogleCredential credentials;
            try {
                InputStream tajnaStream = getResources().openRawResource(R.raw.secret);
                credentials = GoogleCredential.fromStream(tajnaStream).createScoped(Lists.<String>newArrayList("https://www.googleapis.com/auth/datastore"));
                credentials.refreshToken();
                TOKEN = credentials.getAccessToken();
                String imeKategorijeUTF = URLEncoder.encode(nazivKategorije.getText().toString().trim(), "utf-8");
                String urll = "https://firestore.googleapis.com/v1/projects/rma19bajricjasmin/databases/(default)/documents/Kategorije?documentId=" + imeKategorijeUTF;
                URL url = new URL(urll);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Authorization", "Bearer " + TOKEN);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                String dokument = "{ \"fields\": { \"naziv\": {\"stringValue\":\"" + nazivKategorije.getText().toString() +
                                    "\"},\"idIkonice\": {\"stringValue\":\"" + idIkone.getText().toString() + "\"}}}";
                try(OutputStream os = connection.getOutputStream()) {
                    byte[] input = dokument.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                int code = connection.getResponseCode(); //response kod
                InputStream odgovor = connection.getInputStream();
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
        setContentView(R.layout.activity_dodaj_kategoriju_akt);
        nazivKategorije = findViewById(R.id.etNaziv);
        idIkone = findViewById(R.id.etIkona);
        idIkone.setEnabled(false);

        dodajIkonu = findViewById(R.id.btnDodajIkonu);
        dodajKategoriju = findViewById(R.id.btnDodajKategoriju);

        //kliknuto dodaj ikonu
        dodajIkonu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iconDialog.setSelectedIcons(selectedIcons);
                iconDialog.show(getSupportFragmentManager(), "icon_dialog");
            }
        });

        //kliknuto dodaj kategoriju
        dodajKategoriju.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imalVajerlesailiTriGea()) {
                    final Intent myIntent = new Intent(DodajKategorijuAkt.this, DodajKvizAkt.class);
                    nazivkat = nazivKategorije.getText().toString();
                    ID = idIkone.getText().toString();
                    boolean postoji = false;
                    for (Kategorija x : DodajKvizAkt.listaKategorija) {
                        System.out.println("OVO JE KATEGORIJA " + x.getNaziv() + " POREDI SA " + nazivKategorije.getText().toString());
                        if (nazivKategorije.getText().toString().trim().equals(x.getNaziv().trim())) {
                            postoji = true;
                            AlertDialog alertDialog = new AlertDialog.Builder(DodajKategorijuAkt.this).create();
                            alertDialog.setTitle("Greška u dodavanju nove kategorije");
                            alertDialog.setMessage("Unesena kategorija već postoji");
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
                    Iterator it = spisakKategorijaFirestore.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry mapa = (Map.Entry) it.next();
                        Kategorija x = (Kategorija) mapa.getKey();
                        System.out.println("KATEGORIJA MAPA : " + x.getNaziv() + " POREDI SA " + nazivKategorije.getText().toString());
                        if (x.getNaziv().trim().equals(nazivKategorije.getText().toString().trim())) {
                            postoji = true;
                            AlertDialog alertDialog = new AlertDialog.Builder(DodajKategorijuAkt.this).create();
                            alertDialog.setTitle("Greška u dodavanju nove kategorije");
                            alertDialog.setMessage("Unesena kategorija već postoji");
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
                        kat.setNaziv(nazivkat);
                        kat.setId(ID);
                        new DodajKategorijunaBazu(new AsyncResponse() {
                            @Override
                            public void processFinish(String output) {
                                myIntent.putExtra("kategorija", kat);
                                setResult(4); //DodajKategoriju.akt requestcode u startactivity je 4
                                setResult(RESULT_OK, myIntent);
                                finish();
                            }
                        }).execute("dodavanje nove kategorije");
                    }
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), "Niste konektovani na internet!", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    @Override
    public void onIconDialogIconsSelected(Icon[] icons) {
        selectedIcons = icons;
        if (!String.valueOf(selectedIcons[0].getId()).isEmpty()) {
            ID = String.valueOf(selectedIcons[0].getId());
            idIkone.setText(ID);
            idIkone.setBackgroundColor(Color.LTGRAY);
        }
    }


}