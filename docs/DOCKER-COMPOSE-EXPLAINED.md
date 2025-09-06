# 🐳 Docker Compose Files Explained

## Overview

We have two Docker Compose files for different purposes:

1. **`docker-compose.yml`** - Full production setup with all services
2. **`docker-compose.dev.yml`** - Development setup with only infrastructure

## 📁 File Structure

```
stock-genie/
├── docker-compose.yml          # Full stack (production)
├── docker-compose.dev.yml      # Development infrastructure only
├── docker/
│   ├── nginx/
│   │   └── nginx.conf          # Nginx configuration
│   └── init-scripts/           # Database initialization
└── docs/
    └── DOCKER-COMPOSE-EXPLAINED.md
```

## 🚀 Main Docker Compose (`docker-compose.yml`)

### Services Included:

#### 1. **PostgreSQL Database**
```yaml
postgres:
  image: postgres:15
  environment:
    POSTGRES_DB: stockgenie
    POSTGRES_USER: postgres
    POSTGRES_PASSWORD: password
  ports:
    - "5432:5432"
  volumes:
    - postgres_data:/var/lib/postgresql/data
```

**Purpose:**
- Stores stock data, technical indicators, and analysis results
- Port 5432 accessible from host machine
- Data persists in Docker volume

#### 2. **Redis Cache**
```yaml
redis:
  image: redis:7-alpine
  ports:
    - "6379:6379"
  volumes:
    - redis_data:/data
  command: redis-server --appendonly yes
```

**Purpose:**
- Caches API responses to avoid rate limits
- Caches technical analysis results
- Improves application performance

#### 3. **Ollama LLM Service**
```yaml
ollama:
  image: ollama/ollama:latest
  ports:
    - "11434:11434"
  volumes:
    - ollama_data:/root/.ollama
  environment:
    - OLLAMA_HOST=0.0.0.0
    - OLLAMA_ORIGINS=*
  deploy:
    resources:
      reservations:
        memory: 8G
```

**Purpose:**
- Runs Mistral 7B and Llama 2 models locally
- Provides AI-powered stock analysis
- Reserves 8GB RAM for model inference

#### 4. **Spring Boot Backend**
```yaml
backend:
  build:
    context: ./backend
    dockerfile: Dockerfile
  ports:
    - "8080:8080"
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/stockgenie
    - LOCAL_LLM_BASE_URL=http://ollama:11434
  depends_on:
    postgres:
      condition: service_healthy
    redis:
      condition: service_healthy
    ollama:
      condition: service_healthy
```

**Purpose:**
- REST API for stock data and analysis
- Integrates with database, cache, and LLM
- Waits for dependencies to be healthy

#### 5. **React Frontend**
```yaml
frontend:
  build:
    context: ./frontend
    dockerfile: Dockerfile
  ports:
    - "3000:3000"
  environment:
    - REACT_APP_API_URL=http://localhost:8080/api/v1
  depends_on:
    - backend
```

**Purpose:**
- Interactive dashboard for stock analysis
- Connects to backend API
- Displays charts and LLM insights

#### 6. **Nginx Reverse Proxy** (Production only)
```yaml
nginx:
  image: nginx:alpine
  ports:
    - "80:80"
    - "443:443"
  profiles:
    - production
```

**Purpose:**
- Routes traffic to frontend/backend
- Handles SSL termination
- Only runs with `--profile production`

## 🛠️ Development Docker Compose (`docker-compose.dev.yml`)

### Purpose:
- Run only infrastructure services (PostgreSQL, Redis, Ollama)
- Develop backend and frontend locally
- Faster iteration and debugging

### Services:
- ✅ PostgreSQL (database)
- ✅ Redis (cache)
- ✅ Ollama (LLM)
- ❌ Backend (run locally)
- ❌ Frontend (run locally)
- ❌ Nginx (not needed)

### Usage:
```bash
# Start infrastructure only
docker-compose -f docker-compose.dev.yml up -d

# Run backend locally
cd backend && ./mvnw spring-boot:run

# Run frontend locally
cd frontend && npm start
```

## 🔧 Key Features

### Health Checks
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres -d stockgenie"]
  interval: 10s
  timeout: 5s
  retries: 5
```

**Benefits:**
- Ensures services are ready before starting dependencies
- Prevents connection errors
- Automatic retry on failure

### Service Dependencies
```yaml
depends_on:
  postgres:
    condition: service_healthy
  redis:
    condition: service_healthy
```

**Benefits:**
- Automatic startup order
- Waits for dependencies to be healthy
- Prevents race conditions

### Data Persistence
```yaml
volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local
  ollama_data:
    driver: local
```

**Benefits:**
- Data survives container restarts
- Database data persists
- Downloaded models persist

### Network Isolation
```yaml
networks:
  stock-genie-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

**Benefits:**
- Services communicate by name
- Isolated from other projects
- Secure internal communication

## 🚀 Usage Commands

### Development Mode
```bash
# Start infrastructure
docker-compose -f docker-compose.dev.yml up -d

# Check services
docker-compose -f docker-compose.dev.yml ps

# View logs
docker-compose -f docker-compose.dev.yml logs -f

# Stop services
docker-compose -f docker-compose.dev.yml down
```

### Production Mode
```bash
# Start all services
docker-compose up -d

# Start with Nginx
docker-compose --profile production up -d

# Check services
docker-compose ps

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Maintenance
```bash
# Reset everything (WARNING: deletes data)
docker-compose down -v

# Update images
docker-compose pull

# Rebuild services
docker-compose up -d --build

# Scale services
docker-compose up -d --scale backend=2
```

## 🎯 Benefits for Stock Genie

1. **Easy Setup**: One command starts all services
2. **Consistent Environment**: Same setup everywhere
3. **Service Isolation**: No conflicts with other projects
4. **Data Persistence**: Database and models survive restarts
5. **Production Ready**: Same containers for deployment
6. **Team Collaboration**: Everyone has identical setup
7. **Easy Cleanup**: Simple to reset or remove

## 🔍 Troubleshooting

### Common Issues:

1. **Port Conflicts**
   ```bash
   # Check what's using ports
   lsof -i :5432
   lsof -i :6379
   lsof -i :8080
   lsof -i :3000
   ```

2. **Memory Issues**
   ```bash
   # Check Docker memory
   docker system df
   docker system prune -a
   ```

3. **Service Not Starting**
   ```bash
   # Check logs
   docker-compose logs postgres
   docker-compose logs redis
   docker-compose logs ollama
   ```

4. **Network Issues**
   ```bash
   # Check networks
   docker network ls
   docker network inspect stock-genie_stock-genie-network
   ```

## 🎉 Next Steps

1. **Choose Development Approach**: Docker Compose or manual setup
2. **Start Infrastructure**: `docker-compose -f docker-compose.dev.yml up -d`
3. **Begin Backend Development**: Stage 2 implementation
4. **Test Integration**: Verify all services work together
