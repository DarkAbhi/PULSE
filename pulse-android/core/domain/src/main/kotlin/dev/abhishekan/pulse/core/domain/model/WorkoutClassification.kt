package dev.abhishekan.pulse.core.domain.model

import java.time.DayOfWeek

enum class WorkoutType(
    val apiValue: String,
) {
    SingleMuscle("single_muscle"),
    DoubleMuscle("double_muscle"),
    Push("push"),
    Pull("pull"),
    Legs("legs"),
    UpperBody("upper_body"),
    LowerBody("lower_body"),
    FullBody("full_body"),
    Cardio("cardio"),
    Mobility("mobility"),
    Other("other"),
    RestDay("rest_day"),
}

data class WorkoutClassification(
    val type: WorkoutType?,
    val focus: String = "",
) {
    val validFocus get() = focus.codePointCount(0, focus.length) <= 120 && '\u0000' !in focus
}

data class WorkoutScheduleDay(
    val day: DayOfWeek,
    val classification: WorkoutClassification,
)
