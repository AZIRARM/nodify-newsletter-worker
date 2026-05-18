# Nodify Newsletter Worker

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)
[![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-blue.svg)](https://www.postgresql.org/)
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
  ↓ Stores in PostgreSQL database
  ↓ Sends emails via SMTP
  ↓ Tracks opens via tracking pixels
User receives email
```

## Features

- 📨 **Webhook Receiver** - Listens for `newsletter-node` events from Nodify content
- 📄 **Content Fetching** - Retrieves HTML and JSON from Nodify API
- 🗄️ **PostgreSQL Database** - Production-ready database with Docker support
- 📧 **Email Campaigns** - Send newsletters to newsletter subscribers
- 🎯 **Tracking Pixels** - Track who opened which email
- 📊 **Dashboard** - Web interface to view campaigns and statistics
- 🔄 **Auto-Retry** - Automatically retry users who didn't receive the email
- 📅 **Scheduled Campaigns** - Schedule campaigns for future dates
- 🌍 **Multi-language UI** - Support for FR, EN, ES, DE, PT
- 📁 **Export/Import** - Backup and restore all data via JSON
- 🔔 **Subscribe/Unsubscribe** - API endpoints to manage newsletter subscriptions
- 👥 **User Management** - Manage subscribers per newsletter

## Prerequisites

- Java 21+
- Maven
- SMTP server (Gmail, SendGrid, etc.)
- [Nodify Headless CMS](https://github.com/AZIRARM/nodify) instance
- PostgreSQL (or Docker for containerized database)
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

### Using Maven with PostgreSQL

```bash
git clone https://github.com/AZIRARM/nodify-newsletter-worker.git
cd nodify-newsletter-worker

# Start PostgreSQL with Docker
docker run -d --name postgres -e POSTGRES_DB=newsletter -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:15

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

# Database (PostgreSQL)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=newsletter
DB_USERNAME=postgres
DB_PASSWORD=postgres

# SMTP Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Tracking URL (public domain of this application)
TRACKING_BASE_URL=https://newsletter.yourdomain.com

# Webhook Secret (optional, same as NODIFY_API_SECRET)
WEBHOOK_SECRET=your-webhook-secret

# Server Port (default: 8080)
SERVER_PORT=8080
```

### application.yaml

```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:newsletter}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: update

nodify:
  api:
    url: ${NODIFY_API_URL}
    secret: ${NODIFY_API_SECRET}

webhook:
  secret: ${WEBHOOK_SECRET:}
  tracking-base-url: ${TRACKING_BASE_URL}
```

## Webhook Integration with Nodify

### 1. In Nodify Headless CMS

Create a **content** (not a node) with newsletter-worker enabled and configure:

| Field | Value |
|-------|-------|
| Trigger URL | `https://your-server:8080/webhook/trigger` |
| Trigger Secret | (optional, same as `WEBHOOK_SECRET`) |
| Folder | Any name (used as campaign identifier) |

> 📝 **Important:** The webhook is triggered from a **content**, not from a node. Enable SSG on the content.

### 2. Webhook Payload Format

#### Complete Payload with All Fields

```json
{
  "event_type": "newsletter-node",
  "client_payload": {
    "campaignCode": "summer-sale-2024",
    "newsletterCode": "NEWSLETTER-MONTHLY",
    "contentCode": "HTML-CONTENT-xyz123",
    "title": "Summer Sale Newsletter",
    "subject": "Don't miss our summer deals!",
    "scheduledStart": "2024-07-01T09:00:00",
    "startDate": "2024-07-01T00:00:00",
    "endDate": "2024-07-31T23:59:59",
    "emails": [
      "user1@example.com",
      "user2@example.com",
      "user3@example.com"
    ],
    "timestamp": "2024-06-01T10:00:00Z"
  }
}
```

#### Field Descriptions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `event_type` | string | ✅ | Must be `"newsletter-node"` |
| `client_payload` | object | ✅ | Main payload container |
| `client_payload.campaignCode` | string | ❌ | Unique campaign identifier. If provided, creates/updates a campaign |
| `client_payload.newsletterCode` | string | ✅ | Unique newsletter identifier. Creates newsletter if not exists |
| `client_payload.contentCode` | string | ✅ | Nodify content code to fetch HTML/JSON content |
| `client_payload.title` | string | ❌ | Newsletter title (overrides JSON config) |
| `client_payload.subject` | string | ❌ | Email subject line (overrides JSON config) |
| `client_payload.scheduledStart` | string (ISO 8601) | ❌ | When to start sending. If empty, starts immediately |
| `client_payload.startDate` | string (ISO 8601) | ❌ | Campaign start date (for scheduling logic) |
| `client_payload.endDate` | string (ISO 8601) | ❌ | Campaign end date (no emails sent after this) |
| `client_payload.emails` | array of strings | ❌ | List of emails to subscribe to the newsletter |
| `client_payload.timestamp` | string (ISO 8601) | ❌ | Webhook timestamp |

#### Minimal Payload (Newsletter only)

```json
{
  "event_type": "newsletter-node",
  "client_payload": {
    "newsletterCode": "NEWSLETTER-MONTHLY",
    "contentCode": "HTML-CONTENT-xyz123"
  }
}
```

#### Minimal Payload with Campaign

```json
{
  "event_type": "newsletter-node",
  "client_payload": {
    "campaignCode": "summer-sale-2024",
    "newsletterCode": "NEWSLETTER-MONTHLY",
    "contentCode": "HTML-CONTENT-xyz123"
  }
}
```

#### Full Payload with All Features

```json
{
  "event_type": "newsletter-node",
  "client_payload": {
    "campaignCode": "black-friday-2024",
    "newsletterCode": "NEWSLETTER-PROMO",
    "contentCode": "HTML-BLACKFRIDAY",
    "title": "Black Friday Special",
    "subject": "Up to 70% off!",
    "scheduledStart": "2024-11-29T08:00:00",
    "startDate": "2024-11-25T00:00:00",
    "endDate": "2024-12-02T23:59:59",
    "emails": [
      "vip1@example.com",
      "vip2@example.com"
    ]
  }
}
```

### 3. What Happens Next

1. **Newsletter Creation/Update**
   - Finds or creates newsletter by `newsletterCode`
   - Fetches content from Nodify API using `contentCode`
   - Updates newsletter title, subject, and HTML content

2. **User Subscription**
   - For each email in `emails` array:
     - Creates user if not exists
     - Subscribes user to the newsletter (if not already subscribed)

3. **Campaign Handling** (if `campaignCode` provided)
   - Finds or creates campaign by `campaignCode`
   - Updates campaign dates (startDate, endDate, scheduledStart)
   - If `scheduledStart` is present → campaign status "SCHEDULED"
   - If no `scheduledStart` → starts sending immediately

4. **Email Sending**
   - Campaign sends emails to all newsletter subscribers
   - Tracks opens via tracking pixels

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/webhook/trigger` | Receive webhook from Nodify |
| GET | `/webhook/track/{id}` | Tracking pixel (image/gif) |
| GET | `/webhook/subscribe?email={email}&newsletter={id}` | Subscribe user to newsletter |
| GET | `/webhook/unsubscribe?email={email}&newsletter={id}` | Unsubscribe user from newsletter |
| GET | `/` | Dashboard UI |
| GET | `/campaigns` | Campaigns list UI |
| GET | `/campaign/{id}` | Campaign details UI |
| GET | `/newsletters` | Newsletters list UI |
| GET | `/newsletters/{id}/manage-users` | Manage newsletter subscribers UI |
| GET | `/users` | Users management UI |
| GET | `/export` | Export all data as JSON |
| POST | `/import` | Import data from JSON |

## Subscribe/Unsubscribe Endpoints

### Subscribe User

```http
GET /webhook/subscribe?email=user@example.com&newsletter=1
```

**Parameters:**
- `email` (required) - User email address
- `newsletter` (required) - Newsletter ID or newsletter code

**Response:**
```json
{
  "success": true,
  "message": "Subscribed successfully"
}
```

### Unsubscribe User

```http
GET /webhook/unsubscribe?email=user@example.com&newsletter=1
```

**Parameters:**
- `email` (required) - User email address
- `newsletter` (required) - Newsletter ID or newsletter code

**Response:**
```json
{
  "success": true,
  "message": "Unsubscribed successfully"
}
```

## Dashboard Features

- 📊 **Statistics Cards** - Total users, campaigns, sent emails, open rate
- 📧 **Campaign List** - View all campaigns with status and statistics
- 🎯 **Campaign Details** - See which users opened the email
- 🔄 **Retry Campaign** - Resend to users who didn't receive the email
- ⏰ **Schedule** - Schedule campaigns for future dates
- 📁 **Export/Import** - Backup and restore all data
- 👥 **Newsletter Subscribers** - Manage users per newsletter

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

- Users who didn't receive the email are automatically retried after `retryIntervalMinutes`
- Retry interval can be configured per campaign
- Only users with `sentAt = NULL` receive retries

## Data Models

| Entity | Description |
|--------|-------------|
| `User` | Newsletter subscribers |
| `Newsletter` | Email content template |
| `Campaign` | Email sending campaign |
| `UserNewsletterStatus` | Tracking of who received/opened which email (per campaign) |
| `UserNewsletterSubscription` | Link table between users and newsletters (subscriptions) |
| `Translation` | Multi-language content |

## Database Schema

### PostgreSQL Tables

| Table | Description |
|-------|-------------|
| `app_user` | Users/subscribers |
| `newsletter` | Newsletter templates |
| `campaign` | Email campaigns |
| `user_newsletter_status` | Campaign email tracking |
| `user_newsletter_subscription` | User subscriptions to newsletters |
| `translation` | Multi-language content |

## Docker Compose Full Stack

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: newsletter
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  newsletter-worker:
    build: .
    ports:
      - "8080:8080"
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: newsletter
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
    env_file:
      - .env
    depends_on:
      - postgres

volumes:
  postgres_data:
```

## Testing Webhook Locally

### Minimal Request

```bash
curl -X POST http://localhost:8080/webhook/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "newsletter-node",
    "client_payload": {
      "newsletterCode": "TEST-NEWSLETTER",
      "contentCode": "YOUR-CONTENT-CODE"
    }
  }'
```

### Full Request with Campaign and Emails

```bash
curl -X POST http://localhost:8080/webhook/trigger \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "newsletter-node",
    "client_payload": {
      "campaignCode": "test-campaign",
      "newsletterCode": "TEST-NEWSLETTER",
      "contentCode": "YOUR-CONTENT-CODE",
      "title": "Test Newsletter",
      "subject": "Test Subject",
      "scheduledStart": "2024-12-01T10:00:00",
      "startDate": "2024-12-01T00:00:00",
      "endDate": "2024-12-31T23:59:59",
      "emails": ["test1@example.com", "test2@example.com"]
    }
  }'
```

### Request with Authentication

```bash
curl -X POST http://localhost:8080/webhook/trigger \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-webhook-secret" \
  -d '{
    "event_type": "newsletter-node",
    "client_payload": {
      "newsletterCode": "TEST-NEWSLETTER",
      "contentCode": "YOUR-CONTENT-CODE"
    }
  }'
```

## Access URLs (after startup)

| URL | Description |
|-----|-------------|
| `http://localhost:8080` | Dashboard |
| `http://localhost:8080/campaigns` | Campaigns list |
| `http://localhost:8080/newsletters` | Newsletters list |
| `http://localhost:8080/users` | Users management |
| `http://localhost:8080/health` | Health check |

## License

MIT

## Links

- [Nodify Headless CMS](https://github.com/AZIRARM/nodify)
- [Nodify Newsletter Worker](https://github.com/AZIRARM/nodify-newsletter-worker)
- [Report Issue](https://github.com/AZIRARM/nodify-newsletter-worker/issues)

## Support

This application is designed to work **exclusively with Nodify Headless CMS**. For questions about Nodify, visit the [main repository](https://github.com/AZIRARM/nodify).
