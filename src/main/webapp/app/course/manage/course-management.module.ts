import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ArtemisTextExerciseModule } from 'app/exercises/text/manage/text-exercise/text-exercise.module';
import { ArtemisModelingExerciseModule } from 'app/exercises/modeling/manage/modeling-exercise.module';
import { ArtemisFileUploadExerciseManagementModule } from 'app/exercises/file-upload/manage/file-upload-exercise-management.module';

import { CourseExerciseCardComponent } from 'app/course/manage/course-exercise-card.component';
import { FormDateTimePickerModule } from 'app/shared/date-time-picker/date-time-picker.module';
import { ArtemisColorSelectorModule } from 'app/shared/color-selector/color-selector.module';
import { ImageCropperModule } from 'ngx-image-cropper';
import { MomentModule } from 'ngx-moment';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { SortByModule } from 'app/shared/pipes/sort-by.module';
import { CourseExercisesOverviewComponent } from 'app/course/manage/course-exercises-overview.component';
import { CourseDetailComponent } from 'app/course/manage/course-detail.component';
import { CourseUpdateComponent } from 'app/course/manage/course-update.component';
import { courseMangementRoute } from 'app/course/manage/course-mangement.route';
import { CourseManagementComponent } from 'app/course/manage/course-management.component';
import { OrionModule } from 'app/shared/orion/orion.module';
import { ArtemisProgrammingExerciseManagementModule } from 'app/exercises/programming/manage/programming-exercise-management.module';
import { ArtemisQuizManagementModule } from 'app/exercises/quiz/manage/quiz-management.module';
import { ArtemisProgrammingExerciseModule } from 'app/exercises/programming/shared/programming-exercise.module';
import { ArtemisExerciseModule } from 'app/exercises/shared/exercise/exercise.module';
import { ArtemisLectureModule } from 'app/lecture/lecture.module';
import { ArtemisTextExerciseManagementModule } from 'app/exercises/text/manage/text-exercise-management.module';
import { ArtemisDashboardsModule } from 'app/shared/dashboards/dashboards.module';
import { ArtemisParticipationModule } from 'app/exercises/shared/participation/participation.module';

const ENTITY_STATES = [...courseMangementRoute];

@NgModule({
    imports: [
        ArtemisSharedModule,
        RouterModule.forChild(ENTITY_STATES),
        SortByModule,
        FormDateTimePickerModule,
        ReactiveFormsModule,
        ImageCropperModule,
        OrionModule,
        MomentModule,
        ArtemisExerciseModule,
        ArtemisLectureModule,
        ArtemisProgrammingExerciseManagementModule,
        ArtemisFileUploadExerciseManagementModule,
        ArtemisQuizManagementModule,
        ArtemisTextExerciseManagementModule,
        ArtemisProgrammingExerciseModule,
        ArtemisTextExerciseModule,
        ArtemisModelingExerciseModule,
        ArtemisColorSelectorModule,
        ArtemisDashboardsModule,
        ArtemisParticipationModule,
    ],
    declarations: [CourseManagementComponent, CourseDetailComponent, CourseUpdateComponent, CourseExerciseCardComponent, CourseExercisesOverviewComponent],
})
export class ArtemisCourseManagementModule {}