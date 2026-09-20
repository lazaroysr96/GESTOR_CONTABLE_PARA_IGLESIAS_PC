package cu.lazaroysr96.accounting.models;
import com.google.gson.*;

public class WalletMovement
{
	public String id;
    public String fromWalletId;
	public String toWalletId;
	public String description;
	public String type;
    public double amount;
    public long createAt;

    @Override
    public String toString()
    {
        return new Gson().toJson(this);
    }
    
    
}


