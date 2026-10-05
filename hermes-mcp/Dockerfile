FROM python:3.12-slim

WORKDIR /app
COPY hermes-mcp/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY hermes-mcp/server.py .
COPY release.json /release.json

ENV PYTHONDONTWRITEBYTECODE=1
USER 65534
CMD ["python", "server.py"]
