package cu.lazaroysr96.gci;

import cu.lazaroysr96.accounting.models.*;

import java.io.File;
import java.sql.*;
import java.util.*;

public class GCTCPDataBase {
    private final String path;

    public GCTCPDataBase(String path) {
        this.path = path;
    }

    private Connection open() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver no encontrado", e);
        }
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path);
        conn.createStatement().execute("PRAGMA foreign_keys = ON;");
        return conn;
    }

    public String getPath() {
        return path;
    }

    /** Crea el esquema de tablas si la base de datos aún no existe (igual que la app Android). */
    public void createSchemaIfNotExists() {
        if (tableExists(new File(path))) return;
        String[] ddl = {
            "CREATE TABLE wallets (" +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "moneda TEXT NOT NULL DEFAULT 'CUP', " +
                "name TEXT NOT NULL, " +
                "description TEXT," +
                "type TEXT NOT NULL DEFAULT 'BANCO', " +
                "saldo REAL NOT NULL DEFAULT 0," +
                "cuenta TEXT," +
                "tarjeta TEXT" +
                ");",
            "CREATE TABLE wallets_movements(" +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "fromWalletId TEXT, " +
                "toWalletId TEXT, " +
                "description TEXT," +
                "type TEXT NOT NULL DEFAULT 'ENTRADA', " +
                "amount REAL NOT NULL DEFAULT 0," +
                "createAt INTEGER NOT NULL," +
                "FOREIGN KEY(fromWalletId) REFERENCES wallets(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(toWalletId) REFERENCES wallets(id) ON DELETE CASCADE);",
            "CREATE TABLE members (" +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "lastname TEXT NOT NULL, " +
                "sexo INTEGER NOT NULL," +
                "fecha_nacimiento INTEGER," +
                "direccion TEXT," +
                "telefono TEXT," +
                "correo TEXT," +
                "estado_civil TEXT," +
                "fecha_ingreso INTEGER," +
                "bautizado INTEGER," +
                "ministerio TEXT," +
                "diezmo_regular INTEGER," +
                "nota TEXT" +
                ");",
            "CREATE TABLE aportes (" +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "walletId TEXT NOT NULL, " +
                "memberId TEXT NOT NULL, " +
                "movementId TEXT NOT NULL, " +
                "amount REAL NOT NULL," +
                "fecha INTEGER NOT NULL," +
                "description TEXT," +
                "nota TEXT," +
                "concepto TEXT NOT NULL DEFAULT 'Diezmo'" +
                ");",
            "CREATE TABLE eventos (" +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "nombre TEXT NOT NULL, " +
                "fecha INTEGER," +
                "presupuesto REAL NOT NULL DEFAULT 0," +
                "personas_esperadas INTEGER NOT NULL DEFAULT 0," +
                "objetivo TEXT," +
                "notas TEXT," +
                "estado TEXT NOT NULL DEFAULT 'Planificado'," +
                "created_at INTEGER" +
                ");"
        };
        try (Connection conn = open(); Statement st = conn.createStatement()) {
            for (String sql : ddl) st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Hace una copia del archivo de base de datos (respaldo/exportación). */
    public void exportTo(String destPath) throws Exception {
        try (Connection conn = open()) {
            conn.createStatement().execute("PRAGMA wal_checkpoint(TRUNCATE);");
        }
        copy(new File(path), new File(destPath));
    }

    /** Reemplaza el contenido de esta base con la del archivo indicado (importación). */
    public void importFrom(String srcPath) throws Exception {
        File src = new File(srcPath);
        if (!src.exists() || !src.isFile()) throw new Exception("Archivo no encontrado: " + srcPath);
        if (!tableExists(src)) throw new Exception("El archivo no es una base de datos de GCI válida.");
        File dest = new File(path);
        if (dest.getParentFile() != null) dest.getParentFile().mkdirs();
        copy(src, dest);
    }

    private static void copy(File src, File dst) throws Exception {
        try (java.io.FileInputStream in = new java.io.FileInputStream(src);
             java.io.FileOutputStream out = new java.io.FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
        }
    }

    public static boolean tableExists(File dbFile) {
        return tableExists(dbFile, "members") && tableExists(dbFile, "aportes");
    }

    // ---------- MEMBERS ----------

    public ArrayList<Member> getMembers() {
        ArrayList<Member> list = new ArrayList<>();
        String sql = "SELECT * FROM members ORDER BY id";
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet res = ps.executeQuery()) {
            while (res.next()) {
                Member item = new Member();
                item.id = res.getString("id");
                item.name = res.getString("name");
                item.lastname = res.getString("lastname");
                item.sexo = res.getInt("sexo");
                item.fecha_nacimiento = res.getLong("fecha_nacimiento");
                item.direccion = res.getString("direccion");
                item.telefono = res.getString("telefono");
                item.correo = res.getString("correo");
                item.estado_civil = res.getString("estado_civil");
                item.fecha_ingreso = res.getLong("fecha_ingreso");
                item.bautizado = res.getInt("bautizado") > 0;
                item.ministerio = res.getString("ministerio");
                item.diezmo_regular = res.getInt("diezmo_regular") > 0;
                item.nota = res.getString("nota");
                list.add(item);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public int getTotalMembers() {
        return queryInt("SELECT COUNT(*) FROM members", new String[0]);
    }

    // ---------- APORTES ----------

    public ArrayList<Aporte> getAllAportes() {
        ArrayList<Aporte> list = new ArrayList<>();
        String sql = "SELECT * FROM aportes ORDER BY fecha DESC";
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet res = ps.executeQuery()) {
            while (res.next()) {
                Aporte item = new Aporte();
                item.id = res.getString("id");
                item.walletId = res.getString("walletId");
                item.memberId = res.getString("memberId");
                item.movementId = res.getString("movementId");
                item.amount = res.getDouble("amount");
                item.fecha = res.getLong("fecha");
                item.description = res.getString("description");
                item.nota = res.getString("nota");
                try { item.concepto = res.getString("concepto"); } catch (SQLException ignore) {}
                if (item.concepto == null) item.concepto = Aporte.CONCEPTO_DIEZMO;
                list.add(item);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public double getTotalAportes() {
        return queryDouble("SELECT SUM(amount) FROM aportes", new String[0]);
    }

    public double getTotalAportesByConcept(String concepto) {
        return queryDouble("SELECT SUM(amount) FROM aportes WHERE concepto = ?",
                           new String[]{concepto});
    }

    public double getAportesFromMemberId(String memberId) {
        return queryDouble("SELECT SUM(amount) FROM aportes WHERE memberId = ?",
                           new String[]{memberId});
    }

    public double getTotalAportesByPeriod(long inicio, long fin) {
        return queryDouble("SELECT SUM(amount) FROM aportes WHERE fecha >= ? AND fecha <= ?",
                           new String[]{String.valueOf(inicio), String.valueOf(fin)});
    }

    public double getTotalAportesByConceptPeriod(String concepto, long inicio, long fin) {
        return queryDouble("SELECT SUM(amount) FROM aportes WHERE concepto = ? AND fecha >= ? AND fecha <= ?",
                           new String[]{concepto, String.valueOf(inicio), String.valueOf(fin)});
    }

    // ---------- WALLETS ----------

    public ArrayList<Wallet> getWallets() {
        ArrayList<Wallet> list = new ArrayList<>();
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM wallets ORDER BY name");
             ResultSet res = ps.executeQuery()) {
            while (res.next()) {
                Wallet item = new Wallet();
                item.id = res.getString("id");
                item.moneda = res.getString("moneda");
                item.name = res.getString("name");
                item.description = res.getString("description");
                item.type = res.getString("type");
                item.saldo = res.getDouble("saldo");
                item.cuenta = res.getString("cuenta");
                item.tarjeta = res.getString("tarjeta");
                list.add(item);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public double getTotalSaldo() {
        return queryDouble("SELECT SUM(saldo) FROM wallets", new String[0]);
    }

    public ArrayList<WalletMovement> getWalletsMovements() {
        ArrayList<WalletMovement> list = new ArrayList<>();
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM wallets_movements ORDER BY createAt DESC");
             ResultSet res = ps.executeQuery()) {
            while (res.next()) {
                WalletMovement item = new WalletMovement();
                item.id = res.getString("id");
                item.fromWalletId = res.getString("fromWalletId");
                item.toWalletId = res.getString("toWalletId");
                item.description = res.getString("description");
                item.type = res.getString("type");
                item.amount = res.getDouble("amount");
                item.createAt = res.getLong("createAt");
                list.add(item);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    // ---------- EVENTOS ----------

    public ArrayList<Evento> getEventos() {
        ArrayList<Evento> list = new ArrayList<>();
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM eventos ORDER BY fecha DESC");
             ResultSet res = ps.executeQuery()) {
            while (res.next()) {
                Evento item = new Evento();
                item.id = res.getString("id");
                item.nombre = res.getString("nombre");
                item.fecha = res.getLong("fecha");
                item.presupuesto = res.getDouble("presupuesto");
                item.personasEsperadas = res.getInt("personas_esperadas");
                item.objetivo = res.getString("objetivo");
                item.notas = res.getString("notas");
                item.estado = res.getString("estado");
                item.createdAt = res.getLong("created_at");
                list.add(item);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public double getTotalPresupuestoEventos() {
        return queryDouble("SELECT SUM(presupuesto) FROM eventos", new String[0]);
    }

    // ---------- helpers ----------

    private int queryInt(String sql, String[] args) {
        return (int) queryDouble(sql, args);
    }

    private double queryDouble(String sql, String[] args) {
        try (Connection conn = open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setString(i + 1, args[i]);
            try (ResultSet res = ps.executeQuery()) {
                if (res.next()) return res.getDouble(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    public static boolean tableExists(File dbFile, String table) {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, table);
                try (ResultSet res = ps.executeQuery()) {
                    return res.next();
                }
            }
        } catch (Exception e) {
            return false;
        }
    }
}