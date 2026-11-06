package vehicle

import (
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/google/uuid"
)

func TestMaintenanceAttachmentFileName(t *testing.T) {
	t.Parallel()
	uploadedAt := time.Date(2026, 10, 5, 14, 35, 42, 7000000, time.FixedZone("IST", 5*3600+30*60))
	tests := []struct {
		name         string
		originalName string
		extension    string
	}{
		{name: "pdf bill", originalName: "service bill.pdf", extension: ".pdf"},
		{name: "uppercase image extension", originalName: "receipt.JPG", extension: ".JPG"},
		{name: "no extension", originalName: "receipt"},
		{name: "multiple dots", originalName: "service.2026.10.pdf", extension: ".pdf"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()
			got := maintenanceAttachmentFileName(tt.originalName, uploadedAt)
			prefix := "20261005_090542_007_"
			if !strings.HasPrefix(got, prefix) {
				t.Fatalf("filename %q must start with UTC timestamp %q", got, prefix)
			}
			if filepath.Ext(got) != tt.extension {
				t.Fatalf("filename %q must preserve extension %q", got, tt.extension)
			}
			id, err := uuid.Parse(strings.TrimSuffix(strings.TrimPrefix(got, prefix), tt.extension))
			if err != nil {
				t.Fatalf("filename %q must contain a UUID: %v", got, err)
			}
			if id.Version() != 4 {
				t.Errorf("UUID version = %d, want 4", id.Version())
			}
		})
	}
}

func TestMaintenanceAttachmentFileNameRepeatedUploads(t *testing.T) {
	t.Parallel()
	uploadedAt := time.Date(2026, 10, 5, 9, 5, 42, 0, time.UTC)
	seen := make(map[string]bool)
	for range 100 {
		name := maintenanceAttachmentFileName("receipt.pdf", uploadedAt)
		if seen[name] {
			t.Fatalf("uploads with the same filename and timestamp generated duplicate %q", name)
		}
		seen[name] = true
	}
}
