package enrollment;

public abstract class Student {

    // Encapsulation - private attributes
    private String studentId;
    private String name;
    private String course;
    private int yearLevel;
    private String contact;

    // Default constructor
    public Student() {}

    // Parameterized constructor
    public Student(String studentId, String name, String course, int yearLevel, String contact) {
        this.studentId = studentId;
        this.name = name;
        this.course = course;
        this.yearLevel = yearLevel;
        this.contact = contact;
    }

    // Getters and Setters
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public int getYearLevel() { return yearLevel; }
    public void setYearLevel(int yearLevel) { this.yearLevel = yearLevel; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    // Abstract method - must be implemented by subclasses (Abstraction)
    public abstract String getStudentType();

    // Method overriding will happen in RegularStudent (Polymorphism)
    public String getDisplayInfo() {
        return "ID: " + studentId + " | Name: " + name +
               " | Course: " + course + " | Year: " + yearLevel +
               " | Contact: " + contact;
    }
}