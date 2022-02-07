package org.juhrig.classroom.autograder.grader;

import org.juhrig.classroom.autograder.dto.AutograderStudent;

import java.util.Map;

public class GradingResults {

    private AutograderStudent student;
    private Double totalScore;
    private Map<String, Double> gradesByAssignment;
    private Map<String, Double> maxPointsByAssignment;
    private Map<String, Boolean> assignmentTurnedIn;
    private Map<String, Boolean> assignmentOnTime;
    private Map<String, String> commentsByAssignment;


    public GradingResults(AutograderStudent student,
                          Map<String, Double> gradesByAssignment,
                          Map<String, Double> maxPointsByAssignment,
                          Map<String ,Boolean> assignmentTurnedIn,
                          Map<String, Boolean> assignmentOnTime,
                          Map<String, String> commentsByAssignment){
        this.student = student;
        this.gradesByAssignment = gradesByAssignment;
        this.maxPointsByAssignment = maxPointsByAssignment;
        this.assignmentTurnedIn = assignmentTurnedIn;
        this.assignmentOnTime = assignmentOnTime;
        this.commentsByAssignment = commentsByAssignment;
        totalScore = 0.0;

        for(String key: gradesByAssignment.keySet()){
            Double assignmentGrade = gradesByAssignment.get(key);
            if(assignmentGrade != null && Double.isFinite(assignmentGrade)){
                totalScore += assignmentGrade;
            }
        }
    }

    public AutograderStudent getStudent() {
        return student;
    }

    public Map<String, Double> getGradesByAssignment() {
        return gradesByAssignment;
    }

    public Map<String, Double> getMaxPointsByAssignment() {
        return maxPointsByAssignment;
    }

    public Map<String, Boolean> getAssignmentTurnedIn() {
        return assignmentTurnedIn;
    }

    public Map<String, Boolean> getAssignmentOnTime() {
        return assignmentOnTime;
    }

    public Map<String, String> getCommentsByAssignment() {
        return commentsByAssignment;
    }

    public Double getTotalScore() {
        return totalScore;
    }
}
