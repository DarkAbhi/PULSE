package statementimport

type failure struct {
	status  int
	code    string
	message string
}

var (
	unsupported          = &failure{422, "UNSUPPORTED_BANK", "This statement layout is not supported."}
	currencyMismatch     = &failure{422, "CURRENCY_MISMATCH", "The statement currency must match your configured currency."}
	accountMismatch      = &failure{422, "ACCOUNT_MISMATCH", "The statement does not match the selected account."}
	balancesUnavailable  = &failure{422, "BALANCES_UNAVAILABLE", "The statement balances could not be verified."}
	reconciliationFailed = &failure{422, "RECONCILIATION_FAILED", "The statement transactions do not match its balances."}
	invalidRow           = &failure{422, "INVALID_ROW", "A statement row could not be verified."}
)

var (
	invalidPDF         = &failure{422, "INVALID_PDF", "The PDF could not be read."}
	ocrFailed          = &failure{422, "OCR_FAILED", "Text could not be read from the scanned statement."}
	limitsExceeded     = &failure{422, "PDF_LIMIT_EXCEEDED", "The statement exceeds the PDF extraction limits."}
	badRequest         = &failure{400, "INVALID_REQUEST", "Check the import details and try again."}
	tooLarge           = &failure{413, "FILE_TOO_LARGE", "Choose a PDF up to 10 MiB."}
	notPDF             = &failure{415, "NOT_PDF", "Choose a valid PDF statement."}
	encrypted          = &failure{422, "ENCRYPTED_PDF_UNSUPPORTED", "Upload an unencrypted copy of the statement."}
	ambiguousDuplicate = &failure{422, "AMBIGUOUS_DUPLICATE", "Identical transactions overlap with a previous import and could not be distinguished."}
	archived           = &failure{409, "ACCOUNT_ARCHIVED", "Restore the account before importing statements."}
	stale              = &failure{409, "STALE_REVISION", "The preview has changed. Reload it and try again."}
	notReady           = &failure{409, "BATCH_NOT_READY", "This import is not ready to confirm."}
	alreadyImported    = &failure{409, "ALREADY_IMPORTED", "This statement has already been imported for this account."}
	notFound           = &failure{404, "NOT_FOUND", "The import or account was not found."}
	expired            = &failure{410, "BATCH_EXPIRED", "This preview has expired. Upload the statement again."}
	busy               = &failure{429, "EXTRACTOR_BUSY", "The extractor is busy. Retry in a moment."}
	extractFailed      = &failure{502, "EXTRACTION_FAILED", "The PDF could not be processed."}
	timedOut           = &failure{504, "EXTRACTION_TIMEOUT", "The statement took too long to process."}
	internal           = &failure{500, "INTERNAL_ERROR", "The import could not be saved. Try again."}
)
