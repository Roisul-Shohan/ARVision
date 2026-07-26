# ARVision

A full-stack **Augmented Reality (AR) e-commerce platform** built with Spring Boot 3.5 + PostgreSQL + Cloudinary + Stripe. Admins upload `.glb` / `.usdz` 3D models for products; customers view them in AR via their phone cameras.

🌐 **Live Demo:** https://arvision-jvan.onrender.com

---

## 🔑 Demo Credentials

Use these to log in and try the admin dashboard:

| Role | Email | Password |
|---|---|---|
| **SUPER_ADMIN** | `roisul192@gmail.com` | `123456` |

> Admin login is plain-text compared against `123456` in `AuthService.login`. To change it, edit `src/main/java/com/ARVision/service/AuthService.java` line 65 and redeploy.

---

## 🚀 Quick Start (Try It Right Now)

All endpoints are public for read-only flows and authenticated for admin/customer actions.

### Base URL

```
https://arvision-jvan.onrender.com
```

### Public Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET`  | `/api/products` | Paginated product list (`?page=0&size=12&sortBy=createdAt&sortDir=desc`) |
| `GET`  | `/api/products/{id}` | Single product detail |
| `GET`  | `/api/products/search?keyword=chair` | Real-time search (debounced) |
| `GET`  | `/api/products/filter?category=furniture&minPrice=100&maxPrice=500` | Advanced filter |
| `GET`  | `/api/products/categories` | All distinct categories |
| `POST` | `/api/auth/register` | Customer self-signup |
| `POST` | `/api/auth/login` | Returns JWT access + refresh tokens |
| `POST` | `/api/auth/refresh` | Renew access token |
| `POST` | `/api/payments/webhook` | Stripe webhook receiver |

### Admin Endpoints (require SUPER_ADMIN or ADMIN role)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/admin/create` | Create new admin (SUPER_ADMIN only) |
| `POST` | `/api/admin/products` | Create product |
| `POST` | `/api/admin/products/{id}/ar-model` | Upload `.glb` / `.usdz` AR model (multipart, max **100 MB**) |
| `PUT`  | `/api/admin/products/{id}` | Update product |
| `DELETE` | `/api/admin/products/{id}` | Delete product |
| `GET`  | `/api/admin/orders` | View all orders |
| `GET`  | `/api/admin/payments` | View all payments |
| `GET`  | `/api/admin/dashboard/stats` | Sales / revenue / counts |

### Customer Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/customer/cart/items` | Add to cart |
| `GET`  | `/api/customer/cart` | View cart |
| `POST` | `/api/customer/orders` | Place order (creates Stripe PaymentIntent) |
| `POST` | `/api/customer/payments/confirm` | Confirm payment |
| `GET`  | `/api/customer/payments/history` | Payment history |

---

## 🧪 Try It in Postman (60 seconds)

1. **Login as super admin:**
   ```
   POST https://arvision-jvan.onrender.com/api/auth/login
   Content-Type: application/json

   { "email": "roisul192@gmail.com", "password": "123456" }
   ```
   Copy the `accessToken` from the response.

2. **Authorize:** in Postman → Authorization → Type **Bearer Token** → paste `accessToken`.

3. **List products:**
   ```
   GET https://arvision-jvan.onrender.com/api/products
   ```

4. **Upload an AR model** (uses Postman **Body → form-data** with key `file`):
   ```
   POST https://arvision-jvan.onrender.com/api/admin/products/1/ar-model
   Authorization: Bearer <accessToken>
   Body (form-data):
     file   →  select a local .glb or .usdz file (max 100 MB)
   ```
   The file is streamed to Cloudinary; the returned `secure_url` is what the frontend AR viewer loads.

---

## 🛠 Run Locally (Development)

Prerequisites: **JDK 21**, **Maven 3.9+**, **PostgreSQL 16** (or Neon free tier).

```powershell
# 1. Clone
git clone https://github.com/Roisul-Shohan/ARVision.git
cd ARVision

# 2. Set up your dev application properties (do NOT commit this file)
#    Copy src/main/resources/application.properties.example to
#    src/main/resources/application-local.properties and fill in your real secrets.

# 3. Run
.\mvnw spring-boot:run `
   -Dspring-boot.run.profiles=local `
   -Dspring.config.additional-location=classpath:application-local.properties
```

The local server starts on `http://localhost:8080`.

---

## 📦 Tech Stack

| Layer | Tech |
|---|---|
| **Runtime** | Java 21, Spring Boot 3.5.15 |
| **Web** | Spring MVC 6.2, embedded Tomcat 10.1 |
| **Security** | Spring Security 6.5, JWT (jjwt 0.12), BCrypt |
| **Persistence** | Spring Data JPA 3.5, Hibernate 6.6, PostgreSQL |
| **File Storage** | Cloudinary (CDN-backed, supports streaming uploads > 10 MB) |
| **Payments** | Stripe Java SDK 24.3 (PaymentIntents API) |
| **Build** | Maven, multi-stage `Dockerfile` (Temurin 21) |
| **Deploy** | Render Web Service (Docker), Neon Postgres |

---

## 🏗 Architecture

```
┌──────────────┐         ┌──────────────────┐         ┌──────────────┐
│   Frontend   │ ──JWT──▶│  Spring Boot     │ ──────▶ │  Cloudinary  │
│ (mobile/web  │         │  REST API        │         │  (3D files)  │
│   AR viewer) │         │  Render hosted   │         └──────────────┘
└──────────────┘         │                  │
                         │  ├─ Auth/JWT     │ ──────▶ ┌──────────────┐
                         │  ├─ Products     │         │  PostgreSQL  │
                         │  ├─ Cart/Orders  │         │  Neon cloud  │
                         │  ├─ Stripe pay   │         └──────────────┘
                         │  └─ AR uploads   │
                         └──────────────────┘
                                │
                                ▼
                         ┌──────────────┐
                         │ Stripe API   │ (PaymentIntents + webhooks)
                         └──────────────┘
```

### Key design decisions

- **REST + JWT** stateless auth with refresh tokens in HttpOnly cookies + body fallback
- **JPA + Hibernate** with `spring.jpa.open-in-view=false` to avoid lazy-loading bugs
- **Cloudinary direct REST upload** from `ARModelService` (bypasses SDK's 10 MB cap)
- **Stripe PaymentIntents** so the customer authorizes payment client-side and we confirm server-side
- **AR assets served from Cloudinary CDN** with `resource_type=raw` for `.usdz` and an asset URL the AR viewer fetches

---

## 🔐 Environment Variables

All secrets live in **Render → Environment** (never in the repo). Required for full functionality:

| Name | Purpose |
|---|---|
| `DATABASE_URL` | PostgreSQL JDBC URL (Neon `?sslmode=require`) |
| `DATABASE_USERNAME`, `DATABASE_PASSWORD` | DB creds |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | 3D file storage |
| `JWT_SECRET` | HS256 signing key (≥ 256 bits) |
| `STRIPE_SECRET_KEY`, `STRIPE_PUBLISHABLE_KEY`, `STRIPE_WEBHOOK_SECRET` | Payments |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `PORT` | injected by Render automatically |

Local development reads `src/main/resources/application-local.properties` (gitignored).

---

## 📤 Deploying

The repo ships with a multi-stage `Dockerfile` (Temurin JDK 21 → JRE 21):

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/ARVision-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

To redeploy to Render: push to `main`. Render detects the Dockerfile and rebuilds in ~4–6 minutes.

---

## 🧹 Roadmap

- [ ] Replace hardcoded admin password with bcrypt + first-run seed `CommandLineRunner`
- [ ] Frontend AR viewer (model-viewer web component)
- [ ] Customer-order email receipts via SendGrid
- [ ] Image variants / product gallery
- [ ] Inventory low-stock webhook
- [ ] Stripe Connect for marketplace split

---

## 📄 License

MIT — see `LICENSE` (add when ready).

---

**Live URL:** https://arvision-jvan.onrender.com
