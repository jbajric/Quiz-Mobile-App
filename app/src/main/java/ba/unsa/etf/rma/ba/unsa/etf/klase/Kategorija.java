package ba.unsa.etf.rma.ba.unsa.etf.klase;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;

public class Kategorija implements Serializable {
    private String naziv;
    private String id;

    public Kategorija(String naziv, String id) {
        this.naziv = naziv;
        this.id = id;
    }

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIDBAZA() {
        String ID = "";
        return ID;
    }
}
