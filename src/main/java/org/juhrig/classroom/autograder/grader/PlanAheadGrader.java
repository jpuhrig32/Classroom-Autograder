package org.juhrig.classroom.autograder.grader;

import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.ListCourseWorkResponse;
import com.google.api.services.classroom.model.ListStudentsResponse;
import com.google.api.services.classroom.model.Student;
import org.juhrig.classroom.autograder.dto.AutograderAssignment;
import org.juhrig.classroom.autograder.dto.AutograderStudent;
import org.juhrig.classroom.autograder.output.ExcelWriter;
import org.juhrig.classroom.autograder.parameters.InitializationParameters;
import org.juhrig.classroom.autograder.services.GoogleAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlanAheadGrader {

    private final Classroom classroomService;
    private final String courseId;
    private String outputFileName;
    private static final Logger LOG = LoggerFactory.getLogger(PlanAheadGrader.class);
    private static final int PAGE_SIZE = 100;
    private static final String FRIENDLY_TITLE_REGEX = "Turn in (?<title>[A-Za-z/ ]+) Assignments Here";
    private static final Pattern FRIENDLY_TITLE_PATTERN = Pattern.compile(FRIENDLY_TITLE_REGEX);
    private final Double passingScore;
    private boolean testMode = false;
    private final Map<String, Double> maxPointsByAssignment;

    public PlanAheadGrader(String courseID, InitializationParameters initializationParameters){
       this.classroomService = GoogleAuthenticationService.getClassroomService();
       this.courseId = courseID;
       this.outputFileName = initializationParameters.getPropertyOrDefault("planahead.output.file.path", "outputFiles/grades.xlsx");
       this.passingScore = Double.parseDouble(initializationParameters.getPropertyOrDefault("planahead.grades.passingscore", "200"));
       String maxScoresBySubject = initializationParameters.getProperty("planahead.grades.maxpointsbysubject");
       maxPointsByAssignment = processMaxScores(maxScoresBySubject);
    }

    private Map<String, Double> processMaxScores(String maxScoresByClass){
        String[] subjects = maxScoresByClass.split(",");
        Map<String, Double> maxScores = new LinkedHashMap<>();
        for(int i =0; i < subjects.length; i++){
            String[] subjectScoreParts = subjects[i].trim().split(":");
            maxScores.put(subjectScoreParts[0], Double.parseDouble(subjectScoreParts[1]));
        }
        return maxScores;
    }

    public void gradePlanAheads(){
        List<AutograderAssignment> assignments = listAssignments();
        listAndGradeStudents(assignments);
    }

    private List<AutograderAssignment> listAssignments(){
        List<CourseWork> courseWork = listCourseWork();
        courseWork = courseWork.stream()
                .filter(cw -> cw.getTitle().contains("Turn in"))
                .collect(Collectors.toList());
        List<AutograderAssignment> assignments = courseWork.stream()
                .map(cw -> {
                    AutograderAssignment assignment = new AutograderAssignment(cw);
                    String shortTitle = getFriendlyTitle(assignment.getTitle());
                    assignment.setShortTitle(getFriendlyTitle(assignment.getTitle()));
                    assignment.setPoints(maxPointsByAssignment.get(shortTitle));
                    return assignment;
                })
                .collect(Collectors.toList());
        return assignments;
    }
    private List<CourseWork> listCourseWork(){
        List<CourseWork> courses = new ArrayList<>();
        try {
            Classroom.Courses.CourseWork.List courseWorks = classroomService.courses().courseWork().list(courseId);
            courseWorks.setPageSize(PAGE_SIZE);
            while(true){
                ListCourseWorkResponse coursesResponse = courseWorks.execute();
                courses.addAll(coursesResponse.getCourseWork());
                if(coursesResponse.getNextPageToken() != null){
                    courseWorks.setPageToken(coursesResponse.getNextPageToken());
                }
                else{
                    break;
                }
            }

        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
        }

        return courses;

    }
    private void constructHeader(ExcelWriter excelWriter, List<AutograderAssignment> assignments){
        List<String> commonOutputParts = List.of(
                "First Name",
                "Last Name",
                "Full Name",
                "User ID",
                "Passed?",
                "Score"
        );
        List<String> assignmentParts = List.of(
                "Grade",
                "Max Points",
                "Turned in?",
                "Late?",
                "Comments"
        );
        List<String> secondHeaderLine  = new ArrayList<>(commonOutputParts);
        List<String> commonOutputSpacer = commonOutputParts.stream().map(s -> "").collect(Collectors.toList());
        List<String> topLineHeader = new ArrayList<>(commonOutputSpacer);
        for(AutograderAssignment assignment: assignments){
            topLineHeader.add(assignment.getShortTitle());
            secondHeaderLine.addAll(assignmentParts);
            for(int i =0; i < assignmentParts.size()-1; i++){
                topLineHeader.add("");
            }
        }

        excelWriter.writeHeaderRow(topLineHeader);
        excelWriter.writeHeaderRow(secondHeaderLine);
    }

    private void writeStudentGrades(AutograderStudent student, List<AutograderAssignment> assignments, GradingResults results, ExcelWriter writer){
        boolean passed = results != null && (results.getTotalScore() >= passingScore);
        List<String> lineParts = new ArrayList(List.of(
                student.getFirstName(),
                student.getLastName(),
                student.getFullName(),
                student.getUserId(),
                booleanToYesNo(passed),
                (results != null ? Double.toString(results.getTotalScore()) : "N/A")
        ));
        if(results == null){
            for(AutograderAssignment assignment : assignments){
                List<String> resultByAssignment = List.of(
                        "N/A",
                        Double.toString(assignment.getPoints()),
                        booleanToYesNo(false),
                        "N/A",
                        "Nothing turned in, not graded"
                );
                lineParts.addAll(resultByAssignment);
            }
        }
        else{
            for(AutograderAssignment assignment : assignments){
                boolean turnedIn = results.getAssignmentTurnedIn().get(assignment.getShortTitle());
                String late = turnedIn ? booleanToYesNo(results.getAssignmentOnTime().get(assignment.getShortTitle())) : "N/A";
                Double points = results.getGradesByAssignment().get(assignment.getShortTitle());
                String comments =  results.getCommentsByAssignment().get(assignment.getShortTitle());
                List<String> resultsByAssignment = List.of(
                        Double.toString(points != null ? points : 0.0),
                        Double.toString(assignment.getPoints()!= null ? assignment.getPoints(): 0.0),
                        booleanToYesNo(turnedIn),
                        late,
                        comments != null ? comments : ""
                );
                lineParts.addAll(resultsByAssignment);
            }
        }
        if(passed){
            writer.writeRow(lineParts, writer.GREEN);
        }
        else{
            writer.writeRow(lineParts, writer.RED);
        }

    }

    private void writeStudentGrades(AutograderStudent student, List<AutograderAssignment> assignments, ExcelWriter writer){
        writeStudentGrades(student, assignments, null, writer);
    }

    private String booleanToYesNo(boolean yesNo){
        return yesNo ? "Yes" : "No";
    }
    public void listAndGradeStudents(List<AutograderAssignment> assignments){
        ExcelWriter excelWriter = null;
        try {
            Classroom.Courses.Students.List students = classroomService.courses().students().list(courseId);
            excelWriter = new ExcelWriter(outputFileName);
            constructHeader(excelWriter, assignments);
            Map<String, AutograderAssignment> assignmentMap = assignments.stream().collect(Collectors.toMap(assignment -> assignment.getShortTitle(), assignment -> assignment));
            while (true) {
                ListStudentsResponse response = students.execute();
                List<AutograderStudent> responseStudents = response.getStudents().stream().map(stu -> new AutograderStudent(stu)).collect(Collectors.toList());

                ExcelWriter finalExcelWriter = excelWriter;
                responseStudents.parallelStream().filter(student  -> student != null).forEach(student -> {
                    boolean fetchStudentSubmissions = student.loadStudentSubmissions(classroomService, courseId);
                    if (fetchStudentSubmissions) {
                        CourseGrader grader = new CourseGrader(student, assignmentMap, student.getStudentSubmissions());
                        GradingResults gradingResults = grader.gradeCourse();
                        writeStudentGrades(student, assignments, gradingResults, finalExcelWriter);
                    }
                    else{
                        writeStudentGrades(student, assignments, finalExcelWriter);
                    }
                });
                for (Student student : response.getStudents()) {
                    System.out.println("Name: " + student.getProfile().getName());
                }
                if(response.getNextPageToken() != null && !testMode){
                    students.setPageToken(response.getNextPageToken());
                }
                else{
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally{
            if(excelWriter != null){
                excelWriter.close();
            }
        }

    }


    private String getFriendlyTitle(String title){
        Matcher matcher = FRIENDLY_TITLE_PATTERN.matcher(title);
        if(matcher.matches()){
            return matcher.group("title");
        }
        else{
            return title;
        }
    }

}
