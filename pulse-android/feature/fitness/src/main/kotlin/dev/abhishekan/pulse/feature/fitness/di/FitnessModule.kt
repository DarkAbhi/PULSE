package dev.abhishekan.pulse.feature.fitness.di

import dev.abhishekan.pulse.core.domain.usecase.*
import dev.abhishekan.pulse.feature.fitness.FitnessViewModel
import dev.abhishekan.pulse.feature.fitness.WorkoutViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val fitnessModule = module {
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
