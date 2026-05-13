# Nodify Newsletter Worker

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)
[![H2 Database](https://img.shields.io/badge/Database-H2-blue.svg)](https://www.h2database.com/)
[![Nodify](https://img.shields.io/badge/Powered%20by-Nodify-6A0DAD.svg)](https://github.com/AZIRARM/nodify)

## Overview

**Nodify Newsletter Worker** is a self-contained Spring Boot application that works exclusively with [Nodify Headless CMS](https://github.com/AZIRARM/nodify). It receives webhook notifications from Nodify when you publish newsletter content, fetches the complete content from the Nodify API, manages email campaigns, sends newsletters, and tracks open rates via tracking pixels.

> ⚠️ **Note:** This application is designed to work **only with Nodify Headless CMS**. It is not a standalone newsletter solution.

## Architecture

```
Nodify Headless CMS (publish content) 
  ↓ Webhook (POST /webhook/trigger)
Nodify Newsletter Worker
  ↓ Fetches content from Nodify API
  ↓ Stores in embedded H2 database
  ↓ Sends emails via SMTP
  ↓ Tracks opens via tracking pixels
User receives email
```

## Features

- 📨 **Webhook Receiver** - Listens for `newsletter-node` events from Nodify content
- 📄 **Content Fetching** - Retrieves HTML, JSON, and translations from Nodify API
- 🗄️ **Embedded Database** - H2 database with automatic schema creation
- 📧 **Email Campaigns** - Send newsletters to all registered users
- 🎯 **Tracking Pixels** - Track who opened which email
- 📊 **Dashboard** - Web interface to view campaigns and statistics
- 🔄 **Auto-Retry** - Automatically retry non-opened users after configurable intervals
- 📅 **Scheduled Campaigns** - Schedule campaigns for future dates
- 🌍 **Multi-language UI** - Support for FR, EN, ES, DE, PT
- 📁 **Export/Import** - Backup and restore all data via JSON

## Prerequisites

- Java 21+
- Maven
- SMTP server (Gmail, SendGrid, etc.)
- [Nodify Headless CMS](https://github.com/AZIRARM/nodify) instance
- Docker (optional)

## Quick Start

### Using Docker Compose

```bash
git clone https://github.com/AZIRARM/nodify-newsletter-worker.git
cd nodify-newsletter-worker
cp .env.example .env
# Edit .env with your configuration
docker-compose up -d
```

### Using Maven

```bash
git clone https://github.com/AZIRARM/nodify-newsletter-worker.git
cd nodify-newsletter-worker
cp .env.example .env
# Edit .env with your configuration
mvn spring-boot:run
```

## Configuration

### Environment Variables (.env)

```env
# Nodify API
NODIFY_API_URL=https://your-nodify-instance.com
NODIFY_API_SECRET=your-api-secret

# SMTP Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Tracking URL (public domain of this application)
TRACKING_BASE_URL=https://newsletter.yourdomain.com

# Server Port (default: 8080)
SERVER_PORT=8080
```

### application.yaml

```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  datasource:
    url: jdbc:h2:file:./data/newsletterdb
  jpa:
    hibernate:
      ddl-auto: update

nodify:
  api:
    url: ${NODIFY_API_URL}
    secret: ${NODIFY_API_SECRET}

webhook:
  tracking-base-url: ${TRACKING_BASE_URL}
```

## Webhook Integration with Nodify

### 1. In Nodify Headless CMS

Create a **content** (not a node) with newsletter-worker enabled and configure:

| Field | Value |
|-------|-------|
| Trigger URL | `https://your-server:8080/webhook/trigger` |
| Trigger Secret | (optional, same as `NODIFY_API_SECRET`) |
| Folder | Any name (used as campaign identifier) |

> 📝 **Important:** The webhook is triggered from a **content**, not from a node. Enable SSG on the content.

### 2. Webhook Payload (sent by Nodify from a content)

```json
{
  "event_type": "newsletter-node",
  "client_payload": {
    "code": "NEWSLETTER-CONTENT-xxxxx",
    "folder": "campaign-name",
    "ssg": true,
    "timestamp": "2025-01-01T00:00:00.000Z"
  }
}
```

### 3. What Happens Next

1. Nodify publishes your newsletter content (with SSG enabled)
2. Nodify sends a webhook to this application
3. This application fetches content from Nodify API
4. Creates a campaign and starts sending emails
5. Tracks opens via tracking pixels

## Content Structure in Nodify

Your newsletter **content** should contain:

### HTML Content (type: HTML)
- `index.html` or `favorite=true` → main email content
- `header.html` → optional header
- `footer.html` → optional footer

### JSON Configuration (type: JSON)
```json
{
  "title": "Newsletter Title",
  "description": "Newsletter description",
  "subject": "Email subject line",
  "translations": [
    {
      "language": "EN",
      "title": "English Title",
      "description": "English description",
      "subject": "English subject",
      "content": "English HTML content"
    },
    {
      "language": "ES",
      "title": "Título en Español",
      "description": "Descripción en español",
      "subject": "Asunto en español"
    }
  ]
}
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/webhook/trigger` | Receive webhook from Nodify |
| GET | `/webhook/track/{id}` | Tracking pixel (image/gif) |
| GET | `/` | Dashboard UI |
| GET | `/campaigns` | Campaigns list UI |
| GET | `/campaign/{id}` | Campaign details UI |
| GET | `/export` | Export all data as JSON |
| POST | `/import` | Import data from JSON |
| GET | `/health` | Health check |

## Dashboard Features

- 📊 **Statistics Cards** - Total users, campaigns, sent emails, open rate
- 📧 **Campaign List** - View all campaigns with status and statistics
- 🎯 **Campaign Details** - See which users opened the email
- 🔄 **Retry Campaign** - Resend to non-opened users
- ⏰ **Schedule** - Schedule campaigns for future dates
- 📁 **Export/Import** - Backup and restore all data

## Tracking Pixel

When an email is sent, a unique tracking pixel is embedded:

```html
<img src="https://your-server/webhook/track/unique-id" width="1" height="1" />
```

When the user opens the email, the application records:
- Who opened
- When they opened
- Impact metric

## Retry Logic

- Non-opened users are automatically retried after `retryIntervalMinutes`
- Retry interval can be configured per campaign
- Only users who haven't opened receive retries

## Data Models

| Entity | Description |
|--------|-------------|
| `User` | Newsletter subscribers (created via webhook) |
| `Newsletter` | Email content with translations |
| `Campaign` | Email sending campaign |
| `UserNewsletterStatus` | Tracking of who received/opened which email |
| `Translation` | Multi-language content |

## Docker

### Build Image

```bash
docker build -t nodify-newsletter-worker .
```

### Run Container

```bash
docker run -p 8080:8080 --env-file .env nodify-newsletter-worker
```

## Project Structure

```
nodify-newsletter-worker/
├── src/main/java/com/nodify/newsletter/
│   ├── NewsletterApplication.java
│   ├── config/
│   ├── controller/
│   ├── model/
│   ├── repository/
│   ├── service/
│   └── dto/
├── src/main/resources/
│   ├── application.yaml
│   ├── i18n/
│   ├── static/
│   │   ├── css/
│   │   └── js/
│   └── templates/
├── docker-compose.yml
├── Dockerfile
├── .env.example
└── pom.xml
```

## Development

```bash
# Clone
git clone https://github.com/AZIRARM/nodify-newsletter-worker.git

# Build
mvn clean package

# Run
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Testing Webhook Locally

```bash
curl -X POST http://localhost:8080/webhook/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "newsletter-node",
    "client_payload": {
      "code": "NEWSLETTER-CONTENT-xxxxx",
      "folder": "test-campaign",
      "ssg": true
    }
  }'
```

## Access URLs (after startup)

| URL | Description |
|-----|-------------|
| `http://localhost:8080` | Dashboard |
| `http://localhost:8080/campaigns` | Campaigns list |
| `http://localhost:8080/h2-console` | H2 database console |
| `http://localhost:8080/health` | Health check |

## License

MIT

## Links

- [Nodify Headless CMS](https://github.com/AZIRARM/nodify)
- [Nodify Newsletter Worker](https://github.com/AZIRARM/nodify-newsletter-worker)
- [Report Issue](https://github.com/AZIRARM/nodify-newsletter-worker/issues)

## Support

This application is designed to work **exclusively with Nodify Headless CMS**. For questions about Nodify, visit the [main repository](https://github.com/AZIRARM/nodify).