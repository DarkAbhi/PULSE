"""Generate a Kotak-layout PDF with entirely invented statement data."""

from pathlib import Path

OUTPUT = Path(__file__).resolve().parents[1] / "tests" / "statement-import" / "fixtures" / "kotak_savings_synthetic.pdf"


def text(x, top, value):
    literal = value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
    return f"BT /F1 9 Tf {x} {842 - top - 9} Td ({literal}) Tj ET\n"


def pdf(objects):
    data = bytearray(b"%PDF-1.4\n")
    offsets = []
    for number, obj in enumerate(objects, 1):
        offsets.append(len(data))
        data.extend(f"{number} 0 obj\n".encode() + obj + b"\nendobj\n")
    start = len(data)
    data.extend(f"xref\n0 {len(offsets) + 1}\n0000000000 65535 f \n".encode())
    for offset in offsets:
        data.extend(f"{offset:010d} 00000 n \n".encode())
    data.extend(f"trailer\n<< /Size {len(offsets)+1} /Root 1 0 R >>\nstartxref\n{start}\n%%EOF\n".encode())
    return data


def main():
    layout = [
        (35, 20, "Account Statement"),
        (35, 35, "Kotak Mahindra Bank"),
        (35, 50, "Account No. 0000004321"),
        (35, 65, "Savings Account"),
        (35, 80, "Currency INDIAN RUPEE"),
        (44, 100, "#"), (75, 100, "Date"), (124, 100, "Description"),
        (280, 100, "Chq/Ref. No."), (355, 100, "Withdrawal (Dr.)"),
        (429, 100, "Deposit (Cr.)"), (498, 100, "Balance"),
        (39, 120, "-"), (72, 120, "-"), (118, 120, "Opening Balance"), (525, 120, "1,000.00"),
        (39, 140, "1"), (72, 140, "02 Jan 2026"), (118, 140, "Test market"),
        (391, 140, "20.00"), (525, 140, "980.00"),
        (118, 151, "Fabricated continuation"),
        (39, 170, "2"), (72, 170, "04 Jan 2026"), (118, 170, "Test refund"),
        (456, 170, "5.00"), (525, 170, "985.00"),
        (44, 230, "Particulars"), (286, 230, "Opening Balance"), (428, 230, "Closing Balance"),
        (44, 251, "Savings Account"), (365, 251, "1,000.00"), (498, 251, "985.00"),
    ]
    stream = "".join(text(*item) for item in layout).encode()
    objects = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Count 1 /Kids [3 0 R] >>",
        b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        f"<< /Length {len(stream)} >>\nstream\n".encode() + stream + b"endstream",
    ]
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_bytes(pdf(objects))


def write_variants():
    """Requires pypdf, Pillow and Poppler. Inputs contain only generated data."""
    from pypdf import PdfReader, PdfWriter
    from PIL import Image
    import subprocess
    import tempfile

    encrypted = PdfWriter(clone_from=PdfReader(OUTPUT))
    encrypted.encrypt("synthetic-test-password", algorithm="AES-128")
    with OUTPUT.with_name("encrypted_synthetic.pdf").open("wb") as stream:
        encrypted.write(stream)

    with tempfile.TemporaryDirectory() as root:
        scan = Path(root) / "scan"
        subprocess.run(["pdftoppm", "-jpeg", "-r", "144", "-singlefile", str(OUTPUT), str(scan)], check=True)
        jpeg = scan.with_suffix(".jpg").read_bytes()
        with Image.open(scan.with_suffix(".jpg")) as image:
            width, height = image.size
    scan_stream = b"q 595 0 0 842 0 0 cm /Im Do Q\n"
    OUTPUT.with_name("kotak_savings_scanned.pdf").write_bytes(pdf([
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Count 1 /Kids [3 0 R] >>",
        b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /XObject << /Im 4 0 R >> >> /Contents 5 0 R >>",
        (f"<< /Type /XObject /Subtype /Image /Width {width} /Height {height} /ColorSpace /DeviceRGB "
         f"/BitsPerComponent 8 /Filter /DCTDecode /Length {len(jpeg)} >>\nstream\n").encode()
        + jpeg + b"\nendstream",
        f"<< /Length {len(scan_stream)} >>\nstream\n".encode() + scan_stream + b"endstream",
    ]))
    OUTPUT.with_name("malformed.pdf").write_bytes(b"%PDF-1.5\nsynthetic invalid PDF\n")


if __name__ == "__main__":
    import sys

    main()
    if "--variants" in sys.argv[1:]:
        write_variants()
