FROM python:3.11-slim
WORKDIR /app
COPY . /app
RUN pip install --no-cache-dir -r requirements.txt
ENV ALG_PORT=18086
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "18086"]
