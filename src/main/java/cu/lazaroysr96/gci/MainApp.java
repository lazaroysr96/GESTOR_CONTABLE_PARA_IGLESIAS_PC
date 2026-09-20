package cu.lazaroysr96.gci;

import cu.lazaroysr96.accounting.models.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainApp extends Application {

    private GCTCPDataBase db;
    private File dbFile;
    private Label headerDbLabel;
    private final StackPane content = new StackPane();

    // Palette (similar to the Android app light theme)
    private static final String ACCENT = "#0080ff";
    private static final String GREEN = "#00A86B";
    private static final String RED = "#FF5252";
    private static final String TEXT = "#223344";
    private static final String MUTED = "#7A8BA6";
    private static final String CARD = "#FFFFFF";
    private static final String SIDE = "#0E2233";
    private static final String SIDE_ACCENT = "#13405F";

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);

        content.setPadding(new Insets(24));
        content.getChildren().add(buildWelcome());

        BorderPane header = buildHeader(stage);
        root.setTop(header);
        root.setCenter(content);

        openOwnDatabase();

        Scene scene = new Scene(root, 1080, 700);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setTitle("Gestor Contable para Iglesias — PC");
        stage.setScene(scene);
        stage.show();
    }

    private BorderPane buildHeader(Stage stage) {
        Label title = new Label("Gestor Contable para Iglesias");
        title.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:" + TEXT + ";");

        Label lblDb = new Label("Sin base de datos");
        lblDb.setStyle("-fx-text-fill:" + MUTED + "; -fx-font-size:12px;");
        lblDb.setId("lblDb");
        headerDbLabel = lblDb;

        Button btnImport = new Button("⬆ Importar del teléfono…");
        btnImport.setStyle("-fx-background-color:" + ACCENT + "; -fx-text-fill:white; -fx-background-radius:8px; -fx-padding:8px 14px; -fx-cursor:hand;");
        btnImport.setOnAction(e -> importDatabase(lblDb));

        Button btnExport = new Button("⬇ Exportar para el teléfono…");
        btnExport.setStyle("-fx-background-color:#2E7D32; -fx-text-fill:white; -fx-background-radius:8px; -fx-padding:8px 14px; -fx-cursor:hand;");
        btnExport.setOnAction(e -> exportDatabase());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox box = new HBox(12, title, spacer, lblDb, btnImport, btnExport);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(18, 24, 18, 24));
        box.setStyle("-fx-background-color:" + CARD + ";");

        BorderPane header = new BorderPane();
        header.setStyle("-fx-background-color:" + CARD + ";");
        header.setBottom(new Separator());
        header.setCenter(box);
        return header;
    }

    private VBox buildSidebar() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(18, 10, 18, 10));
        box.setStyle("-fx-background-color:" + SIDE + "; -fx-min-width:200;");

        Label logo = new Label("📖 GCI");
        logo.setStyle("-fx-text-fill:white; -fx-font-size:18px; -fx-font-weight:bold; -fx-padding:0 0 14 10;");

        Button bDash = navButton("🏠  Resumen");
        Button bMembers = navButton("👥  Miembros");
        Button bFinanzas = navButton("💰  Finanzas");
        Button bAportes = navButton("🙏  Aportes");
        Button bEventos = navButton("📅  Eventos");

        bDash.setOnAction(e -> content.getChildren().setAll(buildDashboard()));
        bMembers.setOnAction(e -> content.getChildren().setAll(buildMembers()));
        bFinanzas.setOnAction(e -> content.getChildren().setAll(buildFinanzas()));
        bAportes.setOnAction(e -> content.getChildren().setAll(buildAportes()));
        bEventos.setOnAction(e -> content.getChildren().setAll(buildEventos()));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button bExit = navButton("✖  Salir");
        bExit.setOnAction(e -> Platform.exit());

        box.getChildren().addAll(logo, bDash, bMembers, bFinanzas, bAportes, bEventos, spacer, bExit);
        return box;
    }

    private Button navButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:transparent; -fx-text-fill:#C6D3E0; -fx-alignment:CENTER_LEFT; -fx-padding:11px 14px; -fx-background-radius:8px; -fx-cursor:hand; -fx-font-size:14px;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color:" + SIDE_ACCENT + "; -fx-text-fill:white; -fx-alignment:CENTER_LEFT; -fx-padding:11px 14px; -fx-background-radius:8px; -fx-font-size:14px;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color:transparent; -fx-text-fill:#C6D3E0; -fx-alignment:CENTER_LEFT; -fx-padding:11px 14px; -fx-background-radius:8px; -fx-font-size:14px;"));
        return b;
    }

    // ---------- DATABASE ----------

    private void openOwnDatabase() {
        try {
            String home = System.getProperty("user.home");
            File dir = new File(home, "GCI");
            dir.mkdirs();
            dbFile = new File(dir, "database.db");
            db = new GCTCPDataBase(dbFile.getAbsolutePath());
            db.createSchemaIfNotExists();
            if (headerDbLabel != null) headerDbLabel.setText("DB local: " + dbFile.getAbsolutePath());
            content.getChildren().setAll(buildDashboard());
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo crear la base de datos local.");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void importDatabase(Label lblDb) {
        if (dbFile == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar database.db del teléfono");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Base de datos SQLite", "*.db"));
        File chosen = fc.showOpenDialog(null);
        if (chosen == null) return;

        if (!GCTCPDataBase.tableExists(chosen, "members") || !GCTCPDataBase.tableExists(chosen, "aportes")) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Base de datos no válida");
            alert.setHeaderText("El archivo seleccionado no corresponde a la app.");
            alert.showAndWait();
            return;
        }

        try {
            db.importFrom(chosen.getAbsolutePath());
            lblDb.setText("Última importación: " + chosen.getName());
            content.getChildren().setAll(buildDashboard());
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Importación completada");
            ok.setHeaderText("Se importaron los datos del teléfono a la base local.");
            ok.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al importar");
            alert.setHeaderText("No se pudo importar la base de datos.");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void exportDatabase() {
        if (db == null || dbFile == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar copia para el teléfono");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Base de datos SQLite", "*.db"));
        fc.setInitialFileName("gci_database.db");
        File chosen = fc.showSaveDialog(null);
        if (chosen == null) return;

        try {
            if (!chosen.getName().endsWith(".db")) chosen = new File(chosen.getAbsolutePath() + ".db");
            db.exportTo(chosen.getAbsolutePath());
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Exportación completada");
            ok.setHeaderText("Copia guardada en: " + chosen.getAbsolutePath());
            ok.setContentText("Lleve este archivo al teléfono y use la opción de restauración en la app.");
            ok.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al exportar");
            alert.setHeaderText("No se pudo exportar.");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    // ---------- PAGES ----------

    private VBox buildWelcome() {
        VBox v = new VBox(10);
        v.setAlignment(Pos.CENTER);
        Label t = new Label("Bienvenido");
        t.setStyle("-fx-font-size:26px; -fx-font-weight:bold; -fx-text-fill:" + TEXT + ";");
        Label sub = new Label("Abra el database.db exportado desde la app de Android para cargar los datos de la iglesia.");
        sub.setStyle("-fx-text-fill:" + MUTED + "; -fx-font-size:15px;");
        v.getChildren().addAll(t, sub);
        return v;
    }

    private TabPane buildTabs() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        return tabs;
    }

    private VBox page(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:" + TEXT + "; -fx-padding:0 0 14 0;");
        VBox v = new VBox(10, label);
        v.setPadding(new Insets(6));
        return v;
    }

    private VBox buildDashboard() {
        if (db == null) return noDb();
        VBox v = page("📊  Resumen de la iglesia");

        int totalMembers = db.getTotalMembers();
        double totalAportes = db.getTotalAportes();
        double totalSaldo = db.getTotalSaldo();
        double totalEventos = db.getTotalPresupuestoEventos();

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
            statCard("Miembros registrados", color(totalMembers) + "", "#0073e6"),
            statCard("Total aportado", "$ " + money(totalAportes), GREEN),
            statCard("Saldo en monederos", "$ " + money(totalSaldo), "#ffa000"),
            statCard("Presupuesto de eventos", "$ " + money(totalEventos), "#9c27b0"));

        VBox conceptCard = new VBox(10);
        conceptCard.setStyle(cardStyle());
        Label t = new Label("Aportes por concepto");
        t.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:" + TEXT + ";");
        conceptCard.getChildren().add(t);

        double diezmos = db.getTotalAportesByConcept(Aporte.CONCEPTO_DIEZMO);
        double ofrendas = db.getTotalAportesByConcept(Aporte.CONCEPTO_OFRENDA);
        double compromiso = db.getTotalAportesByConcept(Aporte.CONCEPTO_COMPROMISO);
        conceptCard.getChildren().add(conceptRow(Aporte.CONCEPTO_DIEZMO, diezmos, totalAportes));
        conceptCard.getChildren().add(conceptRow(Aporte.CONCEPTO_OFRENDA, ofrendas, totalAportes));
        conceptCard.getChildren().add(conceptRow(Aporte.CONCEPTO_COMPROMISO, compromiso, totalAportes));

        VBox resumen = new VBox(6);
        resumen.setStyle(cardStyle());
        Label rt = new Label("Este mes");
        rt.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:" + TEXT + ";");
        resumen.getChildren().add(rt);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        long monthStart = cal.getTimeInMillis();
        long now = System.currentTimeMillis();
        resumen.getChildren().add(summaryRow("Total aportado este mes", db.getTotalAportesByPeriod(monthStart, now)));
        resumen.getChildren().add(summaryRow("Diezmos este mes", db.getTotalAportesByConceptPeriod(Aporte.CONCEPTO_DIEZMO, monthStart, now)));
        resumen.getChildren().add(summaryRow("Ofrendas este mes", db.getTotalAportesByConceptPeriod(Aporte.CONCEPTO_OFRENDA, monthStart, now)));
        resumen.getChildren().add(summaryRow("Compromiso este mes", db.getTotalAportesByConceptPeriod(Aporte.CONCEPTO_COMPROMISO, monthStart, now)));

        HBox bottom = new HBox(16, conceptCard, resumen);
        HBox.setHgrow(conceptCard, Priority.ALWAYS);
        HBox.setHgrow(resumen, Priority.ALWAYS);

        v.getChildren().addAll(cards, bottom);
        return v;
    }

    private VBox buildMembers() {
        if (db == null) return noDb();
        VBox v = page("👥  Miembros");
        ArrayList<Member> members = db.getMembers();

        TableView<Member> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Member, String> cName = new TableColumn<>("Nombre");
        cName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().name));
        TableColumn<Member, String> cLast = new TableColumn<>("Apellidos");
        cLast.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().lastname));
        TableColumn<Member, String> cSex = new TableColumn<>("Sexo");
        cSex.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().sexo == 1 ? "F" : "M"));
        cSex.setPrefWidth(50);
        TableColumn<Member, String> cMin = new TableColumn<>("Ministerio");
        cMin.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().ministerio == null ? "" : c.getValue().ministerio));
        TableColumn<Member, String> cTel = new TableColumn<>("Teléfono");
        cTel.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().telefono == null ? "" : c.getValue().telefono));
        TableColumn<Member, String> cIng = new TableColumn<>("Ingresó");
        cIng.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().fecha_ingreso > 0 ? DATE_FMT.format(new Date(c.getValue().fecha_ingreso)) : "-"));
        TableColumn<Member, String> cAporte = new TableColumn<>("Aportado");
        cAporte.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty("$ " + money(db.getAportesFromMemberId(c.getValue().id))));
        cAporte.setStyle("-fx-alignment:CENTER; -fx-text-fill:" + GREEN + ";");

        table.getColumns().addAll(cName, cLast, cSex, cMin, cTel, cIng, cAporte);
        table.getItems().addAll(members);
        VBox.setVgrow(table, Priority.ALWAYS);
        v.getChildren().add(table);
        return v;
    }

    private VBox buildFinanzas() {
        if (db == null) return noDb();
        VBox v = page("💰  Finanzas");

        ArrayList<Wallet> wallets = db.getWallets();
        TabPane tabs = buildTabs();

        TableView<Wallet> tWallets = new TableView<>();
        tWallets.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Wallet, String> cN = new TableColumn<>("Nombre");
        cN.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().name));
        TableColumn<Wallet, String> cTipo = new TableColumn<>("Tipo");
        cTipo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().type));
        TableColumn<Wallet, String> cMoneda = new TableColumn<>("Moneda");
        cMoneda.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().moneda));
        TableColumn<Wallet, String> cSaldo = new TableColumn<>("Saldo");
        cSaldo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty("$ " + money(c.getValue().saldo)));
        tWallets.getColumns().addAll(cN, cTipo, cMoneda, cSaldo);
        tWallets.getItems().addAll(wallets);
        Tab tabW = new Tab("Monederos", tWallets);

        TableView<WalletMovement> tMov = new TableView<>();
        tMov.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<WalletMovement, String> mcFecha = new TableColumn<>("Fecha");
        mcFecha.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(DATE_FMT.format(new Date(c.getValue().createAt))));
        TableColumn<WalletMovement, String> mcDes = new TableColumn<>("Descripción");
        mcDes.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().description == null ? "" : c.getValue().description));
        TableColumn<WalletMovement, String> mcTipo = new TableColumn<>("Tipo");
        mcTipo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().type));
        TableColumn<WalletMovement, String> mcMonto = new TableColumn<>("Monto");
        mcMonto.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty("$ " + money(c.getValue().amount)));
        tMov.getColumns().addAll(mcFecha, mcDes, mcTipo, mcMonto);
        tMov.getItems().addAll(db.getWalletsMovements());
        Tab tabM = new Tab("Movimientos", tMov);

        tabs.getTabs().addAll(tabW, tabM);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        v.getChildren().add(tabs);
        return v;
    }

    private VBox buildAportes() {
        if (db == null) return noDb();
        VBox v = page("🙏  Aportes desglosados por concepto");

        ArrayList<Aporte> aportes = db.getAllAportes();
        double total = db.getTotalAportes();

        HBox chips = new HBox(12);
        for (String concepto : Aporte.CONCEPTOS) {
            chips.getChildren().add(chip(concepto, db.getTotalAportesByConcept(concepto), total));
        }
        v.getChildren().add(chips);

        TableView<Aporte> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Aporte, String> cFecha = new TableColumn<>("Fecha");
        cFecha.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(DATE_FMT.format(new Date(c.getValue().fecha))));
        TableColumn<Aporte, String> cConcepto = new TableColumn<>("Concepto");
        cConcepto.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().concepto));
        TableColumn<Aporte, String> cMember = new TableColumn<>("Miembro");
        cMember.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(memberName(c.getValue().memberId)));
        TableColumn<Aporte, String> cMonto = new TableColumn<>("Monto");
        cMonto.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty("$ " + money(c.getValue().amount)));
        table.getColumns().addAll(cFecha, cConcepto, cMember, cMonto);
        table.getItems().addAll(aportes);
        VBox.setVgrow(table, Priority.ALWAYS);
        v.getChildren().add(table);
        return v;
    }

    private VBox buildEventos() {
        if (db == null) return noDb();
        VBox v = page("📅  Eventos");

        ArrayList<Evento> eventos = db.getEventos();
        TableView<Evento> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Evento, String> cN = new TableColumn<>("Nombre");
        cN.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().nombre));
        TableColumn<Evento, String> cFecha = new TableColumn<>("Fecha");
        cFecha.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().fecha > 0 ? DATE_FMT.format(new Date(c.getValue().fecha)) : "-"));
        TableColumn<Evento, String> cP = new TableColumn<>("Presupuesto");
        cP.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty("$ " + money(c.getValue().presupuesto)));
        TableColumn<Evento, String> cPax = new TableColumn<>("Personas");
        cPax.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().personasEsperadas + ""));
        TableColumn<Evento, String> cObj = new TableColumn<>("Objetivo");
        cObj.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().objetivo == null ? "" : c.getValue().objetivo));
        TableColumn<Evento, String> cEstado = new TableColumn<>("Estado");
        cEstado.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().estado));
        table.getColumns().addAll(cN, cFecha, cP, cPax, cObj, cEstado);
        table.getItems().addAll(eventos);
        VBox.setVgrow(table, Priority.ALWAYS);
        v.getChildren().add(table);
        return v;
    }

    // ---------- helpers ----------

    private VBox noDb() {
        VBox v = new VBox(8);
        v.setAlignment(Pos.CENTER);
        Label t = new Label("Sin base de datos cargada");
        t.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:" + TEXT + ";");
        Label sub = new Label("Use el botón superior 'Abrir database.db de la iglesia…'");
        sub.setStyle("-fx-text-fill:" + MUTED + ";");
        v.getChildren().addAll(t, sub);
        return v;
    }

    private VBox statCard(String label, String value, String color) {
        VBox v = new VBox(6);
        v.setStyle(cardStyle());
        Label l = new Label(label);
        l.setStyle("-fx-text-fill:" + MUTED + "; -fx-font-size:13px;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:" + color + ";");
        v.getChildren().addAll(l, val);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    private String cardStyle() {
        return "-fx-background-color:" + CARD + "; -fx-background-radius:14px; -fx-padding:18px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);";
    }

    private HBox conceptRow(String name, double amount, double total) {
        double pct = total > 0 ? amount * 100 / total : 0;
        Label l = new Label(name);
        l.setStyle("-fx-text-fill:" + TEXT + "; -fx-font-size:14px; -fx-min-width:220;");
        Label val = new Label("$ " + money(amount));
        val.setStyle("-fx-font-weight:bold; -fx-text-fill:" + GREEN + ";");
        Label p = new Label(String.format(Locale.US, "%.1f%%", pct));
        p.setStyle("-fx-text-fill:" + MUTED + "; -fx-min-width:60; -fx-alignment:CENTER_RIGHT;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, l, spacer, val, p);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox summaryRow(String label, double amount) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill:" + MUTED + "; -fx-font-size:13px;");
        Label val = new Label("$ " + money(amount));
        val.setStyle("-fx-font-weight:bold; -fx-text-fill:" + TEXT + ";");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, l, spacer, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox chip(String name, double amount, double total) {
        double pct = total > 0 ? amount * 100 / total : 0;
        VBox v = new VBox(4);
        v.setStyle(cardStyle());
        Label l = new Label(name);
        l.setStyle("-fx-font-size:13px; -fx-text-fill:" + MUTED + ";");
        Label val = new Label("$ " + money(amount));
        val.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:" + GREEN + ";");
        Label p = new Label(String.format(Locale.US, "%.1f%% del total", pct));
        p.setStyle("-fx-font-size:11px; -fx-text-fill:" + MUTED + ";");
        v.getChildren().addAll(l, val, p);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    private String memberName(String id) {
        try {
            for (Member m : db.getMembers()) if (m.id.equals(id)) return m.name;
        } catch (Exception e) {
        }
        return id;
    }

    private String money(double v) {
        return String.format(Locale.US, "%,.2f", v);
    }

    private String color(int v) {
        return String.valueOf(v);
    }

    public static void main(String[] args) {
        launch(args);
    }
}