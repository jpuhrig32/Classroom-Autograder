package org.juhrig.classroom.autograder;

import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.ListCoursesResponse;
import org.juhrig.classroom.autograder.grader.PlanAheadGrader;
import org.juhrig.classroom.autograder.parameters.InitializationParameters;
import org.juhrig.classroom.autograder.parameters.InitializationParametersImpl;
import org.juhrig.classroom.autograder.services.GoogleAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class ClassroomAutograder {
    private static final Logger LOG = LoggerFactory.getLogger(ClassroomAutograder.class);
    public static void main(String... args) throws IOException {
        InitializationParameters initializationParameters = new InitializationParametersImpl("src/main/resources/classroom.autograder.properties");
        //ClassroomCredentials classroomCredentials = new ClassroomCredentials(initializationParameters);
        GoogleAuthenticationService.initialize(initializationParameters);
        String planAheadCourseName = initializationParameters.getProperty("course.names");
        if(planAheadCourseName == null){
            LOG.error("No course name defined in classroom.autograder.properties");
            throw new RuntimeException("No course name defined in classroom.autograder.properties");
        }
        Classroom classroom = GoogleAuthenticationService.getClassroomService();
        ListCoursesResponse response = classroom.courses().list()
                .setPageSize(10)
                .execute();
        List<Course> courseList = response.getCourses();
        if(courseList == null || courseList.size() ==0){
            LOG.error("No courses found");
            System.out.println("No courses found");
        }
        else{
            System.out.println("Courses");
            String courseId = null;
            for(Course course: courseList){
                System.out.printf("%s\n", course.getName());
                if(course.getName().contains("Plan Ahead")){
                    courseId = course.getId();
                    break;
                }
            }
            System.out.println("======================================");
            if(courseId != null) {
                PlanAheadGrader planAheadGrader = new PlanAheadGrader(courseId, initializationParameters);
                planAheadGrader.gradePlanAheads();
            }

        }
    }
}
