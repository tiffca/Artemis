package de.tum.in.www1.artemis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.security.Principal;
import java.time.ZonedDateTime;
import java.util.List;

import de.tum.in.www1.artemis.domain.SubmittedAnswer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.Result;
import de.tum.in.www1.artemis.domain.quiz.*;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.service.QuizExerciseService;
import de.tum.in.www1.artemis.service.scheduled.QuizScheduleService;
import de.tum.in.www1.artemis.util.DatabaseUtilService;
import de.tum.in.www1.artemis.util.RequestUtilService;
import de.tum.in.www1.artemis.web.websocket.QuizSubmissionWebsocketService;

public class QuizSubmissionIntegrationTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Autowired
    DatabaseUtilService database;

    @Autowired
    RequestUtilService request;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    ExerciseRepository exerciseRepository;

    @Autowired
    QuizExerciseService quizExerciseService;

    @Autowired
    QuizScheduleService quizScheduleService;

    @Autowired
    QuizSubmissionWebsocketService quizSubmissionWebsocketService;

    @Autowired
    QuizSubmissionRepository quizSubmissionRepository;

    @Autowired
    ParticipationRepository participationRepository;

    @Autowired
    SubmissionRepository submissionRepository;

    @Autowired
    ResultRepository resultRepository;

    @BeforeEach
    public void init() {
        quizScheduleService.stopSchedule();
        database.addUsers(10, 5, 1);
        // do not use the schedule service based on a time interval in the tests, because this would result in flaky tests that run much slower
    }

    @AfterEach
    public void tearDown() {
        quizScheduleService.clearAllQuizData();
        database.resetDatabase();
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void testQuizSubmit() throws Exception {
        // change config to make test faster
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now(), null);
        quizExercise.setDueDate(ZonedDateTime.now().plusSeconds(2));
        quizExercise.setDuration(2);
        quizExercise.setIsPlannedToStart(true);
        quizExercise.setIsVisibleBeforeStart(true);
        quizExerciseService.save(quizExercise);

        int numberOfParticipants = 10;

        for (int i = 1; i <= numberOfParticipants; i++) {
            QuizSubmission quizSubmission = new QuizSubmission();
            for (var question : quizExercise.getQuizQuestions()) {
                for (int j = 1; j <= 10; j++) {
                    quizSubmission.addSubmittedAnswers(database.generateSubmittedAnswerFor(question, j % 2 == 0));
                }
            }
            final var username = "student" + i;
            final Principal principal = () -> username;
            // save
            quizSubmissionWebsocketService.saveSubmission(quizExercise.getId(), quizSubmission, principal);
            verify(messagingTemplate, times(1)).convertAndSendToUser(username, "/topic/quizExercise/" + quizExercise.getId() + "/submission", quizSubmission);
        }

        for (int i = 1; i <= numberOfParticipants; i++) {
            QuizSubmission quizSubmission = new QuizSubmission();
            for (int j = 0; j < quizExercise.getQuizQuestions().size(); j++) {
                quizSubmission.addSubmittedAnswers(database.generateSubmittedAnswerFor(quizExercise.getQuizQuestions().get(j), (i + j) % 2 == 0));
            }

            quizSubmission.setSubmitted(true);
            final var username = "student" + i;
            final Principal principal = () -> username;
            // submit
            quizSubmissionWebsocketService.saveSubmission(quizExercise.getId(), quizSubmission, principal);
            verify(messagingTemplate, times(1)).convertAndSendToUser(username, "/topic/quizExercise/" + quizExercise.getId() + "/submission", quizSubmission);
        }

        // before the quiz submissions are processed, none of them ends up in the database
        assertThat(quizSubmissionRepository.findAll().size()).isEqualTo(0);

        quizScheduleService.processCachedQuizSubmissions();

        // after the quiz submissions have been processed, all submission are saved to the database
        assertThat(quizSubmissionRepository.findAll().size()).isEqualTo(numberOfParticipants);

        // Test the statistics directly from the database
        QuizExercise quizExerciseWithStatistic = quizExerciseService.findOneWithQuestionsAndStatistics(quizExercise.getId());
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getParticipantsUnrated()).isEqualTo(0);
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getParticipantsRated()).isEqualTo(numberOfParticipants);
        int questionScore = quizExerciseWithStatistic.getQuizQuestions().stream().map(QuizQuestion::getScore).reduce(0, Integer::sum);
        assertThat(quizExerciseWithStatistic.getMaxScore()).isEqualTo(questionScore);
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getPointCounters().size()).isEqualTo(questionScore + 1);
        for (var pointCounter : quizExerciseWithStatistic.getQuizPointStatistic().getPointCounters()) {

            if (pointCounter.getPoints() == 0.0) {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(5);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(0);
            }
            else if (pointCounter.getPoints() == 7.0) {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(5);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(0);
            }
            else {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(0);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(0);
            }
        }
        for (var question : quizExerciseWithStatistic.getQuizQuestions()) {
            if (question instanceof DragAndDropQuestion) {
                assertThat(question.getQuizQuestionStatistic().getRatedCorrectCounter()).isEqualTo(0);
            } else {
                assertThat(question.getQuizQuestionStatistic().getRatedCorrectCounter()).isEqualTo(5);
            }
            assertThat(question.getQuizQuestionStatistic().getUnRatedCorrectCounter()).isEqualTo(0);
            assertThat(question.getQuizQuestionStatistic().getParticipantsRated()).isEqualTo(numberOfParticipants);
            assertThat(question.getQuizQuestionStatistic().getParticipantsUnrated()).isEqualTo(0);
        }
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void testQuizSubmitPractice() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(false);
        Course course = courses.get(0);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExercise.setDueDate(ZonedDateTime.now().minusSeconds(2));
        quizExercise.setDuration(2);
        quizExercise.setIsPlannedToStart(true);
        quizExercise.setIsVisibleBeforeStart(true);
        quizExercise.setIsOpenForPractice(true);
        quizExerciseService.save(quizExercise);

        assertThat(quizSubmissionRepository.findAll().size()).isEqualTo(0);
        assertThat(participationRepository.findAll().size()).isEqualTo(0);

        var numberOfParticipants = 10;


        // submit 10 times for 10 different students
        for (int i = 1; i <= numberOfParticipants; i++) {
            //generate some mixed submissions for each student
            QuizSubmission quizSubmission = new QuizSubmission();
            quizSubmission.addSubmittedAnswers(database.generateSubmittedAnswerFor(quizExercise.getQuizQuestions().get(0), i % 2 != 0));
            quizSubmission.addSubmittedAnswers(database.generateSubmittedAnswerFor(quizExercise.getQuizQuestions().get(1), i % 2 == 0));
            quizSubmission.addSubmittedAnswers(database.generateSubmittedAnswerFor(quizExercise.getQuizQuestions().get(2), i % 2 == 0));

            quizSubmission.setSubmitted(true);
            database.changeUser("student" + i);
            Result receivedResult = request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/submissions/practice", quizSubmission, Result.class, HttpStatus.OK);
            assertThat(((QuizSubmission) receivedResult.getSubmission()).getSubmittedAnswers().size()).isEqualTo(quizSubmission.getSubmittedAnswers().size());
        }

        // after the quiz has ended, all submission are saved to the database
        assertThat(quizSubmissionRepository.findAll().size()).isEqualTo(numberOfParticipants);
        assertThat(participationRepository.findAll().size()).isEqualTo(numberOfParticipants);

        // processing the quiz submissions will update the statistics
        quizScheduleService.processCachedQuizSubmissions();

        // Test the statistics directly from the database
        QuizExercise quizExerciseWithStatistic = quizExerciseService.findOneWithQuestionsAndStatistics(quizExercise.getId());
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getParticipantsRated()).isEqualTo(0);
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getParticipantsUnrated()).isEqualTo(numberOfParticipants);
        int questionScore = quizExerciseWithStatistic.getQuizQuestions().stream().map(QuizQuestion::getScore).reduce(0, Integer::sum);
        assertThat(quizExerciseWithStatistic.getMaxScore()).isEqualTo(questionScore);
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getPointCounters().size()).isEqualTo(questionScore + 1);
        for (var pointCounter : quizExerciseWithStatistic.getQuizPointStatistic().getPointCounters()) {
            if (pointCounter.getPoints() == 3.0) {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(0);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(5);
            }
            else if (pointCounter.getPoints() == 4.0) {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(0);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(5);
            }
            else {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(0);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(0);
            }
        }
        for (var question : quizExerciseWithStatistic.getQuizQuestions()) {
            if (question instanceof DragAndDropQuestion) {
                assertThat(question.getQuizQuestionStatistic().getUnRatedCorrectCounter()).isEqualTo(0);
            } else {
                assertThat(question.getQuizQuestionStatistic().getUnRatedCorrectCounter()).isEqualTo(5);
            }
            assertThat(question.getQuizQuestionStatistic().getRatedCorrectCounter()).isEqualTo(0);
            assertThat(question.getQuizQuestionStatistic().getParticipantsRated()).isEqualTo(0);
            assertThat(question.getQuizQuestionStatistic().getParticipantsUnrated()).isEqualTo(numberOfParticipants);
        }
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void testQuizSubmitPractice_badRequest() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        QuizExercise quizExerciseServer = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExerciseServer.setDueDate(ZonedDateTime.now().minusSeconds(2));
        quizExerciseServer.setDuration(2);
        quizExerciseServer.setIsPlannedToStart(true);
        quizExerciseServer.setIsVisibleBeforeStart(true);
        quizExerciseServer.setIsOpenForPractice(false);
        quizExerciseService.save(quizExerciseServer);

        assertThat(quizSubmissionRepository.findAll().size()).isEqualTo(0);

        QuizSubmission quizSubmission = new QuizSubmission();
        for (var question : quizExerciseServer.getQuizQuestions()) {
            for (int i = 1; i <= 10; i++) {
                quizSubmission.addSubmittedAnswers(database.generateSubmittedAnswerFor(question, i % 2 == 0));
            }
        }
        quizSubmission.setSubmitted(true);
        // quiz not open for practice --> bad request expected
        Result result = request.postWithResponseBody("/api/exercises/" + quizExerciseServer.getId() + "/submissions/practice", quizSubmission, Result.class,
                HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void testQuizSubmitPreview_forbidden() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExerciseService.save(quizExercise);
        request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/submissions/preview", new QuizSubmission(), Result.class, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(value = "student1", roles = "USER")
    public void testQuizSubmitPractice_forbidden() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        course.setStudentGroupName("abc");
        courseRepository.save(course);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExerciseService.save(quizExercise);
        request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/submissions/practice", new QuizSubmission(), Result.class, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(value = "tutor1", roles = "TA")
    public void testQuizSubmitPreview_forbidden_otherTa() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        course.setTeachingAssistantGroupName("tutor2");
        courseRepository.save(course);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExerciseService.save(quizExercise);
        request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/submissions/preview", new QuizSubmission(), Result.class, HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testQuizSubmitPreview_badRequest_noQuiz() throws Exception {
        request.postWithResponseBody("/api/exercises/" + 11 + "/submissions/preview", new QuizSubmission(), Result.class, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testQuizSubmitPractice_badRequest_noQuiz() throws Exception {
        request.postWithResponseBody("/api/exercises/" + 11 + "/submissions/practice", new QuizSubmission(), Result.class, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testQuizSubmitPreview_badRequest_submissionId() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExerciseService.save(quizExercise);
        var quizSubmission = new QuizSubmission();
        quizSubmission.setId(1L);
        request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/submissions/preview", quizSubmission, Result.class, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testQuizSubmitPractice_badRequest_submissionId() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExerciseService.save(quizExercise);
        var quizSubmission = new QuizSubmission();
        quizSubmission.setId(1L);
        request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/submissions/practice", quizSubmission, Result.class, HttpStatus.BAD_REQUEST);
    }

    @Test
    @WithMockUser(value = "instructor1", roles = "INSTRUCTOR")
    public void testQuizSubmitPreview() throws Exception {
        List<Course> courses = database.createCoursesWithExercisesAndLectures(true);
        Course course = courses.get(0);
        QuizExercise quizExercise = database.createQuiz(course, ZonedDateTime.now().minusSeconds(4), null);
        quizExerciseService.save(quizExercise);

        QuizSubmission quizSubmission = new QuizSubmission();
        for (var question : quizExercise.getQuizQuestions()) {
            for (int i = 1; i <= 10; i++) {
                quizSubmission.addSubmittedAnswers(database.generateSubmittedAnswerFor(question, i % 2 == 0));
            }
        }

        Result result = request.postWithResponseBody("/api/exercises/" + quizExercise.getId() + "/submissions/preview", quizSubmission, Result.class, HttpStatus.OK);
        // TODO: check the result

        // in the preview the submission will not be saved to the database
        assertThat(quizSubmissionRepository.findAll().size()).isEqualTo(0);

        quizScheduleService.processCachedQuizSubmissions();

        // all stats must be 0 because we have a preview here
        // Test the statistics directly from the database
        QuizExercise quizExerciseWithStatistic = quizExerciseService.findOneWithQuestionsAndStatistics(quizExercise.getId());
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getParticipantsRated()).isEqualTo(0);
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getParticipantsUnrated()).isEqualTo(0);
        int questionScore = quizExerciseWithStatistic.getQuizQuestions().stream().map(QuizQuestion::getScore).reduce(0, Integer::sum);
        assertThat(quizExerciseWithStatistic.getMaxScore()).isEqualTo(questionScore);
        assertThat(quizExerciseWithStatistic.getQuizPointStatistic().getPointCounters().size()).isEqualTo(questionScore + 1);
        for (var pointCounter : quizExerciseWithStatistic.getQuizPointStatistic().getPointCounters()) {
            if (pointCounter.getPoints() == 0.0f) {
                // all participants have 0 points (and are unrated)
                assertThat(pointCounter.getRatedCounter()).isEqualTo(0);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(0);
            }
            else {
                assertThat(pointCounter.getRatedCounter()).isEqualTo(0);
                assertThat(pointCounter.getUnRatedCounter()).isEqualTo(0);
            }
        }
        for (var question : quizExerciseWithStatistic.getQuizQuestions()) {
            assertThat(question.getQuizQuestionStatistic().getRatedCorrectCounter()).isEqualTo(0);
            assertThat(question.getQuizQuestionStatistic().getUnRatedCorrectCounter()).isEqualTo(0);
        }
        // TODO: check more statistics (e.g. for each question)
    }

}
