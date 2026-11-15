"""Extract PDF geometry in a disposable subprocess."""

import json
import subprocess
import sys

import pdfplumber
from pdfminer.pdfdocument import PDFPasswordIncorrect

MAX_PAGES = 100
MAX_WORDS = 100_000
MAX_RESPONSE = 32 << 20


class ExtractionError(Exception):
    pass


def read_pages(path, original_text=None):
    pages = []
    total_words = 0
    with pdfplumber.open(path) as pdf:
        if pdf.doc.encryption is not None:
            raise ExtractionError("ENCRYPTED_PDF_UNSUPPORTED")
        if not 0 < len(pdf.pages) <= MAX_PAGES:
            raise ExtractionError("PDF_LIMIT_EXCEEDED")
        if original_text is not None and len(original_text) != len(pdf.pages):
            raise ExtractionError("OCR_FAILED")
        for index, page in enumerate(pdf.pages):
            if not (0 < page.width <= 2500 and 0 < page.height <= 2500):
                raise ExtractionError("PDF_LIMIT_EXCEEDED")
            words = []
            for item in page.extract_words(return_chars=True):
                sizes = [c["size"] for c in item["chars"] if c.get("size") is not None]
                words.append(
                    {key: item[key] for key in ("text", "x0", "x1", "top", "bottom")}
                    | {"size": round(sum(sizes) / len(sizes), 2) if sizes else None}
                )
            total_words += len(words)
            if total_words > MAX_WORDS:
                raise ExtractionError("PDF_LIMIT_EXCEEDED")
            has_text = bool(words) if original_text is None else original_text[index]
            pages.append({
                "number": index + 1,
                "width": page.width,
                "height": page.height,
                "text_layer_exists": has_text,
                "ocr_used": original_text is not None and not has_text and bool(words),
                "words": words,
                "tables": [
                    {"bbox": table.bbox, "rows": table.extract(), "row_bounds": [row.bbox for row in table.rows]}
                    for table in page.find_tables()
                ],
            })
            pages[-1]["scan_candidate"] = not words and bool(page.images)
            page.close()
    return pages


def extract(source, ocr_output):
    pages = read_pages(source)
    flags = [page["text_layer_exists"] for page in pages]
    scan_indexes = [i for i, page in enumerate(pages) if page["scan_candidate"]]
    if scan_indexes:
        result = subprocess.run(
            [
                "ocrmypdf", "--skip-text", "--jobs", "1", "--skip-big", "24",
                "--tesseract-timeout", "35", "--optimize", "0", "--output-type", "pdf",
                source, ocr_output,
            ],
            stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
            check=False,
        )
        if result.returncode != 0:
            raise ExtractionError("OCR_FAILED")
        pages = read_pages(ocr_output, flags)
        if any(not pages[i]["words"] for i in scan_indexes):
            raise ExtractionError("OCR_FAILED")
    for page in pages:
        page.pop("scan_candidate")
    return {"page_count": len(pages), "ocr_used": bool(scan_indexes), "pages": pages}


def write_result(source, ocr_output, result):
    try:
        data = json.dumps(extract(source, ocr_output), separators=(",", ":"), allow_nan=False).encode()
        if len(data) > MAX_RESPONSE:
            raise ExtractionError("PDF_LIMIT_EXCEEDED")
    except (PDFPasswordIncorrect, pdfplumber.utils.exceptions.PdfminerException) as exc:
        if isinstance(exc, PDFPasswordIncorrect) or any(isinstance(arg, PDFPasswordIncorrect) for arg in exc.args):
            data = b'{"code":"ENCRYPTED_PDF_UNSUPPORTED"}'
        else:
            data = b'{"code":"INVALID_PDF"}'
    except ExtractionError as exc:
        data = json.dumps({"code": exc.args[0]}).encode()
    except Exception:
        data = b'{"code":"INVALID_PDF"}'
    with open(result, "wb") as output:
        output.write(data)


if __name__ == "__main__":
    write_result(*sys.argv[1:4])
