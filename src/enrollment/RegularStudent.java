package enrollment;

public class RegularStudent extends Student {

    // Default constructor
    public RegularStudent() {
        super();
    }

    // Parameterized constructor - calls parent constructor (Inheritance)
    public RegularStudent(String studentId, String name, String course,
                          int yearLevel, String contact) {
        super(studentId, name, course, yearLevel, contact);
    }

    // Implementing abstract method (Abstraction)
    @Override
    public String getStudentType() {
        return "Regular Student";
    }

    // Overriding parent method (Polymorphism)
    @Override
    public String getDisplayInfo() {
        return "[" + getStudentType() + "] " + super.getDisplayInfo();
    }
}