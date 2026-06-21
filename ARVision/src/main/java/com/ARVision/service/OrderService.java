package com.ARVision.service;

import com.ARVision.dto.order.*;
import com.ARVision.entity.*;
import com.ARVision.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    // ── Get customer ───────────────────────────────────────────
    private Customer getCustomer(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    // ── Generate order number ──────────────────────────────────
    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }

    // ── Map to response ────────────────────────────────────────
    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getOrderItems()
                .stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .orderItemId(item.getOrderItemId())
                        .productId(item.getProduct().getProductId())
                        .productName(item.getProduct().getName())
                        .productImage(item.getProduct().getImageUrl())
                        .category(item.getProduct().getCategory())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getPrice())
                        .subtotal(item.getSubtotal())
                        .hasArModel(item.getProduct().getArModel() != null)
                        .arModelUrl(item.getProduct().getArModel() != null
                                ? item.getProduct().getArModel().getFileUrl()
                                : null)
                        .build())
                .toList();

        // Can only cancel if status is PENDING
        boolean canCancel = order.getStatus() == Order.OrderStatus.PENDING;

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomer().getUserId())
                .customerName(order.getCustomer().getName())
                .customerEmail(order.getCustomer().getEmail())
                .status(order.getStatus())
                .canCancel(canCancel)
                .totalAmount(order.getTotalAmount())
                .shippingAddress(AddressDto.fromString(order.getShippingAddress()))
                .contactNumber(order.getContactNumber())
                .orderDate(order.getOrderDate())
                .estimatedDelivery(order.getEstimatedDelivery())
                .items(itemResponses)
                .build();
    }

    // ── Resolve shipping address ───────────────────────────────
     // User provided a new address
    private String resolveAddress(
            Customer customer,
            AddressDto requestAddress,
            boolean updateSaved) {

        String resolvedAddress;

        if (requestAddress != null) {
            resolvedAddress = requestAddress.toFormattedString();

            System.out.println("=== ADDRESS DEBUG ===");
            System.out.println("updateSaved: " + updateSaved);
            System.out.println("Formatted: " + resolvedAddress);
            System.out.println("Customer ID: " + customer.getUserId());

            if (updateSaved) {
                Customer freshCustomer = customerRepository
                        .findById(customer.getUserId())
                        .orElseThrow(() -> new RuntimeException("Customer not found"));
                freshCustomer.setShippingAddress(resolvedAddress);
                customerRepository.saveAndFlush(freshCustomer);
                System.out.println("Address saved: " + freshCustomer.getShippingAddress());
            }

        } else if (customer.getShippingAddress() != null) {
            resolvedAddress = customer.getShippingAddress();
        } else {
            throw new RuntimeException(
                    "No shipping address found. Please provide a shipping address."
            );
        }

        return resolvedAddress;
    }

    // ── Check address (called before placing order) ────────────
    public AddressCheckResponse checkAddress(String email) {
        Customer customer = getCustomer(email);

        if (customer.getShippingAddress() != null) {
            return AddressCheckResponse.builder()
                    .hasAddress(true)
                    .savedAddress(AddressDto.fromString(customer.getShippingAddress()))
                    .message("We found your saved address. " +
                            "Would you like to use it or enter a new one?")
                    .build();
        }

        return AddressCheckResponse.builder()
                .hasAddress(false)
                .savedAddress(null)
                .message("No saved address found. Please enter your shipping address.")
                .build();
    }

    // ── Place order FROM CART ──────────────────────────────────
    @Transactional
    public OrderResponse placeOrderFromCart(String email, PlaceOrderRequest request) {
        Customer customer = getCustomer(email);

        // ── Fetch cart with items explicitly ──────────────────────
        Cart cart = cartRepository.findByCustomerWithItems(customer.getUserId())
                .orElseThrow(() -> new RuntimeException("No cart found"));

        System.out.println("=== CART DEBUG ===");
        System.out.println("Cart ID: " + cart.getCartId());
        System.out.println("Cart items size: " +
                (cart.getCartItems() != null ? cart.getCartItems().size() : "NULL"));

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cart is empty. Add products before ordering.");
        }

        // ── Resolve and save address ───────────────────────────────
        String shippingAddress = resolveAddress(
                customer,
                request.getShippingAddress(),
                request.isUpdateSavedAddress()
        );

        // ── Create order ───────────────────────────────────────────
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomer(customer);
        order.setShippingAddress(shippingAddress);
        order.setContactNumber(request.getContactNumber() != null
                ? request.getContactNumber()
                : customer.getPhone());
        order.setEstimatedDelivery(LocalDateTime.now().plusDays(5));
        order.setOrderItems(new ArrayList<>());

        Order savedOrder = orderRepository.save(order);

        // ── Create order items from cart ───────────────────────────
        float total = 0f;
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient stock for: " + product.getName()
                                + ". Available: " + product.getStockQuantity()
                );
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setSubtotal(cartItem.getSubtotal());
            orderItemRepository.save(orderItem);

            product.setStockQuantity(
                    product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            total += cartItem.getSubtotal();
        }

        savedOrder.setTotalAmount(total);
        orderRepository.save(savedOrder);

        // ── Clear cart ─────────────────────────────────────────────
        cart.getCartItems().clear();
        cart.setTotalAmount(0f);
        cartRepository.save(cart);

        // ── Reload fresh from DB ───────────────────────────────────
        Order freshOrder = orderRepository.findByIdWithItems(savedOrder.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return toResponse(freshOrder);
    }
    // ── Place DIRECT ORDER (without cart) ─────────────────────
    @Transactional
    public OrderResponse placeDirectOrder(String email, DirectOrderRequest request) {
        Customer customer = getCustomer(email);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Validate stock
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException(
                    "Only " + product.getStockQuantity() + " items available in stock"
            );
        }

        // Resolve address
        String shippingAddress = resolveAddress(
                customer,
                request.getShippingAddress(),
                request.isUpdateSavedAddress()
        );

        // Create order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomer(customer);
        order.setShippingAddress(shippingAddress);
        order.setContactNumber(request.getContactNumber() != null
                ? request.getContactNumber()
                : customer.getPhone());
        order.setEstimatedDelivery(LocalDateTime.now().plusDays(5));

        Order savedOrder = orderRepository.save(order);

        // Create single order item
        float subtotal = product.getPrice() * request.getQuantity();

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(savedOrder);
        orderItem.setProduct(product);
        orderItem.setQuantity(request.getQuantity());
        orderItem.setPrice(product.getPrice());
        orderItem.setSubtotal(subtotal);
        orderItemRepository.save(orderItem);

        // Reduce stock
        product.setStockQuantity(
                product.getStockQuantity() - request.getQuantity());
        productRepository.save(product);

        // Set total
        savedOrder.setTotalAmount(subtotal);
        orderRepository.save(savedOrder);

        Order freshOrder = orderRepository.findByIdWithItems(savedOrder.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return toResponse(freshOrder);

    }

    // ── Cancel order ───────────────────────────────────────────
    @Transactional
    public OrderResponse cancelOrder(String email, Long orderId) {
        Customer customer = getCustomer(email);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Security — customer can only cancel their own orders
        if (!order.getCustomer().getUserId().equals(customer.getUserId())) {
            throw new RuntimeException("Unauthorized to cancel this order");
        }

        // Cannot cancel if SHIPPED or beyond
        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
                order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new RuntimeException(
                    "Cannot cancel order. Order has already been " +
                            order.getStatus().name().toLowerCase()
            );
        }

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Order is already cancelled");
        }

        // Restore stock
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(
                    product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    // ── Get customer's orders ──────────────────────────────────
    @Transactional
    public Page<OrderResponse> getMyOrders(String email, int page, int size) {
        Customer customer = getCustomer(email);
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository
                .findByCustomerUserIdOrderByOrderDateDesc(
                        customer.getUserId(), pageable)
                .map(this::toResponse);
    }

    // ── Get single order ───────────────────────────────────────
    @Transactional
    public OrderResponse getOrderById(String email, Long orderId) {
        Customer customer = getCustomer(email);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Security check
        if (!order.getCustomer().getUserId().equals(customer.getUserId())) {
            throw new RuntimeException("Unauthorized to view this order");
        }

        return toResponse(order);
    }

    // ── ADMIN: Get all orders ──────────────────────────────────
    @Transactional
    public Page<OrderResponse> getAllOrders(
            String keyword,
            Order.OrderStatus status,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.searchOrders(keyword, status, pageable)
                .map(this::toResponse);
    }

    // ── ADMIN: Update order status ─────────────────────────────
    @Transactional
    public OrderResponse updateOrderStatus(
            Long orderId,
            OrderStatusUpdateRequest request) {

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Prevent going backwards in status
        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new RuntimeException("Cannot change status of a delivered order");
        }

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot change status of a cancelled order");
        }

        order.setStatus(request.getStatus());
        return toResponse(orderRepository.save(order));
    }

    // ── ADMIN: Dashboard stats ─────────────────────────────────
    public java.util.Map<String, Object> getOrderStats() {
        return java.util.Map.of(
                "totalOrders",    orderRepository.count(),
                "pendingOrders",  orderRepository.countByStatus(Order.OrderStatus.PENDING),
                "shippedOrders",  orderRepository.countByStatus(Order.OrderStatus.SHIPPED),
                "deliveredOrders",orderRepository.countByStatus(Order.OrderStatus.DELIVERED),
                "cancelledOrders",orderRepository.countByStatus(Order.OrderStatus.CANCELLED),
                "totalRevenue",   orderRepository.getTotalRevenue()
        );
    }
}