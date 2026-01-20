FROM python:3.13-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PYTHONPATH=/app/src

WORKDIR /app

COPY requirements/ingest.txt /app/requirements/ingest.txt
COPY requirements/common.txt /app/requirements/common.txt
RUN pip install --no-cache-dir -r /app/requirements/ingest.txt

COPY . /app

ENV MODE=daily
CMD ["python", "-m", "rmhi.ingest.cli"]

