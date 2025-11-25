// MainApp.java
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MainApp extends Application {

    private BirthdayDAO dao = new BirthdayDAO();
    private ObservableList<BirthdayRow> tableData = FXCollections.observableArrayList();

    private TableView<BirthdayRow> table;
    private TextField nameField;
    private DatePicker dobPicker;
    private TextField notesField;
    private TextField searchField;
    private ComboBox<String> monthFilter;

    private DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) {
        stage.setTitle("জন্মদিন ব্যবস্থাপক");

        // Input pane
        VBox inputBox = new VBox(8);
        inputBox.setPadding(new Insets(10));
        inputBox.setMaxWidth(350);

        Label title = new Label("শিক্ষার্থীর জন্মদিন যোগ/হালনাগাদ");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        nameField = new TextField();
        nameField.setPromptText("নাম");
        Label nameLabel = new Label("নাম:");

        dobPicker = new DatePicker();
        dobPicker.setPromptText("জন্ম তারিখ");
        Label dobLabel = new Label("জন্ম তারিখ:");

        notesField = new TextField();
        notesField.setPromptText("বিবরণ (ঐচ্ছিক)");
        Label notesLabel = new Label("বিবরণ:");

        HBox buttons = new HBox(8);
        Button addBtn = new Button("যোগ করুন");
        Button updateBtn = new Button("হালনাগাদ");
        Button deleteBtn = new Button("মুছুন");
        buttons.getChildren().addAll(addBtn, updateBtn, deleteBtn);

        inputBox.getChildren().addAll(title,
                nameLabel, nameField,
                dobLabel, dobPicker,
                notesLabel, notesField,
                buttons);

        // Table
        table = new TableView<>();
        table.setPrefWidth(600);
        TableColumn<BirthdayRow, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(40);

        TableColumn<BirthdayRow, String> nameCol = new TableColumn<>("নাম");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<BirthdayRow, String> dobCol = new TableColumn<>("জন্ম তারিখ");
        dobCol.setCellValueFactory(new PropertyValueFactory<>("dobStr"));
        dobCol.setPrefWidth(120);

        TableColumn<BirthdayRow, String> nextCol = new TableColumn<>("আগামী জন্মদিন");
        nextCol.setCellValueFactory(new PropertyValueFactory<>("nextBirthdayStr"));
        nextCol.setPrefWidth(150);

        TableColumn<BirthdayRow, String> notesCol = new TableColumn<>("বিবরণ");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));
        notesCol.setPrefWidth(200);

        table.getColumns().addAll(idCol, nameCol, dobCol, nextCol, notesCol);
        table.setItems(tableData);

        // Search / filters
        HBox searchBox = new HBox(8);
        searchBox.setPadding(new Insets(8));
        searchBox.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("নাম দিয়ে খুঁজুন");
        Button searchBtn = new Button("খুঁজুন");

        monthFilter = new ComboBox<>();
        monthFilter.getItems().add("সব মাস");
        for (int i = 1; i <= 12; i++) monthFilter.getItems().add(String.format("%02d - %s", i, java.time.Month.of(i).name()));
        monthFilter.getSelectionModel().selectFirst();

        Button refreshBtn = new Button("তাজা করুন");
        Button upcomingBtn = new Button("আগামী জন্মদিন দেখুন");

        searchBox.getChildren().addAll(new Label("খুঁজুন:"), searchField, searchBtn, new Label("মাস:"), monthFilter, searchBtn, refreshBtn, upcomingBtn);
        // I accidentally added searchBtn twice in nodes — remove duplicate
        searchBox.getChildren().remove(5); // remove the second searchBtn added by mistake

        BorderPane root = new BorderPane();
        root.setLeft(inputBox);
        VBox centerBox = new VBox(6);
        centerBox.getChildren().addAll(searchBox, table);
        root.setCenter(centerBox);

        // Hook actions
        addBtn.setOnAction(e -> handleAdd());
        updateBtn.setOnAction(e -> handleUpdate());
        deleteBtn.setOnAction(e -> handleDelete());
        searchBtn.setOnAction(e -> handleSearch());
        refreshBtn.setOnAction(e -> loadAll());
        upcomingBtn.setOnAction(e -> showUpcomingDialog());

        table.setOnMouseClicked(e -> {
            BirthdayRow row = table.getSelectionModel().getSelectedItem();
            if (row != null) {
                nameField.setText(row.getName());
                dobPicker.setValue(row.getDob());
                notesField.setText(row.getNotes());
            }
        });

        Scene scene = new Scene(root, 980, 500);
        stage.setScene(scene);
        stage.show();

        // initial load & today's notifications
        loadAll();
        Platform.runLater(this::showTodayNotifications);
    }

    private void handleAdd() {
        try {
            String name = nameField.getText().trim();
            LocalDate dob = dobPicker.getValue();
            String notes = notesField.getText().trim();
            if (name.isEmpty() || dob == null) {
                showAlert("ত্রুটি", "নাম ও জন্ম তারিখ আবশ্যক");
                return;
            }
            Birthday b = new Birthday(name, dob, notes);
            dao.add(b);
            clearInputs();
            loadAll();
        } catch (SQLException ex) {
            showAlert("ডাটাবেস ত্রুটি", ex.getMessage());
        }
    }

    private void handleUpdate() {
        BirthdayRow sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("নির্বাচন নেই", "আপডেট করার জন্য একটি রেকর্ড নির্বাচন করুন"); return; }
        try {
            String name = nameField.getText().trim();
            LocalDate dob = dobPicker.getValue();
            String notes = notesField.getText().trim();
            if (name.isEmpty() || dob == null) { showAlert("ত্রুটি", "নাম ও জন্ম তারিখ আবশ্যক"); return; }
            Birthday b = new Birthday(sel.getId(), name, dob, notes);
            dao.update(b);
            clearInputs();
            loadAll();
        } catch (SQLException ex) {
            showAlert("ডাটাবেস ত্রুটি", ex.getMessage());
        }
    }

    private void handleDelete() {
        BirthdayRow sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("নির্বাচন নেই", "মুছার জন্য একটি রেকর্ড নির্বাচন করুন"); return; }
        if (!confirm("নির্দিষ্টকরণ", "আপনি নিশ্চিতভাবে মুছতে চান?")) return;
        try {
            dao.delete(sel.getId());
            loadAll();
        } catch (SQLException ex) {
            showAlert("ডাটাবেস ত্রুটি", ex.getMessage());
        }
    }

    private void handleSearch() {
        String q = searchField.getText().trim();
        String monthSel = monthFilter.getSelectionModel().getSelectedItem();
        try {
            List<Birthday> results;
            if (!q.isEmpty()) {
                results = dao.searchByName(q);
            } else if (monthSel != null && !monthSel.equals("সব মাস")) {
                int m = Integer.parseInt(monthSel.split(" - ")[0]);
                results = dao.searchByMonth(m);
            } else {
                results = dao.findAll();
            }
            loadIntoTable(results);
        } catch (SQLException ex) {
            showAlert("ডাটাবেস ত্রুটি", ex.getMessage());
        }
    }

    private void loadAll() {
        try {
            List<Birthday> all = dao.findAll();
            loadIntoTable(all);
        } catch (SQLException ex) {
            showAlert("ডাটাবেস ত্রুটি", ex.getMessage());
        }
    }

    private void loadIntoTable(List<Birthday> list) {
        List<BirthdayRow> rows = list.stream()
                .map(BirthdayRow::fromBirthday)
                .sorted(Comparator.comparing(BirthdayRow::getNextBirthday))
                .collect(Collectors.toList());
        tableData.setAll(rows);
    }

    private void showUpcomingDialog() {
        List<BirthdayRow> rows = new ArrayList<>(tableData);
        if (rows.isEmpty()) {
            showAlert("তথ্য নেই", "কোনও রেকর্ড পাওয়া যায়নি");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (BirthdayRow r : rows) {
            sb.append(r.getName())
              .append(" — ").append(r.getNextBirthdayStr())
              .append(" (বয়স: ").append(r.getUpcomingAge()).append(")").append("\n");
        }
        showInfo("আগামী জন্মদিন", sb.toString());
    }

    private void showTodayNotifications() {
        LocalDate today = LocalDate.now();
        List<BirthdayRow> todays = tableData.stream()
                .filter(r -> MonthDay.from(r.getDob()).equals(MonthDay.from(today)))
                .collect(Collectors.toList());
        if (!todays.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (BirthdayRow r : todays) {
                sb.append(r.getName()).append(" — আজ জন্মদিন!\n");
            }
            showInfo("আজ জন্মদিন", sb.toString());
        }
    }

    private void clearInputs() {
        nameField.clear();
        dobPicker.setValue(null);
        notesField.clear();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        Optional<ButtonType> res = a.showAndWait();
        return res.isPresent() && res.get() == ButtonType.OK;
    }

    // Helper row class for table & computed fields
    public static class BirthdayRow {
        private int id;
        private String name;
        private LocalDate dob;
        private String notes;

        public BirthdayRow(int id, String name, LocalDate dob, String notes) {
            this.id = id; this.name = name; this.dob = dob; this.notes = notes;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        public LocalDate getDob() { return dob; }
        public String getDobStr() { return dob.toString(); }
        public String getNotes() { return notes == null ? "" : notes; }

        // next birthday date (this or next year)
        public LocalDate getNextBirthday() {
            LocalDate today = LocalDate.now();
            LocalDate next = dob.withYear(today.getYear());
            if (next.isBefore(today) || next.isEqual(today) && false) { // keep birthdays today as today (not next year)
                // no change; keep it if it equals today — for sorting today will come first anyway
            }
            if (next.isBefore(today)) next = next.plusYears(1);
            return next;
        }
        public String getNextBirthdayStr() {
            LocalDate nd = getNextBirthday();
            return nd.toString();
        }
        public int getUpcomingAge() {
            LocalDate nd = getNextBirthday();
            return nd.getYear() - dob.getYear();
        }

        public static BirthdayRow fromBirthday(Birthday b) {
            return new BirthdayRow(b.getId(), b.getName(), b.getDob(), b.getNotes());
        }
    }
}
