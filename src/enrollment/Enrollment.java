package enrollment;

public class Enrollment {

    // Encapsulation - private attributes
    private int id;
    private String studentId;
    private String subjectCode;
    private String dateEnrolled;

    // Default constructor
    public Enrollment() {}

    // Parameterized constructor
    public Enrollment(int id, String studentId, String subjectCode, String dateEnrolled) {
        this.id = id;
        this.studentId = studentId;
        this.subjectCode = subjectCode;
        this.dateEnrolled = dateEnrolled;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getDateEnrolled() { return dateEnrolled; }
    public void setDateEnrolled(String dateEnrolled) { this.dateEnrolled = dateEnrolled; }

    @Override
    public String toString() {
        return "Student ID: " + studentId + " | Subject: " + subjectCode +
               " | Date: " + dateEnrolled;
    }
}