FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8000
# Start the FastAPI app with uvicorn. Shell form so $PORT (set by cloud hosts like
# Render) is honoured; falls back to 8000 locally.
CMD uvicorn main:app --host 0.0.0.0 --port ${PORT:-8000}
