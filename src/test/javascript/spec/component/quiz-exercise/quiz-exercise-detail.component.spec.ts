import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { ArtemisTestModule } from '../../test.module';
import { QuizExerciseDetailComponent } from 'app/exercises/quiz/manage/quiz-exercise-detail.component';
import { QuizExercise } from 'app/entities/quiz/quiz-exercise.model';
import { CourseExerciseService } from 'app/course/manage/course-management.service';
import { Course } from 'app/entities/course.model';
import { MockSyncStorage } from '../../helpers/mocks/service/mock-sync-storage.service';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { MockTranslateService } from '../../helpers/mocks/service/mock-translate.service';
import { TranslateService } from '@ngx-translate/core';

describe('QuizExercise Management Detail Component', () => {
    let service: CourseExerciseService;
    let comp: QuizExerciseDetailComponent;
    let fixture: ComponentFixture<QuizExerciseDetailComponent>;
    const course: Course = { id: 123 } as Course;
    const quizExercise = new QuizExercise(course);
    quizExercise.id = 456;
    const route = ({ snapshot: { paramMap: convertToParamMap({ courseId: course.id }) } } as any) as ActivatedRoute;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ArtemisTestModule],
            declarations: [QuizExerciseDetailComponent],
            providers: [
                { provide: ActivatedRoute, useValue: route },
                { provide: LocalStorageService, useClass: MockSyncStorage },
                { provide: SessionStorageService, useClass: MockSyncStorage },
                { provide: TranslateService, useClass: MockTranslateService },
            ],
        })
            .overrideTemplate(QuizExerciseDetailComponent, '')
            .compileComponents();
        fixture = TestBed.createComponent(QuizExerciseDetailComponent);
        comp = fixture.componentInstance;
        service = fixture.debugElement.injector.get(CourseExerciseService);
    });

    describe('OnInit', () => {
        it('Should call loadExercises on init', () => {
            // GIVEN

            // WHEN
            comp.course = course;
            comp.ngOnInit();

            // THEN
        });
    });
});
