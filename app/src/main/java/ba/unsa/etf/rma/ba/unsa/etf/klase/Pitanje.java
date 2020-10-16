package ba.unsa.etf.rma.ba.unsa.etf.klase;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

public class Pitanje implements Serializable {
    private String naziv;
    private String tekstPitanja;
    private ArrayList<String> odgovori;
    private String tacan;

    public Pitanje(String naziv, String tekstPisanja, ArrayList<String> odgovori, String tacan) {
        this.naziv = naziv;
        this.tekstPitanja = tekstPisanja;
        this.odgovori = odgovori;
        this.tacan = tacan;
    }

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public String getTekstPitanja() {
        return tekstPitanja;
    }

    public void setTekstPitanja(String tekstPitanja) {
        this.tekstPitanja = tekstPitanja;
    }

    public ArrayList<String> getOdgovori() {
        return odgovori;
    }

    public void setOdgovori(ArrayList<String> odgovori) {
        this.odgovori = odgovori;
    }

    public String getTacan() {
        return tacan;
    }

    public void setTacan(String tacan) {
        this.tacan = tacan;
    }

    ArrayList<String> dajRandomOdgovore() {
        ArrayList<String> randomOdgovori = getOdgovori();
        Collections.shuffle(randomOdgovori);
        return randomOdgovori;
    }

    public String getIDBAZA() {
        String ID = "";
        return ID;
    }

}
