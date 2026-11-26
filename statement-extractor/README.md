# Statement extractor

The private Compose service accepts multipart PDFs on `POST /extract` and
returns page geometry, words, tables, and OCR provenance. Go owns authentication,
profile selection, reconciliation, and transaction writes. pdfplumber **0.11.9**
handles digital text and tables. OCRmyPDF and Tesseract handle scans, which the
clients flag for additional review. An encrypted PDF returns
`ENCRYPTED_PDF_UNSUPPORTED`.

Every request runs extraction in a child process with a 120-second limit. Files
exist only in a private temporary folder in container tmpfs. The Python service
has no database credentials and exposes no host port.

Local worker tests, from the repository root:

```sh
python3 -m pip install -r statement-extractor/requirements.txt
python3 -m unittest discover -s statement-extractor -v
```

The scan test also needs OCRmyPDF and Tesseract. The service exposes `/healthz`
and Prometheus `/metrics` on its private network.
