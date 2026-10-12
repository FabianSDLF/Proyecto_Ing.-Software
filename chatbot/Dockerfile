FROM python:3.11-slim

WORKDIR /app

# Instalar dependencias del sistema necesarias para compilar paquetes
RUN apt-get update && apt-get install -y --no-install-recommends \
    g++ \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Crear carpeta de recursos
RUN mkdir -p recursos

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
