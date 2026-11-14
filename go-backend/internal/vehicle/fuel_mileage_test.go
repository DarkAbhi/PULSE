package vehicle

import (
	"math"
	"testing"
)

func TestCalculateAverageFuelEconomies(t *testing.T) {
	tests := []struct {
		name     string
		entries  []fuelMileageEntry
		expected map[string]float64
	}{
		{
			name: "uses the first recorded high odometer as the baseline",
			entries: []fuelMileageEntry{
				{FuelType: "petrol", FillType: "full", OdometerKM: 50000, Quantity: 45},
				{FuelType: "petrol", FillType: "full", OdometerKM: 50500, Quantity: 40},
			},
			expected: map[string]float64{"petrol": 12.5},
		},
		{
			name: "weights multiple intervals by fuel used",
			entries: []fuelMileageEntry{
				{FuelType: "petrol", FillType: "full", OdometerKM: 10000, Quantity: 40},
				{FuelType: "petrol", FillType: "full", OdometerKM: 10100, Quantity: 10},
				{FuelType: "petrol", FillType: "full", OdometerKM: 10400, Quantity: 20},
			},
			expected: map[string]float64{"petrol": 400.0 / 30.0},
		},
		{
			name: "includes partial fills after a full tank",
			entries: []fuelMileageEntry{
				{FuelType: "petrol", FillType: "full", OdometerKM: 50000, Quantity: 45},
				{FuelType: "petrol", FillType: "partial", OdometerKM: 50200, Quantity: 10},
				{FuelType: "petrol", FillType: "full", OdometerKM: 50500, Quantity: 20},
			},
			expected: map[string]float64{"petrol": 500.0 / 30.0},
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
			expected: map[string]float64{"petrol": 10},
		},
		{
			name: "calculates each fuel type independently",
			entries: []fuelMileageEntry{
				{FuelType: "petrol", FillType: "full", OdometerKM: 1000, Quantity: 20},
				{FuelType: "diesel", FillType: "full", OdometerKM: 1000, Quantity: 20},
				{FuelType: "petrol", FillType: "full", OdometerKM: 1200, Quantity: 20},
				{FuelType: "diesel", FillType: "full", OdometerKM: 1300, Quantity: 20},
			},
			expected: map[string]float64{"petrol": 10, "diesel": 15},
		},
		{
			name: "does not calculate mileage without two full tanks",
			entries: []fuelMileageEntry{
				{FuelType: "petrol", FillType: "partial", OdometerKM: 1000, Quantity: 10},
				{FuelType: "petrol", FillType: "full", OdometerKM: 1100, Quantity: 20},
			},
			expected: map[string]float64{},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := map[string]float64{}
			for fuelType, stat := range calculateFuelEfficiency(tt.entries) {
				if stat.AverageKMPerLitre != nil {
					got[fuelType] = *stat.AverageKMPerLitre
				}
			}
			if len(got) != len(tt.expected) {
				t.Fatalf("got %v, want %v", got, tt.expected)
			}
			for fuelType, expected := range tt.expected {
				if gotValue, ok := got[fuelType]; !ok || math.Abs(gotValue-expected) > 0.000001 {
					t.Errorf("%s: got %v, want %v", fuelType, gotValue, expected)
				}
			}
		})
	}
}

func TestCalculateFuelEfficiency(t *testing.T) {
	tests := []struct {
		name    string
		entries []fuelMileageEntry
		want    map[string]fuelEfficiencyStats
	}{
		{
			name: "empty history",
			want: map[string]fuelEfficiencyStats{},
		},
		{
			name:    "single fill has totals but no efficiency",
			entries: []fuelMileageEntry{{FuelType: "petrol", FillType: "full", OdometerKM: 50000, Quantity: 40, TotalCost: 4000}},
			want:    map[string]fuelEfficiencyStats{"petrol": {TotalCost: 4000, TotalVolume: 40}},
		},
		{
			name: "weighted average and extrema include partial fills but not trailing partial",
			entries: []fuelMileageEntry{
				{FuelType: "petrol", FillType: "full", OdometerKM: 50000, Quantity: 40, TotalCost: 4000},
				{FuelType: "petrol", FillType: "full", OdometerKM: 50100, Quantity: 10, TotalCost: 1000},
				{FuelType: "petrol", FillType: "partial", OdometerKM: 50200, Quantity: 5, TotalCost: 500},
				{FuelType: "petrol", FillType: "full", OdometerKM: 50400, Quantity: 15, TotalCost: 1500},
				{FuelType: "petrol", FillType: "partial", OdometerKM: 50500, Quantity: 5, TotalCost: 500},
			},
			want: map[string]fuelEfficiencyStats{"petrol": {TotalCost: 7500, TotalVolume: 75, AverageKMPerLitre: floatPtr(400.0 / 30), MaxKMPerLitre: floatPtr(15), MinKMPerLitre: floatPtr(10), LastKMPerLitre: floatPtr(15)}},
		},
		{
			name: "missed fill breaks interval and fuels stay independent",
			entries: []fuelMileageEntry{
				{FuelType: "petrol", FillType: "full", OdometerKM: 1000, Quantity: 10, TotalCost: 1000},
				{FuelType: "diesel", FillType: "full", OdometerKM: 1000, Quantity: 10, TotalCost: 900},
				{FuelType: "petrol", FillType: "full", OdometerKM: 1200, Quantity: 10, TotalCost: 1000},
				{FuelType: "petrol", FillType: "missed", OdometerKM: 1300, Quantity: 10, TotalCost: 1000},
				{FuelType: "petrol", FillType: "full", OdometerKM: 1500, Quantity: 10, TotalCost: 1000},
				{FuelType: "petrol", FillType: "full", OdometerKM: 1600, Quantity: 10, TotalCost: 1000},
				{FuelType: "diesel", FillType: "full", OdometerKM: 1600, Quantity: 20, TotalCost: 1800},
			},
			want: map[string]fuelEfficiencyStats{
				"petrol": {TotalCost: 5000, TotalVolume: 50, AverageKMPerLitre: floatPtr(15), MaxKMPerLitre: floatPtr(20), MinKMPerLitre: floatPtr(10), LastKMPerLitre: floatPtr(10)},
				"diesel": {TotalCost: 2700, TotalVolume: 30, AverageKMPerLitre: floatPtr(30), MaxKMPerLitre: floatPtr(30), MinKMPerLitre: floatPtr(30), LastKMPerLitre: floatPtr(30)},
			},
		},
		{
			name: "equal or decreasing odometer does not create efficiency",
			entries: []fuelMileageEntry{
				{FuelType: "petrol", FillType: "full", OdometerKM: 1000, Quantity: 10},
				{FuelType: "petrol", FillType: "full", OdometerKM: 1000, Quantity: 10},
				{FuelType: "petrol", FillType: "full", OdometerKM: 900, Quantity: 10},
			},
			want: map[string]fuelEfficiencyStats{"petrol": {TotalVolume: 30}},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := calculateFuelEfficiency(tt.entries)
			if len(got) != len(tt.want) {
				t.Fatalf("got %v, want %v", got, tt.want)
			}
			for fuel, want := range tt.want {
				stat, ok := got[fuel]
				if !ok || stat.TotalCost != want.TotalCost || stat.TotalVolume != want.TotalVolume {
					t.Fatalf("%s totals: got %+v, want %+v", fuel, stat, want)
				}
				for name, pair := range map[string][2]*float64{
					"average": {stat.AverageKMPerLitre, want.AverageKMPerLitre},
					"max":     {stat.MaxKMPerLitre, want.MaxKMPerLitre},
					"min":     {stat.MinKMPerLitre, want.MinKMPerLitre},
					"last":    {stat.LastKMPerLitre, want.LastKMPerLitre},
				} {
					if (pair[0] == nil) != (pair[1] == nil) {
						t.Errorf("%s %s: got %v, want %v", fuel, name, pair[0], pair[1])
					} else if pair[0] != nil && math.Abs(*pair[0]-*pair[1]) > 0.000001 {
						t.Errorf("%s %s: got %v, want %v", fuel, name, *pair[0], *pair[1])
					}
				}
			}
		})
	}
}
