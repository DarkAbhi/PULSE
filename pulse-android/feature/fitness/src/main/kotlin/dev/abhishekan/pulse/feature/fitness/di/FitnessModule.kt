package dev.abhishekan.pulse.feature.fitness.di
import dev.abhishekan.pulse.core.domain.usecase.DeleteExercise
import dev.abhishekan.pulse.core.domain.usecase.DeleteWorkout
import dev.abhishekan.pulse.core.domain.usecase.GetFitnessOverview
import dev.abhishekan.pulse.core.domain.usecase.GetWorkoutDetail
import dev.abhishekan.pulse.core.domain.usecase.RecordWorkout
import dev.abhishekan.pulse.core.domain.usecase.SaveExercise
import dev.abhishekan.pulse.core.domain.usecase.SearchExercises
import dev.abhishekan.pulse.feature.fitness.FitnessViewModel
import dev.abhishekan.pulse.feature.fitness.WorkoutViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val fitnessModule =
    module {
        factoryOf(::GetFitnessOverview)
        factoryOf(::GetWorkoutDetail)
        factoryOf(::RecordWorkout)
        factoryOf(::SearchExercises)
        factoryOf(::SaveExercise)
        factoryOf(::DeleteWorkout)
        factoryOf(::DeleteExercise)
        viewModelOf(::FitnessViewModel)
        viewModelOf(::WorkoutViewModel)
    }
