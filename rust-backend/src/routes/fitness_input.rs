//! Numeric bounds shared by the flat and nested fitness import contracts.

/// Largest value stored in the shared NUMERIC(8,2) calorie columns.
pub(super) const MAX_CALORIES: f64 = 999_999.99;
/// Largest value stored in the NUMERIC(10,3) workout duration column.
pub(super) const MAX_DURATION: f64 = 9_999_999.999;
/// Largest value stored in the NUMERIC(10,2) workout distance column.
pub(super) const MAX_DISTANCE: f64 = 99_999_999.99;

pub(super) fn number(value: f64, max: f64) -> Option<f64> {
    (value.is_finite() && value >= 0.0 && value <= max).then_some(value)
}

#[expect(
    clippy::cast_possible_truncation,
    reason = "finite integral value is checked against the i32 range before casting"
)]
pub(super) fn integer(value: f64) -> Option<i32> {
    let n = number(value, f64::from(i32::MAX))?;
    (n.fract() == 0.0).then_some(n as i32)
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn bounds_preserve_whole_floats_and_reject_overflows() {
        for value in [json!(0), json!(7500.0), json!(i32::MAX)] {
            assert!(integer(value.as_f64().unwrap()).is_some(), "input {value}");
        }
        for value in [
            json!(-1),
            json!(0.5),
            json!(2_147_483_648_i64),
            json!(9_007_199_254_740_993_i64),
        ] {
            assert_eq!(integer(value.as_f64().unwrap()), None, "input {value}");
        }
        for (max, overflow) in [
            (MAX_CALORIES, 1_000_000.0),
            (MAX_DURATION, 10_000_000.0),
            (MAX_DISTANCE, 100_000_000.0),
        ] {
            assert!(number(max, max).is_some(), "limit {max}");
            assert_eq!(number(overflow, max), None, "limit {max}");
        }
    }
}
