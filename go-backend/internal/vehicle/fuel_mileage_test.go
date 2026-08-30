package vehicle

import (
	"math"
	"testing"
)

func TestCalculateAverageFuelEconomies(t *testing.T) {
	tests := []struct {
		name    string
		entries []fuelMileageEntry
		want    map[string]float64
	}{
		{
			name: "uses the first recorded high odometer as the baseline",
			entries: []fuelMileageEntry{
				{FuelType: "petrol", FillType: "full", OdometerKM: 50000, Quantity: 45},
				{FuelType: "petrol", FillType: "full", OdometerKM: 50500, Quantity: 40},
			},
			want: map[string]float64{"petrol": 12.5},
		},
		{
			name: "weights multiple intervals by fuel used",
			entries: []fuelMileageEntry{
				{FuelType: "petrol", FillType: "full", OdometerKM: 10000, Quantity: 40},
				{FuelType: "petrol", FillType: "full", OdometerKM: 10100, Quantity: 10},
				{FuelType: "petrol", FillType: "full", OdometerKM: 10400, Quantity: 20},
			},
			want: map[string]float64{"petrol": 400.0 / 30.0},
		},
		{
			name: "includes partial fills after a full tank",
			entries: []fuelMileageEntry{
				{FuelType: "petrol", FillType: "full", OdometerKM: 50000, Quantity: 45},
				{FuelType: "petrol", FillType: "partial", OdometerKM: 50200, Quantity: 10},
				{FuelType: "petrol", FillType: "full", OdometerKM: 50500, Quantity: 20},
			},
			want: map[string]float64{"petrol": 500.0 / 30.0},
		},
		{
			name: "resets after a missed fill",
			entries: []fuelMileageEntry{
				{FuelType: "petrol", FillType: "full", OdometerKM: 1000, Quantity: 20},
				{FuelType: "petrol", FillType: "full", OdometerKM: 1200, Quantity: 20},
				{FuelType: "petrol", FillType: "missed", OdometerKM: 1250, Quantity: 10},
				{FuelType: "petrol", FillType: "full", OdometerKM: 1500, Quantity: 25},
				{FuelType: "petrol", FillType: "full", OdometerKM: 1700, Quantity: 20},
			},
			want: map[string]float64{"petrol": 10},
		},
		{
			name: "calculates each fuel type independently",
			entries: []fuelMileageEntry{
				{FuelType: "petrol", FillType: "full", OdometerKM: 1000, Quantity: 20},
				{FuelType: "diesel", FillType: "full", OdometerKM: 1000, Quantity: 20},
				{FuelType: "petrol", FillType: "full", OdometerKM: 1200, Quantity: 20},
				{FuelType: "diesel", FillType: "full", OdometerKM: 1300, Quantity: 20},
			},
			want: map[string]float64{"petrol": 10, "diesel": 15},
		},
		{
			name: "does not calculate mileage without two full tanks",
			entries: []fuelMileageEntry{
				{FuelType: "petrol", FillType: "partial", OdometerKM: 1000, Quantity: 10},
				{FuelType: "petrol", FillType: "full", OdometerKM: 1100, Quantity: 20},
			},
			want: map[string]float64{},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := calculateAverageFuelEconomies(tt.entries)
			if len(got) != len(tt.want) {
				t.Fatalf("got %v, want %v", got, tt.want)
			}
			for fuelType, want := range tt.want {
				if gotValue, ok := got[fuelType]; !ok || math.Abs(gotValue-want) > 0.000001 {
					t.Errorf("%s: got %v, want %v", fuelType, gotValue, want)
				}
			}
		})
	}
}
