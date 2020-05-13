import { TutorLeaderboardElement } from 'app/shared/dashboards/tutor-leaderboard/tutor-leaderboard.model';
import {DueDateStat} from "app/course/dashboards/instructor-course-dashboard/due-date-stat.model";

export class StatsForDashboard {
    public numberOfStudents = 0;
    public numberOfSubmissions = new DueDateStat();
    public numberOfAssessments = new DueDateStat();
    public numberOfAutomaticAssistedAssessments = 0;
    public numberOfComplaints = 0;
    public numberOfOpenComplaints = 0;
    public numberOfMoreFeedbackRequests = 0;
    public numberOfOpenMoreFeedbackRequests = 0;

    public tutorLeaderboardEntries: TutorLeaderboardElement[] = [];

    constructor() {}

    static from(object: StatsForDashboard): StatsForDashboard {
        let stats = Object.assign(new StatsForDashboard(), object);
        stats.numberOfSubmissions = Object.assign(new DueDateStat(), stats.numberOfSubmissions);
        stats.numberOfAssessments = Object.assign(new DueDateStat(), stats.numberOfAssessments);
        return stats;
    }

}
