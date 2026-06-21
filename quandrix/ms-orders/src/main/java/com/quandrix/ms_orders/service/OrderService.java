package com.quandrix.ms_orders.service;

import com.quandrix.ms_orders.client.*;
import com.quandrix.ms_orders.dto.*;
import com.quandrix.ms_orders.exception.*;
import com.quandrix.ms_orders.model.Order;
import com.quandrix.ms_orders.model.OrderStatus;
import com.quandrix.ms_orders.repository.OrderRepository;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ListingClient listingClient;
    private final PaymentClient paymentClient;
    private final TransactionClient transactionClient;
    private final NotificationClient notificationClient;

    public OrderService(OrderRepository orderRepository,
                        ListingClient listingClient,
                        PaymentClient paymentClient,
                        TransactionClient transactionClient,
                        NotificationClient notificationClient) {
        this.orderRepository = orderRepository;
        this.listingClient = listingClient;
        this.paymentClient = paymentClient;
        this.transactionClient = transactionClient;
        this.notificationClient = notificationClient;
    }

    /**
     * Crea una nueva orden para un listing específico.
     * Se usa @Transactional para asegurar que la creación de la orden, el procesamiento del pago y la actualización del listing sean atómicos.
     * Es decir, si alguna de las operaciones falla, toda la transacción se revertirá para mantener la consistencia de los datos.
     * El flujo general es:
     * 1. Validar que el listing exista y esté disponible.
     * 2. Crear la orden en estado PENDING.
     * 3. Procesar el pago a través del PaymentService.
     * 4. Si el pago es aprobado, actualizar la orden a CONFIRMED y restar stock del listing o marcar como vendido según corresponda.
     * 5. Registrar la transacción en el TransactionService.
     * 6. Enviar notificaciones al comprador y al vendedor.
     * 7. Finalmente, marcar la orden como COMPLETED. 
     * Si en cualquier paso ocurre un error, se lanzará una excepción y la transacción se revertirá, dejando el sistema en un estado consistente.
     *   
     * 
     * @param request La solicitud de creación de orden.
     * @return La respuesta con la orden creada.
     */
    @Transactional
    public OrderResponse create(OrderRequest request) {
        log.info("Iniciando creación de orden: buyerId={} listingId={}",
                request.getBuyerId(), request.getListingId());

        // 1. Obtener y validar el listing
        ListingResponse listing;
        try {
            listing = listingClient.getById(request.getListingId());
        } catch (Exception e) {
            log.warn("Listing no encontrado: {}", request.getListingId());
            throw new InvalidOrderException(
                    "El listing " + request.getListingId() + " no existe");
        }

        // Validar que el listing esté activo
        if (!"ACTIVE".equals(listing.getStatus())) {
            log.warn("Listing {} no está activo, status={}",
                    request.getListingId(), listing.getStatus());
            throw new InvalidOrderException(
                    "El listing no está disponible para compra");
        }

        // Validar que el listing tenga stock disponible
        if (listing.getQuantity() == null || listing.getQuantity() <= 0) {
        log.warn("Listing {} sin stock disponible", request.getListingId());
        throw new InvalidOrderException("El listing no tiene stock disponible");
        }

        // Un comprador no puede comprar su propio listing
        if (listing.getSellerId().equals(request.getBuyerId())) {
            throw new InvalidOrderException(
                    "No puedes comprar tu propio listing");
        }

        // 2. Crear la orden en estado PENDING
        Order order = new Order();
        order.setBuyerId(request.getBuyerId());
        order.setListingId(request.getListingId());
        order.setSellerId(listing.getSellerId());
        order.setAmount(listing.getPrice());
        order.setPaymentMethod(request.getPaymentMethod());
        order = orderRepository.save(order);
        log.info("Orden creada en PENDING con id={}", order.getId());

        // 3. Procesar el pago
        PaymentResponse payment;

        try {
            log.info("Procesando pago");
            payment = paymentClient.process(new PaymentRequest(
                    order.getId(),
                    order.getAmount(),
                    request.getPaymentMethod()
            ));
        } catch (Exception e) {
            log.error("Error al procesar pago para orderId={}: {}",
                    order.getId(), e.getMessage());
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            throw new InvalidOrderException("Error al procesar el pago: " + e.getMessage());
        }

        if (!"APPROVED".equals(payment.getStatus())) {
            log.warn("Pago rechazado para orderId={}", order.getId());
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            throw new InvalidOrderException("El pago fue rechazado");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("Pago aprobado, orden {} en CONFIRMED", order.getId());

        // 4. Marcar el listing como vendido
        try {
            listingClient.markAsSold(request.getListingId());
            log.info("Listing {} marcado como SOLD", request.getListingId());
        } catch (Exception e) {
            log.error("Error al marcar listing como sold: {}", e.getMessage());
            // Continuamos el flujo — el pago ya fue procesado
        }

        // 5. Registrar la transacción
        try {
            transactionClient.register(new TransactionRequest(
                    order.getId(),
                    order.getBuyerId(),
                    order.getSellerId(),
                    listing.getScryfallId(),
                    order.getAmount()
            ));
            log.info("Transacción registrada para orderId={}", order.getId());
        } catch (Exception e) {
            log.error("Error al registrar transacción: {}", e.getMessage());
        }

        // 6. Notificar al comprador y al vendedor
        try {
            notificationClient.send(new NotificationRequest(
                    order.getBuyerId(),
                    "NEW_ORDER",
                    "Tu compra ha sido confirmada. Orden #" + order.getId()
            ));
            notificationClient.send(new NotificationRequest(
                    order.getSellerId(),
                    "LISTING_SOLD",
                    "Tu listing ha sido vendido. Orden #" + order.getId()
            ));
            log.info("Notificaciones enviadas para orderId={}", order.getId());
        } catch (Exception e) {
            log.error("Error al enviar notificaciones: {}", e.getMessage());
        }

        // 7. Marcar como completada
        order.setStatus(OrderStatus.COMPLETED);
        Order completed = orderRepository.save(order);
        log.info("Orden {} completada exitosamente", order.getId());
        return toResponse(completed);
    }

    public OrderResponse getById(Long id) {
        log.info("Buscando orden id={}", id);
        return toResponse(orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Orden no encontrada: {}", id);
                    return new OrderNotFoundException(id);
                }));
    }

    public List<OrderResponse> getByBuyer(Long buyerId) {
        log.info("Buscando órdenes del comprador={}", buyerId);
        return orderRepository.findByBuyerId(buyerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<OrderResponse> getBySeller(Long sellerId) {
        log.info("Buscando órdenes del vendedor={}", sellerId);
        return orderRepository.findBySellerId(sellerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public OrderResponse cancel(Long id, Long buyerId) {
        log.info("Cancelando orden id={} por buyerId={}", id, buyerId);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new InvalidOrderException(
                    "No puedes cancelar una orden que no es tuya");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Orden {} no cancelable en status={}", id, order.getStatus());
            throw new OrderNotCancellableException(id);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order cancelled = orderRepository.save(order);
        log.info("Orden {} cancelada", id);
        return toResponse(cancelled);
    }

    private OrderResponse toResponse(Order o) {
        return new OrderResponse(
                o.getId(), o.getBuyerId(), o.getListingId(),
                o.getSellerId(), o.getAmount(), o.getStatus(),
                o.getPaymentMethod(), o.getCreatedAt(), o.getUpdatedAt()
        );
    }
}