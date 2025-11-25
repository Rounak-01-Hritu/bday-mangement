// Birthday.java
import java.time.LocalDate;

public class Birthday {
    private int id;
    private String name;
    private LocalDate dob;
    private String notes;

    public Birthday() {}

    public Birthday(int id, String name, LocalDate dob, String notes) {
        this.id = id;
        this.name = name;
        this.dob = dob;
        this.notes = notes;
    }

    public Birthday(String name, LocalDate dob, String notes) {
        this.name = name;
        this.dob = dob;
        this.notes = notes;
    }

    // getters & setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
