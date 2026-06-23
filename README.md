
## Technology Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.5 |
| Language | Java 21 |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| Database | PostgreSQL (NeonDB serverless) |
| ORM | Spring Data JPA / Hibernate 6 |
| File Storage | Cloudinary (GLB/USDZ AR models) |
| Payment | Stripe |
| Connection Pool | HikariCP |
| Build Tool | Maven |
| API Style | REST + JSON |

## System Architecture

```
React/Next.js Frontend
        │
        ▼
Spring Boot REST API (port 8080)
        │
        ├── Spring Security (JWT Auth Filter)
        ├── Controller Layer
        ├── Service Layer
        ├── Repository Layer (Spring Data JPA)
        │
        ├── PostgreSQL (NeonDB) ──── All relational data
        ├── Cloudinary ──────────── 3D AR model files (GLB/USDZ)
        └── Stripe ──────────────── Payment processing
```

## System Roles

- **SUPER_ADMIN**: Full access to everything
- **PRODUCT_MANAGER**: Products and AR models only
- **ORDER_MANAGER**: Orders and payments only
- **USER_MANAGER**: User management only
- **CUSTOMER**: Shopping, cart, orders, payments

## Global Authentication & Authorization

All protected routes require an HTTP Authorization header containing a JSON Web Token (JWT).

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Token Strategy

VisionCart uses a dual-token strategy:

| Token | Location | Expiry | Purpose |
|---|---|---|---|
| Access Token | JSON response body → stored in memory | 15 minutes | Authenticates every API request |
| Refresh Token | HttpOnly cookie (JS cannot read it) | 7 days | Issues new access tokens silently |

### Decoded JWT Access Token Payload Structure

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "role": "CUSTOMER",
    "adminRole": null,
    "employeeId": null,
    "email": "roisul@example.com",
    "name": "Roisul Islam",
    "userId": 1
  }
}
```

### Response Format

Every API response follows this unified structure:

**Success Response:**
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-06-23T11:00:00"
}
```

**Error Response:**
```json
{
  "success": false,
  "message": "Descriptive error message here",
  "timestamp": "2026-06-23T11:00:00"
}
```

**Validation Error Response:**
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "email": "Invalid email format",
    "password": "Password must be at least 6 characters"
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

**Paginated Response** (`data` field contains):
```json
{
  "content": [ ... ],
  "totalElements": 100,
  "totalPages": 9,
  "size": 12,
  "number": 0,
  "first": true,
  "last": false
}
```

---

## API Endpoint Specifications

### 1. Auth Service
Base URL: `/api/auth`

#### 1.1 Customer Register

```
POST /api/auth/register
```

**Auth Required:** No

**Request Body:**

```json
{
  "name": "Roisul Islam",
  "email": "roisul@example.com",
  "password": "123456",
  "phone": "01711111111",
  "address": "Sylhet, Bangladesh",
  "shippingAddress": null
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| name | String | Yes | Full name |
| email | String | Yes | Must be valid email, must be unique |
| password | String | Yes | Minimum 6 characters |
| phone | String | No | Contact number |
| address | String | No | General address |
| shippingAddress | String | No | Default shipping address |

**Success Response (200):**

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "role": "CUSTOMER",
    "adminRole": null,
    "employeeId": null,
    "email": "roisul@example.com",
    "name": "Roisul Islam",
    "userId": 1
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

> **Cookie set:** `refreshToken` (HttpOnly, path `/api/auth`)

---

#### 1.2 Login

```
POST /api/auth/login
```

**Auth Required:** No — works for both customers and admins

**Request Body:**

```json
{
  "email": "roisul@example.com",
  "password": "123456"
}
```

**Success Response (200):**

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "role": "CUSTOMER",
    "adminRole": null,
    "employeeId": null,
    "email": "roisul@example.com",
    "name": "Roisul Islam",
    "userId": 1
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

**Frontend routing logic:**
```
CUSTOMER → redirect to /shop
ADMIN →
  SUPER_ADMIN      → /dashboard/super
  PRODUCT_MANAGER  → /dashboard/products
  ORDER_MANAGER    → /dashboard/orders
  USER_MANAGER     → /dashboard/users
```

**Error Response (400):**

```json
{
  "success": false,
  "message": "Invalid email or password",
  "timestamp": "2026-06-23T11:00:00"
}
```

---

#### 1.3 Refresh Access Token

```
POST /api/auth/refresh
```

**Auth Required:** No — browser automatically sends `refreshToken` cookie

**Request Body:** None

**Success Response (200):**

```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "role": "CUSTOMER",
    "email": "roisul@example.com",
    "name": "Roisul Islam",
    "userId": 1
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

> **When to call this:** When any API returns 401, silently call this endpoint to get a new access token, then retry the original request.

---

#### 1.4 Logout

```
POST /api/auth/logout
```

**Auth Required:** Yes (any role)

**Request Body:** None

**Success Response (200):**

```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null,
  "timestamp": "2026-06-23T11:00:00"
}
```

> Clears the `refreshToken` cookie.

---

#### 1.5 Create Admin (Super Admin Only)

```
POST /api/auth/admin/create
Authorization: Bearer <SUPER_ADMIN_ACCESS_TOKEN>
```

**Auth Required:** SUPER_ADMIN only

**Request Body:**

```json
{
  "name": "Product Manager John",
  "email": "john@arvision.com",
  "password": "123456",
  "phone": "01722222222",
  "employeeId": "EMP002",
  "adminRole": "PRODUCT_MANAGER"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| name | String | Yes | Admin's full name |
| email | String | Yes | Must be unique |
| password | String | Yes | Min 6 characters |
| phone | String | No | Contact number |
| employeeId | String | No | Employee ID |
| adminRole | Enum | Yes | `SUPER_ADMIN`, `PRODUCT_MANAGER`, `ORDER_MANAGER`, `USER_MANAGER` |

**Success Response (200):**

```json
{
  "success": true,
  "message": "Admin created successfully",
  "data": {
    "role": "ADMIN",
    "adminRole": "PRODUCT_MANAGER",
    "employeeId": "EMP002",
    "email": "john@arvision.com",
    "name": "Product Manager John",
    "userId": 3
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

---

### 2. Product APIs

Public base URL: `/api/products` | Admin base URL: `/api/admin/products`

#### 2.1 Get All Products (Home Page)

```
GET /api/products?page=0&size=12&sortBy=createdAt&sortDir=desc
```

**Auth Required:** No

**Query Parameters:**

| Parameter | Default | Description |
|---|---|---|
| page | 0 | Page number (0-based) |
| size | 12 | Items per page |
| sortBy | createdAt | Sort field: `createdAt`, `price`, `name` |
| sortDir | desc | Sort direction: `asc` or `desc` |

**Success Response (200):**

```json
{
  "success": true,
  "message": "Products fetched successfully",
  "data": {
    "content": [
      {
        "productId": 1,
        "name": "Modern Sofa",
        "description": "A comfortable 3-seater sofa perfect for living room",
        "price": 25000.0,
        "category": "Furniture",
        "stockQuantity": 15,
        "imageUrl": "https://example.com/sofa.jpg",
        "hasArModel": true,
        "arModelUrl": "https://res.cloudinary.com/.../sofa.glb",
        "createdAt": "2026-06-20T10:00:00"
      }
    ],
    "totalElements": 50,
    "totalPages": 5,
    "size": 12,
    "number": 0,
    "first": true,
    "last": false
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

> **`hasArModel: true`** means an AR button should be shown on this product card.

---

#### 2.2 Get Single Product

```
GET /api/products/{id}
```

**Auth Required:** No

**Success Response (200):**

```json
{
  "success": true,
  "message": "Product fetched successfully",
  "data": {
    "productId": 1,
    "name": "Modern Sofa",
    "description": "A comfortable 3-seater sofa perfect for living room",
    "price": 25000.0,
    "category": "Furniture",
    "stockQuantity": 15,
    "imageUrl": "https://example.com/sofa.jpg",
    "hasArModel": true,
    "arModelUrl": "https://res.cloudinary.com/.../sofa.glb",
    "createdAt": "2026-06-20T10:00:00"
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

**Error Response (404):**

```json
{
  "success": false,
  "message": "Product not found with id: 999",
  "timestamp": "2026-06-23T11:00:00"
}
```

---

#### 2.3 Real-Time Search

```
GET /api/products/search?keyword=sofa&page=0&size=12
```

**Auth Required:** No

**Query Parameters:**

| Parameter | Required | Description |
|---|---|---|
| keyword | No | Search term — if empty, returns all products |
| page | No (default 0) | Page number |
| size | No (default 12) | Items per page |

**Success Response (200):** Same structure as Get All Products.

> **Call this on every keystroke** with a 300ms debounce. Searches across product name, description, and category simultaneously.

---

#### 2.4 Filter Products

```
GET /api/products/filter?category=Furniture&minPrice=5000&maxPrice=50000&sortBy=price&sortDir=asc
```

**Auth Required:** No

**Query Parameters:**

| Parameter | Required | Description |
|---|---|---|
| keyword | No | Search term |
| category | No | Filter by category name |
| minPrice | No | Minimum price (float) |
| maxPrice | No | Maximum price (float) |
| page | No (default 0) | Page number |
| size | No (default 12) | Items per page |
| sortBy | No (default price) | Sort field |
| sortDir | No (default asc) | `asc` or `desc` |

> Only returns products with `stockQuantity > 0`.

**Success Response (200):** Same structure as Get All Products.

---

#### 2.5 Get All Categories

```
GET /api/products/categories
```

**Auth Required:** No

**Success Response (200):**

```json
{
  "success": true,
  "message": "Categories fetched successfully",
  "data": ["Electronics", "Furniture", "Home Decor", "Fashion"],
  "timestamp": "2026-06-23T11:00:00"
}
```

> Use this to populate the category filter dropdown.

---

#### 2.6 [Admin] Create Product

```
POST /api/admin/products?page=0&size=12
```

**Auth Required:** SUPER_ADMIN, PRODUCT_MANAGER

**Request Body:**

```json
{
  "name": "Modern Sofa",
  "description": "A comfortable 3-seater sofa perfect for living room",
  "price": 25000.00,
  "category": "Furniture",
  "stockQuantity": 15,
  "imageUrl": "https://example.com/sofa.jpg"
}
```

| Field | Type | Required | Validation |
|---|---|---|---|
| name | String | Yes | Not blank |
| description | String | No | - |
| price | Float | Yes | Must be positive |
| category | String | Yes | Not blank |
| stockQuantity | Integer | Yes | Must be >= 0 |
| imageUrl | String | No | URL to product image |

**Success Response (201):**

```json
{
  "success": true,
  "message": "Product created successfully",
  "data": {
    "productId": 5,
    "name": "Modern Sofa",
    "description": "A comfortable 3-seater sofa perfect for living room",
    "price": 25000.0,
    "category": "Furniture",
    "stockQuantity": 15,
    "imageUrl": "https://example.com/sofa.jpg",
    "hasArModel": false,
    "arModelUrl": null,
    "createdAt": "2026-06-23T11:00:00"
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

---

#### 2.7 [Admin] Update Product

```
PUT /api/admin/products/{id}
```

**Auth Required:** SUPER_ADMIN, PRODUCT_MANAGER

**Request Body:** Same as Create Product

**Success Response (200):** Updated product object.

---

#### 2.8 [Admin] Delete Product

```
DELETE /api/admin/products/{id}
```

**Auth Required:** SUPER_ADMIN, PRODUCT_MANAGER

**Success Response (200):**

```json
{
  "success": true,
  "message": "Product deleted successfully",
  "data": null,
  "timestamp": "2026-06-23T11:00:00"
}
```

---

#### 2.9 [Admin] Update Stock

```
PATCH /api/admin/products/{id}/stock?quantity=50
```

**Auth Required:** SUPER_ADMIN, PRODUCT_MANAGER

**Query Parameter:** `quantity` (integer, required, >= 0)

**Success Response (200):** Returns updated product.

---

#### 2.10 [Admin] Get Low Stock Products

```
GET /api/admin/products/low-stock?threshold=10
```

**Auth Required:** SUPER_ADMIN, PRODUCT_MANAGER

**Query Parameter:** `threshold` (default 10) — products with stock at or below this value

**Success Response (200):**

```json
{
  "success": true,
  "message": "Low stock products fetched",
  "data": [
    {
      "productId": 3,
      "name": "Gaming Chair",
      "stockQuantity": 2,
      "price": 18000.0,
      "category": "Furniture"
    }
  ],
  "timestamp": "2026-06-23T11:00:00"
}
```

---

### 3. AR Model APIs

#### 3.1 Get AR Model for a Product

```
GET /api/products/{productId}/ar-model
```

**Auth Required:** No

**Success Response (200):**

```json
{
  "success": true,
  "message": "AR model fetched successfully",
  "data": {
    "modelId": 1,
    "productId": 1,
    "productName": "Modern Sofa",
    "fileUrl": "https://res.cloudinary.com/your-cloud/raw/upload/v.../sofa.glb",
    "fileName": "ar-models/product_1_1781974990686.glb",
    "fileType": "GLB",
    "fileSize": 7.54,
    "uploadedAt": "2026-06-20T23:03:17"
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

> **Frontend AR integration:**
> ```html
> <script type="module" src="https://unpkg.com/@google/model-viewer/dist/model-viewer.min.js"></script>
> <model-viewer
>   src="{data.fileUrl}"
>   ar
>   ar-modes="webxr scene-viewer quick-look"
>   camera-controls
>   auto-rotate>
>   <button slot="ar-button">View in Your Room</button>
> </model-viewer>
> ```
> The "View in Your Room" button only appears on mobile (Android Chrome / iPhone Safari).

**Error Response (404):**

```json
{
  "success": false,
  "message": "No AR model found for this product",
  "timestamp": "2026-06-23T11:00:00"
}
```

---

#### 3.2 [Admin] Upload AR Model

```
POST /api/admin/products/{productId}/ar-model
```

**Auth Required:** SUPER_ADMIN, PRODUCT_MANAGER

**Content-Type:** `multipart/form-data`

**Request:** Form data with key `file` — must be a `.glb` or `.usdz` file, max 50MB.

```
form-data:
  file: [select .glb file]
```

**Success Response (200):**

```json
{
  "success": true,
  "message": "AR model uploaded successfully",
  "data": {
    "modelId": 1,
    "productId": 1,
    "productName": "Modern Sofa",
    "fileUrl": "https://res.cloudinary.com/.../sofa.glb",
    "fileName": "ar-models/product_1_1781974990686.glb",
    "fileType": "GLB",
    "fileSize": 7.54,
    "uploadedAt": "2026-06-20T23:03:17"
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

> If a product already has an AR model, uploading a new one automatically replaces and deletes the old one from Cloudinary.

---

#### 3.3 [Admin] Get All AR Models

```
GET /api/admin/ar-models
```

**Auth Required:** SUPER_ADMIN, PRODUCT_MANAGER

**Success Response (200):** Array of AR model objects.

---

#### 3.4 [Admin] Delete AR Model

```
DELETE /api/admin/products/{productId}/ar-model
```

**Auth Required:** SUPER_ADMIN, PRODUCT_MANAGER

**Success Response (200):**

```json
{
  "success": true,
  "message": "AR model deleted successfully",
  "data": null,
  "timestamp": "2026-06-23T11:00:00"
}
```

---

### 4. Cart APIs

Base URL: `/api/customer/cart`

All cart endpoints require: `Authorization: Bearer <CUSTOMER_TOKEN>`

---

#### 4.1 Get Cart

```
GET /api/customer/cart
```

**Auth Required:** CUSTOMER

**Success Response (200):**

```json
{
  "success": true,
  "message": "Cart fetched successfully",
  "data": {
    "cartId": 1,
    "customerId": 1,
    "items": [
      {
        "cartItemId": 3,
        "productId": 1,
        "productName": "Modern Sofa",
        "productImage": "https://example.com/sofa.jpg",
        "category": "Furniture",
        "unitPrice": 25000.0,
        "quantity": 2,
        "subtotal": 50000.0,
        "availableStock": 13,
        "hasArModel": true,
        "arModelUrl": "https://res.cloudinary.com/.../sofa.glb"
      }
    ],
    "totalItems": 1,
    "totalQuantity": 2,
    "totalAmount": 50000.0
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

> If the customer has no cart yet, an empty cart is created automatically and returned.

---

#### 4.2 Add Item to Cart

```
POST /api/customer/cart
```

**Auth Required:** CUSTOMER

**Request Body:**

```json
{
  "productId": 1,
  "quantity": 2
}
```

> If the product is already in the cart, the quantity is **added** to the existing quantity. Stock is validated — you cannot add more than available stock.

**Success Response (200):** Returns the full updated cart.

**Error Response (400) — insufficient stock:**

```json
{
  "success": false,
  "message": "Only 3 items available in stock",
  "timestamp": "2026-06-23T11:00:00"
}
```

---

#### 4.3 Update Cart Item Quantity

```
PATCH /api/customer/cart/items/{cartItemId}
```

**Auth Required:** CUSTOMER

**Request Body:**

```json
{
  "quantity": 3
}
```

> Set `quantity` to `0` to automatically remove the item from the cart.

**Success Response (200):** Returns the full updated cart.

---

#### 4.4 Remove Item from Cart

```
DELETE /api/customer/cart/items/{cartItemId}
```

**Auth Required:** CUSTOMER

**Success Response (200):** Returns the full updated cart without the removed item.

---

#### 4.5 Clear Entire Cart

```
DELETE /api/customer/cart
```

**Auth Required:** CUSTOMER

**Success Response (200):**

```json
{
  "success": true,
  "message": "Cart cleared successfully",
  "data": null,
  "timestamp": "2026-06-23T11:00:00"
}
```

---

### 5. Order APIs

Base URL: `/api/customer/orders`

All customer order endpoints require: `Authorization: Bearer <CUSTOMER_TOKEN>`

---

#### 5.1 Check Saved Address

```
GET /api/customer/orders/check-address
```

**Auth Required:** CUSTOMER

> Call this **before** showing the order placement form.

**Response when address exists (200):**

```json
{
  "success": true,
  "message": "Address check completed",
  "data": {
    "hasAddress": true,
    "savedAddress": {
      "division": "Dhaka",
      "zilla": "Dhaka",
      "upazilla": "Mirpur",
      "detailAddress": "House 10, Road 5, Block A"
    },
    "message": "We found your saved address. Would you like to use it or enter a new one?"
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

**Response when no address (200):**

```json
{
  "success": true,
  "message": "Address check completed",
  "data": {
    "hasAddress": false,
    "savedAddress": null,
    "message": "No saved address found. Please enter your shipping address."
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

> **Frontend UX flow:**
> - `hasAddress: false` → show address form (required)
> - `hasAddress: true` → show saved address with option to use it or enter a new one
> - `updateSavedAddress: true` → saves new address to customer's profile

---

#### 5.2 Place Order from Cart

```
POST /api/customer/orders/from-cart
```

**Auth Required:** CUSTOMER

> Converts the entire cart into an order. Cart is cleared automatically on success.

**Request Body — use saved address:**

```json
{
  "shippingAddress": null,
  "updateSavedAddress": false,
  "contactNumber": "01711111111"
}
```

**Request Body — new address, save it:**

```json
{
  "shippingAddress": {
    "division": "Dhaka",
    "zilla": "Dhaka",
    "upazilla": "Mirpur",
    "detailAddress": "House 10, Road 5, Block A"
  },
  "updateSavedAddress": true,
  "contactNumber": "01711111111"
}
```

| Field | Required | Description |
|---|---|---|
| shippingAddress | No | If `null`, uses saved address. Required if no saved address exists. |
| updateSavedAddress | No (default false) | If `true`, saves this address to the customer's profile |
| contactNumber | No | Uses profile phone if not provided |

**Success Response (200):**

```json
{
  "success": true,
  "message": "Order placed successfully",
  "data": {
    "orderId": 5,
    "orderNumber": "ORD-ABC12345",
    "customerId": 1,
    "customerName": "Roisul Islam",
    "customerEmail": "roisul@example.com",
    "status": "PENDING",
    "canCancel": true,
    "totalAmount": 50000.0,
    "shippingAddress": {
      "division": "Dhaka",
      "zilla": "Dhaka",
      "upazilla": "Mirpur",
      "detailAddress": "House 10, Road 5, Block A"
    },
    "contactNumber": "01711111111",
    "orderDate": "2026-06-23T11:00:00",
    "estimatedDelivery": "2026-06-28T11:00:00",
    "items": [
      {
        "orderItemId": 1,
        "productId": 1,
        "productName": "Modern Sofa",
        "productImage": "https://example.com/sofa.jpg",
        "category": "Furniture",
        "quantity": 2,
        "unitPrice": 25000.0,
        "subtotal": 50000.0,
        "hasArModel": true,
        "arModelUrl": "https://res.cloudinary.com/.../sofa.glb"
      }
    ]
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

> **Next step:** Use the `orderId` to initiate payment via `/api/customer/payments/{orderId}/create-intent`

---

#### 5.3 Place Direct Order (Without Cart)

```
POST /api/customer/orders/direct
```

**Auth Required:** CUSTOMER

> Buy a single product immediately without adding to cart first.

**Request Body:**

```json
{
  "productId": 1,
  "quantity": 1,
  "shippingAddress": {
    "division": "Chittagong",
    "zilla": "Chittagong",
    "upazilla": "Hathazari",
    "detailAddress": "Village: Mirsharai, House 5"
  },
  "updateSavedAddress": false,
  "contactNumber": "01811111111"
}
```

**Success Response (200):** Same structure as Place Order from Cart.

---

#### 5.4 Get My Orders

```
GET /api/customer/orders?page=0&size=10
```

**Auth Required:** CUSTOMER

**Success Response (200):** Paginated list of orders, newest first.

---

#### 5.5 Get Single Order

```
GET /api/customer/orders/{orderId}
```

**Auth Required:** CUSTOMER

**Success Response (200):** Full order details.

**Error Response (401):**

```json
{
  "success": false,
  "message": "Unauthorized to view this order",
  "timestamp": "2026-06-23T11:00:00"
}
```

---

#### 5.6 Cancel Order

```
DELETE /api/customer/orders/{orderId}/cancel
```

**Auth Required:** CUSTOMER

> Cancellation is only allowed when order status is `PENDING`. Once shipped, cancellation is blocked. Stock is automatically restored when cancelled.

**Success Response (200):** Updated order with `status: "CANCELLED"` and `canCancel: false`.

**Error Response (400) — already shipped:**

```json
{
  "success": false,
  "message": "Cannot cancel order. Order has already been shipped",
  "timestamp": "2026-06-23T11:00:00"
}
```

---

#### Order Status Reference

| Status | Meaning | Customer Can Cancel? |
|---|---|---|
| PENDING | Order placed, awaiting payment | Yes |
| PROCESSING | Payment received, being prepared | Yes |
| SHIPPED | Dispatched for delivery | No |
| DELIVERED | Delivered to customer | No |
| CANCELLED | Order cancelled | No |

---

### 6. Payment APIs

Base URL: `/api/customer/payments`

All customer payment endpoints require: `Authorization: Bearer <CUSTOMER_TOKEN>`

VisionCart uses **Stripe** for payment processing. The flow is:
1. Backend creates a PaymentIntent → returns `clientSecret`
2. Frontend uses Stripe.js to show card form and collect payment
3. Stripe sends a webhook to the backend confirming success
4. Backend updates payment and order status

---

#### 6.1 Create Payment Intent

```
POST /api/customer/payments/{orderId}/create-intent
```

**Auth Required:** CUSTOMER

**Request Body:** None

**Success Response (200):**

```json
{
  "success": true,
  "message": "Payment intent created successfully",
  "data": {
    "clientSecret": "pi_3TlA14FR3FTGIqHe17YwLfIH_secret_xxxxx",
    "paymentIntentId": "pi_3TlA14FR3FTGIqHe17YwLfIH",
    "amount": 5000000,
    "currency": "usd",
    "publishableKey": "pk_test_xxxxx",
    "orderId": 5,
    "orderTotal": 50000.0
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

> `amount` is in cents. `50000.0 BDT × 100 = 5000000 cents`.

**Frontend Stripe integration:**

```javascript
import { loadStripe } from '@stripe/stripe-js';

const stripe = await loadStripe(data.publishableKey);

const { error } = await stripe.confirmCardPayment(data.clientSecret, {
  payment_method: {
    card: cardElement,
    billing_details: { name: 'Customer Name' }
  }
});

if (!error) {
  // Payment confirmed on frontend
  // Backend webhook handles the status update automatically
}
```

**Test card numbers (Stripe test mode):**

| Scenario | Card Number | Expiry | CVC |
|---|---|---|---|
| Successful payment | 4242 4242 4242 4242 | Any future date | Any 3 digits |
| Declined | 4000 0000 0000 0002 | Any future date | Any 3 digits |
| 3D Secure required | 4000 0025 0000 3155 | Any future date | Any 3 digits |

---

#### 6.2 Get Payment Receipt

```
GET /api/customer/payments/{orderId}/receipt
```

**Auth Required:** CUSTOMER

**Success Response (200):**

```json
{
  "success": true,
  "message": "Receipt fetched successfully",
  "data": {
    "paymentId": 1,
    "orderId": 5,
    "orderNumber": "ORD-ABC12345",
    "stripePaymentIntentId": "pi_3TlA14FR3FTGIqHe17YwLfIH",
    "method": "STRIPE",
    "amount": 50000.0,
    "status": "COMPLETED",
    "transactionId": "ch_3TlA14FR3FTGIqHe17YwLfIH",
    "paymentDate": "2026-06-23T11:05:00",
    "receiptUrl": null,
    "refundable": true
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

> `refundable: true` means a refund button should be shown to the customer.

---

#### 6.3 Request Refund

```
POST /api/customer/payments/{orderId}/refund
```

**Auth Required:** CUSTOMER

**Request Body:**

```json
{
  "reason": "Changed my mind"
}
```

> Refunds are processed through Stripe automatically. Order is cancelled on refund. Refunds are blocked if the order is `SHIPPED` or `DELIVERED`.

**Success Response (200):**

```json
{
  "success": true,
  "message": "Refund processed successfully",
  "data": {
    "paymentId": 1,
    "status": "REFUNDED",
    "amount": 50000.0
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

---

#### Payment Status Reference

| Status | Meaning |
|---|---|
| PENDING | Payment initiated, awaiting confirmation |
| COMPLETED | Payment successful |
| FAILED | Payment failed |
| REFUND_REQUESTED | Customer requested refund |
| REFUNDED | Refund processed |

---

### 7. Admin Dashboard APIs

Base URL: `/api/admin/dashboard`

All dashboard endpoints require: `Authorization: Bearer <ANY_ADMIN_TOKEN>`

---

#### 7.1 Dashboard Overview

```
GET /api/admin/dashboard/overview
```

**Auth Required:** ANY_ADMIN

**Success Response (200):**

```json
{
  "success": true,
  "message": "Dashboard overview fetched",
  "data": {
    "totalOrders": 25,
    "pendingOrders": 5,
    "processingOrders": 8,
    "shippedOrders": 7,
    "deliveredOrders": 4,
    "cancelledOrders": 1,
    "totalRevenue": 625000.0,
    "revenueThisMonth": 350000.0,
    "revenueToday": 75000.0,
    "pendingPayments": 3,
    "completedPayments": 20,
    "failedPayments": 1,
    "refundedPayments": 1,
    "refundRequested": 0,
    "totalProducts": 12,
    "lowStockProducts": 3,
    "outOfStockProducts": 0,
    "productsWithAR": 5,
    "totalCustomers": 18,
    "newCustomersThisMonth": 7,
    "newCustomersToday": 2
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

---

#### 7.2 Sales Report

```
GET /api/admin/dashboard/sales-report?from=2026-06-01&to=2026-06-30
```

**Auth Required:** ANY_ADMIN

**Query Parameters:**

| Parameter | Required | Format | Description |
|---|---|---|---|
| from | Yes | YYYY-MM-DD | Start date (inclusive) |
| to | Yes | YYYY-MM-DD | End date (inclusive) |

> Maximum date range: 90 days.

**Success Response (200):**

```json
{
  "success": true,
  "message": "Sales report generated",
  "data": {
    "fromDate": "2026-06-01",
    "toDate": "2026-06-30",
    "totalOrders": 25,
    "totalRevenue": 625000.0,
    "averageOrderValue": 26041.67,
    "cancelledOrders": 1,
    "dailyBreakdown": [
      {
        "date": "2026-06-20",
        "ordersCount": 3,
        "revenue": 75000.0
      },
      {
        "date": "2026-06-21",
        "ordersCount": 5,
        "revenue": 125000.0
      },
      {
        "date": "2026-06-23",
        "ordersCount": 2,
        "revenue": 50000.0
      }
    ]
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

---

#### 7.3 Top Selling Products

```
GET /api/admin/dashboard/top-products?limit=10
```

**Auth Required:** ANY_ADMIN

**Query Parameter:** `limit` (default 10) — number of products to return

**Success Response (200):**

```json
{
  "success": true,
  "message": "Top products fetched",
  "data": [
    {
      "productId": 1,
      "productName": "Modern Sofa",
      "category": "Furniture",
      "imageUrl": "https://example.com/sofa.jpg",
      "totalQuantitySold": 12,
      "totalRevenue": 300000.0,
      "currentStock": 8,
      "hasArModel": true
    },
    {
      "productId": 3,
      "productName": "Smart TV 55inch",
      "category": "Electronics",
      "imageUrl": "https://example.com/tv.jpg",
      "totalQuantitySold": 7,
      "totalRevenue": 350000.0,
      "currentStock": 3,
      "hasArModel": false
    }
  ],
  "timestamp": "2026-06-23T11:00:00"
}
```

---

### 8. Admin Order Management APIs

Base URL: `/api/admin/orders`

Access: SUPER_ADMIN, ORDER_MANAGER

---

#### 8.1 Get All Orders

```
GET /api/admin/orders?keyword=roisul&status=PENDING&page=0&size=20
```

**Auth Required:** SUPER_ADMIN, ORDER_MANAGER

**Query Parameters:**

| Parameter | Required | Description |
|---|---|---|
| keyword | No | Search by customer name, email, or order number |
| status | No | `PENDING`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED` |
| page | No (default 0) | Page number |
| size | No (default 20) | Items per page |

**Success Response (200):** Paginated list of orders.

---

#### 8.2 Update Order Status

```
PATCH /api/admin/orders/{orderId}/status
```

**Auth Required:** SUPER_ADMIN, ORDER_MANAGER

**Request Body:**

```json
{
  "status": "SHIPPED",
  "note": "Dispatched via Pathao courier. Tracking: PTH123456"
}
```

Valid `status` transitions:
```
PENDING → PROCESSING → SHIPPED → DELIVERED
Any status → CANCELLED (unless already DELIVERED or CANCELLED)
```

**Success Response (200):** Updated order object.

**Error Response (400) — invalid transition:**

```json
{
  "success": false,
  "message": "Cannot change status of a delivered order",
  "timestamp": "2026-06-23T11:00:00"
}
```

---

#### 8.3 Order Stats

```
GET /api/admin/orders/stats
```

**Auth Required:** SUPER_ADMIN, ORDER_MANAGER

**Success Response (200):**

```json
{
  "success": true,
  "message": "Order stats fetched successfully",
  "data": {
    "totalOrders": 25,
    "pendingOrders": 5,
    "shippedOrders": 7,
    "deliveredOrders": 4,
    "cancelledOrders": 1,
    "totalRevenue": 625000.0
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

---

### 9. Admin Payment Management APIs

Base URL: `/api/admin/payments`

Access: SUPER_ADMIN, ORDER_MANAGER

---

#### 9.1 Get All Payments

```
GET /api/admin/payments?status=PENDING&page=0&size=20
```

**Auth Required:** SUPER_ADMIN, ORDER_MANAGER

**Query Parameters:**

| Parameter | Required | Description |
|---|---|---|
| status | No | `PENDING`, `COMPLETED`, `FAILED`, `REFUND_REQUESTED`, `REFUNDED` |
| page | No (default 0) | Page number |
| size | No (default 20) | Items per page |

**Success Response (200):** Paginated list of payment objects.

---

#### 9.2 Payment Stats

```
GET /api/admin/payments/stats
```

**Auth Required:** SUPER_ADMIN, ORDER_MANAGER

**Success Response (200):**

```json
{
  "success": true,
  "message": "Payment stats fetched successfully",
  "data": {
    "totalRevenue": 625000.0,
    "pendingPayments": 3,
    "completedPayments": 20,
    "failedPayments": 1,
    "refundedPayments": 1,
    "refundRequested": 0
  },
  "timestamp": "2026-06-23T11:00:00"
}
```

---

### 10. Stripe Webhook

```
POST /api/payments/webhook
```

**Auth Required:** No — called automatically by Stripe, not by the frontend

> Do NOT call this endpoint from the frontend. Stripe calls it automatically after a payment event. It handles `payment_intent.succeeded` and `payment_intent.payment_failed` events.

When `payment_intent.succeeded` fires:
- Payment status → `COMPLETED`
- Order status → `PROCESSING`

When `payment_intent.payment_failed` fires:
- Payment status → `FAILED`

---

## Role-Based Access Control

Quick reference for frontend routing decisions:

| Endpoint Group | CUSTOMER | PRODUCT_MANAGER | ORDER_MANAGER | USER_MANAGER | SUPER_ADMIN |
|---|---|---|---|---|---|
| `/api/products/**` (public) | | | | | |
| `/api/customer/**` | | | | | |
| `/api/admin/products/**` | | | | | |
| `/api/admin/ar-models/**` | | | | | |
| `/api/admin/orders/**` | | | | | |
| `/api/admin/payments/**` | | | | | |
| `/api/admin/dashboard/**` | | | | | |
| `/api/auth/admin/create` | | | | | |

---

## Standard Error Code Responses Reference

### 401 Unauthorized

Returned when the Authorization bearer token is missing, invalid, or expired.

```json
{
  "success": false,
  "error": "Unauthorized",
  "message": "Access token is missing or has expired."
}
```

### 403 Forbidden

Returned when user's RBAC role does not possess the permissions necessary to access the resource (e.g., a patient accessing Admin routes).

```json
{
  "success": false,
  "error": "Forbidden",
  "message": "You do not have permission to access this resource."
}
```
