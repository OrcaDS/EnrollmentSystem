package enrollment;

public class Subject {

    // Encapsulation - private attributes
    private int id;
    private String subjectCode;
    private String subjectName;
    private int units;
    private String schedule;

    // Default constructor
    public Subject() {}

    // Parameterized constructor
    public Subject(int id, String subjectCode, String subjectName,
                   int units, String schedule) {
        this.id = id;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.units = units;
        this.schedule = schedule;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public int getUnits() { return units; }
    public void setUnits(int units) { this.units = units; }

    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    @Override
    public String toString() {
        return subjectCode + " - " + subjectName + " (" + units + " units) | " + schedule;
    }
}