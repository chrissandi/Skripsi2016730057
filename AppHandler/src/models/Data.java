package models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Data {

	private StringProperty alamat;
	private StringProperty time;
	private FloatProperty x;
	private FloatProperty y;
	private FloatProperty z;
	
	public Data(String alamat, String time, float x, float y, float z) {
		this.alamat = new SimpleStringProperty(alamat);
		this.time = new SimpleStringProperty(time);
		this.x = new SimpleFloatProperty(x);
		this.y = new SimpleFloatProperty(y);
		this.z = new SimpleFloatProperty(z);
	}

	public StringProperty alamatProperty() {
		return alamat;
	}

	public void setAlamatProperty(StringProperty alamat) {
		this.alamat = alamat;
	}

	public StringProperty timeProperty() {
		return time;
	}

	public void setTimeProperty(StringProperty time) {
		this.time = time;
	}

	public FloatProperty xProperty() {
		return x;
	}

	public void setXProperty(FloatProperty x) {
		this.x = x;
	}

	public FloatProperty yProperty() {
		return y;
	}

	public void setYProperty(FloatProperty y) {
		this.y = y;
	}

	public FloatProperty zProperty() {
		return z;
	}

	public void setZProperty(FloatProperty z) {
		this.z = z;
	}
	
}
