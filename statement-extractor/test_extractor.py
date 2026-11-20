import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
import unittest
from unittest.mock import patch

import server

fixture = Path(__file__).resolve().parents[1] / "tests/statement-import/fixtures/kotak_savings_synthetic.pdf"


class TransportTests(unittest.TestCase):
    def test_multipart_preserves_binary_bytes(self):
        header = "multipart/form-data; boundary=synthetic-test"
        payload = b"%PDF-1.7\r\n%\x80\xeb\x00\r\na middle line\r\n"
        form = (
            b'--synthetic-test\r\nContent-Disposition: form-data; name="file"; filename="test.pdf"\r\n'
            b"Content-Type: application/pdf\r\n\r\n" + payload + b"\r\n--synthetic-test--\r\n"
        )
        self.assertEqual(server.multipart_pdf(header, form), payload)
        self.assertIsNone(server.multipart_pdf(header, form[:-8]))

    def test_worker_cleanup_and_timeout_never_returns_document_data(self):
        with tempfile.TemporaryDirectory() as root:
            worker = Path(root) / "worker.py"
            worker.write_text("import time\ntime.sleep(60)\n")
            with patch.object(server, "WORKER", worker), patch.object(server, "TIMEOUT", 0.05):
                with patch.object(tempfile, "tempdir", root):
                    status, payload = server.process(b"%PDF-1.7 not logged")
            self.assertEqual(status, 504)
            self.assertEqual(json.loads(payload), {"code": "EXTRACTION_TIMEOUT"})
            self.assertEqual(list(Path(root).iterdir()), [worker])


@unittest.skipUnless(importlib.util.find_spec("pdfplumber"), "install requirements to test extraction")
class GeometryTests(unittest.TestCase):
    def test_stable_geometry_and_no_pdf_left_after_success(self):
        with tempfile.TemporaryDirectory() as root:
            with patch.object(tempfile, "tempdir", root):
                status, raw = server.process(fixture.read_bytes())
            self.assertEqual(status, 200)
            document = json.loads(raw)
            self.assertEqual(document["page_count"], 1)
            self.assertFalse(document["ocr_used"])
            page = document["pages"][0]
            self.assertTrue(page["text_layer_exists"])
            self.assertEqual((page["width"], page["height"]), (595, 842))
            anchor = next(word for word in page["words"] if word["text"] == "Description")
            self.assertAlmostEqual(anchor["x0"], 124, delta=0.25)
            self.assertAlmostEqual(anchor["size"], 9, delta=0.25)
            self.assertEqual(list(Path(root).iterdir()), [])

    def test_invalid_pdf_returns_no_parser_exception(self):
        status, raw = server.process(fixture.with_name("malformed.pdf").read_bytes())
        self.assertEqual(status, 422)
        self.assertEqual(json.loads(raw), {"code": "INVALID_PDF"})

    def test_encrypted_pdf_is_rejected(self):
        status, raw = server.process(fixture.with_name("encrypted_synthetic.pdf").read_bytes())
        self.assertEqual(status, 422)
        self.assertEqual(json.loads(raw), {"code": "ENCRYPTED_PDF_UNSUPPORTED"})

    @unittest.skipUnless(shutil.which("ocrmypdf"), "OCRmyPDF is required for this test")
    def test_rasterized_pdf_uses_ocr(self):
        status, raw = server.process(fixture.with_name("kotak_savings_scanned.pdf").read_bytes())
        self.assertEqual(status, 200)
        document = json.loads(raw)
        self.assertTrue(document["ocr_used"])
        self.assertFalse(document["pages"][0]["text_layer_exists"])
        self.assertIn("Balance", {word["text"] for word in document["pages"][0]["words"]})
