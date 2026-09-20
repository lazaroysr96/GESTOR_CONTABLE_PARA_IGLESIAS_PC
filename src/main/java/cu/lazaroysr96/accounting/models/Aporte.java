package cu.lazaroysr96.accounting.models;
import java.util.*;

public class Aporte {
	
    public String id;
    public String walletId;
	public String memberId;
    public String movementId;
    public double amount;
    public long fecha;
    public String description;
	public String nota;
	public String concepto;

    public static final String CONCEPTO_DIEZMO = "Diezmo";
    public static final String CONCEPTO_OFRENDA = "Ofrenda";
    public static final String CONCEPTO_COMPROMISO = "Compromiso del Miembro (Pro-Templo)";

    public static final String[] CONCEPTOS = {
        CONCEPTO_DIEZMO,
        CONCEPTO_OFRENDA,
        CONCEPTO_COMPROMISO
    };

    public Aporte() {
        this.id = UUID.randomUUID().toString();
        this.fecha = System.currentTimeMillis();
        this.concepto = CONCEPTO_DIEZMO;
    }
}