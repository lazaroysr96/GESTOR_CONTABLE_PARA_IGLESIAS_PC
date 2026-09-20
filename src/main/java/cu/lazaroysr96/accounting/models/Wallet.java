package cu.lazaroysr96.accounting.models;
import com.google.gson.*;

public class Wallet{
	public String id;
    public String moneda;
    public String name;
	public String description;
	public String type;
    public double saldo;
    public String cuenta;
    public String tarjeta;

	@Override
	public String toString(){
		return new Gson().toJson(this).toString();
	}
}
