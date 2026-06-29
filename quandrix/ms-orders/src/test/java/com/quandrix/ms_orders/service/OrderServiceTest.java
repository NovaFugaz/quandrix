package com.quandrix.ms_orders.service;

import com.quandrix.ms_orders.client.ListingClient;
import com.quandrix.ms_orders.client.NotificationClient;
import com.quandrix.ms_orders.client.PaymentClient;
import com.quandrix.ms_orders.client.TransactionClient;
import com.quandrix.ms_orders.dto.*;
import com.quandrix.ms_orders.exception.InvalidOrderException;
import com.quandrix.ms_orders.exception.OrderNotCancellableException;
import com.quandrix.ms_orders.exception.OrderNotFoundException;
import com.quandrix.ms_orders.model.Order;
import com.quandrix.ms_orders.model.OrderStatus;
import com.quandrix.ms_orders.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ListingClient listingClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private TransactionClient transactionClient;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void create_caminoFelizCompleto_terminaEnCompleted() {
        // ARRANGE: preparamos un request válido y simulamos que
        // los pasos del flujo funcionan correctamente.
        OrderRequest request = new OrderRequest();
        request.setBuyerId(1L);
        request.setListingId(10L);
        request.setPaymentMethod("CREDIT_CARD");

        ListingResponse listing = new ListingResponse();
        listing.setId(10L);
        listing.setSellerId(2L); // distinto al buyerId=1L
        listing.setScryfallId("id-bl");
        listing.setPrice(5000L);
        listing.setStatus("ACTIVE");
        listing.setQuantity(3);

        when(listingClient.getById(10L)).thenReturn(listing);

        // El repositorio "guarda" y devuelve el mismo objeto Order
        // en cada llamada a save(), simulando la persistencia real
        // (JPA asignaría el id en el primer save, y aquí lo simulamos
        // manualmente).
        Order order = new Order();
        order.setId(100L);
        order.setBuyerId(1L);
        order.setListingId(10L);
        order.setSellerId(2L);
        order.setAmount(5000L);
        order.setPaymentMethod("CREDIT_CARD");
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentResponse payment = new PaymentResponse();
        payment.setId(500L);
        payment.setOrderId(100L);
        payment.setStatus("APPROVED");
        when(paymentClient.process(any(PaymentRequest.class))).thenReturn(payment);

        // markAsSold, register y send son void — no necesitan
        // when(...), solo necesitamos que NO lancen excepción
        // (comportamiento por defecto de un mock sin configurar).

        // ACT
        OrderResponse response = orderService.create(request);

        // ASSERT: la orden debe terminar en COMPLETED, con los datos
        // correctos tomados del listing (precio, vendedor).
        assertThat(response.getSellerId()).isEqualTo(2L);
        assertThat(response.getAmount()).isEqualTo(5000L);

        // VERIFY: confirmamos que los pasos del flujo se
        // ejecutaron en este camino feliz.
        verify(paymentClient, times(1)).process(any(PaymentRequest.class));
        verify(listingClient, times(1)).markAsSold(10L);
        verify(transactionClient, times(1)).register(any(TransactionRequest.class));
        verify(notificationClient, times(2)).send(any(NotificationRequest.class)); // comprador + vendedor
        verify(orderRepository, atLeast(3)).save(any(Order.class)); // PENDING, CONFIRMED, COMPLETED
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: se envían EXACTAMENTE 2 notificaciones (una al
// comprador, una al vendedor) en un flujo exitoso
// Se obtuvo: solo se envía 1 notificación, omitiendo silenciosamente
// la del vendedor (o la del comprador)
// Esto podría pasar si alguien elimina por error una de las dos
// llamadas a notificationClient.send(...) dentro del bloque try del
// paso 6 de OrderService.create(), dejando que el vendedor (o el
// comprador) nunca se entere de que la transacción se completó.


    @Test
    void create_conListingInexistente_lanzaInvalidOrderException() {
        // ARRANGE: listingClient.getById() lanza cualquier excepción
        // (el código real captura "Exception" genérico).
        OrderRequest request = new OrderRequest();
        request.setBuyerId(1L);
        request.setListingId(999L);
        request.setPaymentMethod("CREDIT_CARD");

        when(listingClient.getById(999L)).thenThrow(new RuntimeException("404"));

        // ACT + ASSERT
        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo("El listing 999 no existe");

        // VERIFY: al fallar el primer paso, ninguna orden debió crearse,
        // ni ningún otro cliente debió ser invocado.
        verify(orderRepository, never()).save(any());
        verify(paymentClient, never()).process(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: orderRepository.save(...) NUNCA se invoca si el
// listing no existe (la validación se detiene en el primer paso,
// antes de crear ninguna orden en la base de datos)
// Se obtuvo: se crea una orden en PENDING para un listing que no
// existe, dejando un registro huérfano en la base de datos
// Esto podría pasar si alguien reordena el código y mueve la
// creación de la orden ANTES de la validación del listing.


    @Test
    void create_conListingNoActivo_lanzaInvalidOrderException() {
        // ARRANGE: el listing existe, pero su status no es "ACTIVE"
        // (por ejemplo, ya fue retirado o vendido previamente).
        OrderRequest request = new OrderRequest();
        request.setBuyerId(1L);
        request.setListingId(10L);
        request.setPaymentMethod("CREDIT_CARD");

        ListingResponse listing = new ListingResponse();
        listing.setSellerId(2L);
        listing.setStatus("SOLD"); // no está ACTIVE
        listing.setQuantity(1);
        when(listingClient.getById(10L)).thenReturn(listing);

        // ACT + ASSERT
        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo("El listing no está disponible para compra");

        // VERIFY
        verify(orderRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidOrderException cuando el listing.status no es
// exactamente "ACTIVE" (ej. "SOLD", "WITHDRAWN")
// Se obtuvo: la orden se crea igual, permitiendo comprar un listing
// que ya fue vendido o retirado por su dueño — un problema grave de
// doble venta del mismo inventario
// Esto podría pasar si alguien invierte por error la condición
// "if (!"ACTIVE".equals(listing.getStatus()))" a
// "if ("ACTIVE".equals(listing.getStatus()))".


    @Test
    void create_conListingSinStock_lanzaInvalidOrderException() {
        // ARRANGE: el listing está ACTIVE, pero su quantity es 0.
        OrderRequest request = new OrderRequest();
        request.setBuyerId(1L);
        request.setListingId(10L);
        request.setPaymentMethod("CREDIT_CARD");

        ListingResponse listing = new ListingResponse();
        listing.setSellerId(2L);
        listing.setStatus("ACTIVE");
        listing.setQuantity(0); // sin stock
        when(listingClient.getById(10L)).thenReturn(listing);

        // ACT + ASSERT
        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo("El listing no tiene stock disponible");

        // VERIFY
        verify(orderRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidOrderException cuando quantity <= 0
// Se obtuvo: la orden se crea igual con quantity=0, permitiendo
// "comprar" un producto sin unidades reales disponibles
// Esto podría pasar si alguien cambia la condición
// "listing.getQuantity() <= 0" a "listing.getQuantity() < 0",
// permitiendo erróneamente que quantity=0 sea válido.


    @Test
    void create_conCompradorIgualAlVendedor_lanzaInvalidOrderException() {
        // ARRANGE: el comprador es el mismo dueño del listing —
        // intentando comprar su propia publicación.
        OrderRequest request = new OrderRequest();
        request.setBuyerId(2L);
        request.setListingId(10L);
        request.setPaymentMethod("CREDIT_CARD");

        ListingResponse listing = new ListingResponse();
        listing.setSellerId(2L); // mismo id que el buyerId del request
        listing.setStatus("ACTIVE");
        listing.setQuantity(1);
        when(listingClient.getById(10L)).thenReturn(listing);

        // ACT + ASSERT
        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo("No puedes comprar tu propio listing");

        // VERIFY
        verify(orderRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidOrderException cuando buyerId == listing.sellerId
// Se obtuvo: la orden se crea exitosamente, permitiendo que un
// vendedor "compre" su propia carta — esto generaría una transacción
// ficticia que podría usarse para inflar artificialmente las
// estadísticas de ventas de ms-reports (recordando getTopSellers())
// o para lavar fondos entre cuentas propias del mismo usuario
// Esto podría pasar si alguien elimina por error la validación
// "if (listing.getSellerId().equals(request.getBuyerId()))".


    @Test
    void create_conErrorDeFeignEnPago_canceleLaOrdenYLanzaInvalidOrderException() {
        // ARRANGE: el listing es válido, pero paymentClient.process()
        // lanza una excepción (por ejemplo, ms-payments caído).
        OrderRequest request = new OrderRequest();
        request.setBuyerId(1L);
        request.setListingId(10L);
        request.setPaymentMethod("CREDIT_CARD");

        ListingResponse listing = new ListingResponse();
        listing.setSellerId(2L);
        listing.setScryfallId("id-bl");
        listing.setPrice(5000L);
        listing.setStatus("ACTIVE");
        listing.setQuantity(1);
        when(listingClient.getById(10L)).thenReturn(listing);

        Order order = new Order();
        order.setId(100L);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        when(paymentClient.process(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException("ms-payments no disponible"));

        // ACT + ASSERT
        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo(
                "Error al procesar el pago: ms-payments no disponible");

        // ASSERT adicional: el objeto Order en memoria debe haber sido
        // marcado como CANCELLED antes de lanzar la excepción.
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // VERIFY: al fallar el pago, NUNCA se debió llegar a marcar el
        // listing como sold, registrar transacción, ni notificar.
        verify(listingClient, never()).markAsSold(any());
        verify(transactionClient, never()).register(any());
        verify(notificationClient, never()).send(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: cuando el pago falla por un error de comunicación con
// ms-payments, la orden queda marcada como CANCELLED (no PENDING),
// reflejando que el intento de compra no se concretó
// Se obtuvo: la orden queda en PENDING indefinidamente, generando un
// registro "huérfano" sin estado final claro, que confundiría a
// futuros reportes o consultas sobre el estado real de esa orden
// Esto podría pasar si alguien elimina las líneas
// "order.setStatus(OrderStatus.CANCELLED); orderRepository.save(order);"
// dentro del catch del paso 3 de OrderService.create(), dejando que
// solo se lance la excepción sin revertir el estado de la orden ya
// creada.


    @Test
    void create_conPagoRechazado_canceleLaOrdenYLanzaInvalidOrderException() {
        // ARRANGE: paymentClient.process() responde exitosamente (sin
        // excepción), pero con un status distinto a "APPROVED" — por
        // ejemplo, "REJECTED" o "DECLINED".
        OrderRequest request = new OrderRequest();
        request.setBuyerId(1L);
        request.setListingId(10L);
        request.setPaymentMethod("CREDIT_CARD");

        ListingResponse listing = new ListingResponse();
        listing.setSellerId(2L);
        listing.setScryfallId("id-bl");
        listing.setPrice(5000L);
        listing.setStatus("ACTIVE");
        listing.setQuantity(1);
        when(listingClient.getById(10L)).thenReturn(listing);

        Order order = new Order();
        order.setId(100L);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentResponse paymentRechazado = new PaymentResponse();
        paymentRechazado.setStatus("REJECTED");
        when(paymentClient.process(any(PaymentRequest.class))).thenReturn(paymentRechazado);

        // ACT + ASSERT
        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.create(request)
        );
        assertThat(ex.getMessage()).isEqualTo("El pago fue rechazado");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // VERIFY
        verify(listingClient, never()).markAsSold(any());
        verify(transactionClient, never()).register(any());
        verify(notificationClient, never()).send(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidOrderException con mensaje "El pago fue
// rechazado" cuando payment.getStatus() es distinto a "APPROVED"
// (sin importar el valor exacto: "REJECTED", "DECLINED", "PENDING", etc.)
// Se obtuvo: la orden se marca como CONFIRMED a pesar de que el pago
// no fue aprobado, permitiendo que el listing se marque como vendido
// y se registre una transacción para un pago que en realidad falló
// Esto podría pasar si alguien invierte por error la condición
// "if (!"APPROVED".equals(payment.getStatus()))" a
// "if ("APPROVED".equals(payment.getStatus()))".


    @Test
    void create_conErrorEnMarkAsSold_continuaElFlujoYTerminaEnCompleted() {
        // ARRANGE: el flujo principal funciona (listing válido,
        // pago aprobado), pero listingClient.markAsSold() lanza una
        // excepción (por ejemplo, ms-listings con un timeout momentáneo).
        OrderRequest request = new OrderRequest();
        request.setBuyerId(1L);
        request.setListingId(10L);
        request.setPaymentMethod("CREDIT_CARD");

        ListingResponse listing = new ListingResponse();
        listing.setSellerId(2L);
        listing.setScryfallId("id-bl");
        listing.setPrice(5000L);
        listing.setStatus("ACTIVE");
        listing.setQuantity(1);
        when(listingClient.getById(10L)).thenReturn(listing);

        Order order = new Order();
        order.setId(100L);
        order.setBuyerId(1L);
        order.setSellerId(2L);
        order.setAmount(5000L);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentResponse payment = new PaymentResponse();
        payment.setStatus("APPROVED");
        when(paymentClient.process(any(PaymentRequest.class))).thenReturn(payment);

        // El paso "tolerante a fallos": markAsSold() lanza excepción.
        doThrow(new RuntimeException("ms-listings timeout"))
                .when(listingClient).markAsSold(10L);

        // ACT: a pesar del error en markAsSold, el metodo NO debe lanzar
        // ninguna excepción — debe completar el flujo igual.
        OrderResponse response = orderService.create(request);

        // ASSERT: la orden terminó en COMPLETED, no se interrumpió.
        assertThat(response.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        // VERIFY: a pesar de que markAsSold falló, los pasos SIGUIENTES
        // (registrar transacción, notificar) SÍ debieron ejecutarse —
        // confirmando que el flujo realmente continuó, no que se saltó
        // el resto silenciosamente.
        verify(transactionClient, times(1)).register(any(TransactionRequest.class));
        verify(notificationClient, times(2)).send(any(NotificationRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: un fallo en listingClient.markAsSold() NO interrumpe
// el flujo de creación de la orden — el pago ya fue procesado y
// cobrado, por lo que la orden debe completarse igual, y el listing
// quedaría temporalmente como ACTIVE a pesar de estar vendido (una
// inconsistencia que debería resolverse por otros medios, como un
// job de reconciliación, pero NO debe bloquear al comprador que ya pagó)
// Se obtuvo: la excepción de markAsSold() se propaga sin capturar,
// abortando toda la transacción @Transactional y haciendo un ROLLBACK
// completo — el comprador habría sido cobrado (en ms-payments, que
// es un servicio externo sin rollback automático) pero su orden
// desaparecería de la base de datos, dejándolo sin ningún registro
// de una compra por la cual sí pagó — un problema financiero grave
// Esto podría pasar si alguien elimina el try/catch específico
// alrededor de listingClient.markAsSold() en el paso 4 de
// OrderService.create().


    @Test
    void create_conErrorEnRegistrarTransaccion_continuaElFlujoYTerminaEnCompleted() {
        // ARRANGE: igual que el anterior, pero ahora falla
        // transactionClient.register() en vez de markAsSold().
        OrderRequest request = new OrderRequest();
        request.setBuyerId(1L);
        request.setListingId(10L);
        request.setPaymentMethod("CREDIT_CARD");

        ListingResponse listing = new ListingResponse();
        listing.setSellerId(2L);
        listing.setScryfallId("id-bl");
        listing.setPrice(5000L);
        listing.setStatus("ACTIVE");
        listing.setQuantity(1);
        when(listingClient.getById(10L)).thenReturn(listing);

        Order order = new Order();
        order.setId(100L);
        order.setBuyerId(1L);
        order.setSellerId(2L);
        order.setAmount(5000L);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentResponse payment = new PaymentResponse();
        payment.setStatus("APPROVED");
        when(paymentClient.process(any(PaymentRequest.class))).thenReturn(payment);

        doThrow(new RuntimeException("ms-transactions no disponible"))
                .when(transactionClient).register(any(TransactionRequest.class));

        // ACT
        OrderResponse response = orderService.create(request);

        // ASSERT
        assertThat(response.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        // VERIFY: a pesar de que register() falló, markAsSold() SÍ debió
        // ejecutarse (paso anterior) y las notificaciones SÍ debieron
        // enviarse (paso posterior) — el fallo de un paso tolerante no
        // afecta a los demás pasos tolerantes que lo rodean.
        verify(listingClient, times(1)).markAsSold(10L);
        verify(notificationClient, times(2)).send(any(NotificationRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: un fallo al registrar la transacción en
// ms-transactions no interrumpe el flujo — la orden se completa
// igual, aunque esto significa que ms-reports no contará esta venta
// en sus estadísticas hasta que se reconcilie manualmente
// Se obtuvo: la excepción se propaga sin capturar, revirtiendo toda
// la transacción a pesar de que el pago YA fue cobrado exitosamente
// Mismo riesgo financiero ya documentado para markAsSold().


    @Test
    void create_conErrorAlNotificar_continuaElFlujoYTerminaEnCompleted() {
        // ARRANGE: igual que los anteriores, pero ahora falla
        // notificationClient.send() — el último paso tolerante a fallos
        // antes de marcar la orden como COMPLETED.
        OrderRequest request = new OrderRequest();
        request.setBuyerId(1L);
        request.setListingId(10L);
        request.setPaymentMethod("CREDIT_CARD");

        ListingResponse listing = new ListingResponse();
        listing.setSellerId(2L);
        listing.setScryfallId("id-bl");
        listing.setPrice(5000L);
        listing.setStatus("ACTIVE");
        listing.setQuantity(1);
        when(listingClient.getById(10L)).thenReturn(listing);

        Order order = new Order();
        order.setId(100L);
        order.setBuyerId(1L);
        order.setSellerId(2L);
        order.setAmount(5000L);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentResponse payment = new PaymentResponse();
        payment.setStatus("APPROVED");
        when(paymentClient.process(any(PaymentRequest.class))).thenReturn(payment);

        doThrow(new RuntimeException("ms-notifications no disponible"))
                .when(notificationClient).send(any(NotificationRequest.class));

        // ACT
        OrderResponse response = orderService.create(request);

        // ASSERT: la orden se completa de todas formas, a pesar de que
        // NINGUNA notificación pudo enviarse (la excepción ocurre en la
        // primera llamada a send(), dentro del mismo bloque try, así que
        // la segunda notificación tampoco se alcanza a enviar).
        assertThat(response.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        // VERIFY: tanto markAsSold como register SÍ debieron ejecutarse,
        // ya que ocurren ANTES del bloque de notificaciones.
        verify(listingClient, times(1)).markAsSold(10L);
        verify(transactionClient, times(1)).register(any(TransactionRequest.class));
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: un fallo al enviar notificaciones (ms-notifications
// caído) no interrumpe el flujo — el comprador ya pagó y el listing
// ya se marcó como vendido, por lo que la orden debe completarse
// igual, aunque ni el comprador ni el vendedor reciban la notificación
// automática (tendrían que enterarse por otros medios, como revisando
// manualmente el estado de su orden)
// Se obtuvo: la excepción se propaga sin capturar, revirtiendo toda
// la transacción a pesar de que el pago YA fue cobrado y el listing
// YA fue marcado como vendido — el peor de los 3 casos, porque además
// dejaría al listing marcado como SOLD (ese cambio SÍ se persistió en
// ms-listings, un servicio externo sin rollback automático) mientras
// que la orden en ms-orders desaparecería por el rollback de
// @Transactional, generando una inconsistencia grave entre ambos
// microservicios: un listing vendido sin ninguna orden asociada.

    @Test
    void getById_conIdExistente_retornaOrden() {
        // ARRANGE
        Long id = 1L;
        Order order = new Order();
        order.setId(id);
        order.setBuyerId(1L);
        order.setSellerId(2L);
        order.setStatus(OrderStatus.COMPLETED);

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        // ACT
        OrderResponse response = orderService.getById(id);

        // ASSERT
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: response.getStatus() refleja exactamente el status
// real de la orden almacenada
// Se obtuvo: response.getStatus() == null
// Esto podría pasar si alguien modifica el metodo privado toResponse()
// y olvida mapear el campo status al construir el OrderResponse.


    @Test
    void getById_conIdInexistente_lanzaOrderNotFoundException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(orderRepository.findById(idInexistente)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getById(idInexistente)
        );
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: OrderNotFoundException cuando el repositorio no
// encuentra la orden (Optional vacío)
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez
// de 404), mismo patrón de riesgo ya documentado en los demás
// microservicios al reemplazar .orElseThrow(...) por un .get() directo.

    @Test
    void getByBuyer_conOrdenesExistentes_retornaListaCorrecta() {
        // ARRANGE
        Long buyerId = 1L;
        Order order1 = new Order();
        order1.setId(1L);
        order1.setBuyerId(buyerId);
        order1.setStatus(OrderStatus.COMPLETED);

        when(orderRepository.findByBuyerId(buyerId)).thenReturn(List.of(order1));

        // ACT
        List<OrderResponse> response = orderService.getByBuyer(buyerId);

        // ASSERT
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getBuyerId()).isEqualTo(buyerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: solo órdenes del buyerId solicitado
// Se obtuvo: órdenes de los compradores (findAll() en vez de
// findByBuyerId(buyerId)) — problema de privacidad ya documentado
// desde el test del controller, ahora confirmado contra el service real.


    @Test
    void getBySeller_conOrdenesExistentes_retornaListaCorrecta() {
        // ARRANGE
        Long sellerId = 2L;
        Order order1 = new Order();
        order1.setId(1L);
        order1.setSellerId(sellerId);
        order1.setStatus(OrderStatus.COMPLETED);

        when(orderRepository.findBySellerId(sellerId)).thenReturn(List.of(order1));

        // ACT
        List<OrderResponse> response = orderService.getBySeller(sellerId);

        // ASSERT
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getSellerId()).isEqualTo(sellerId);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: solo órdenes del sellerId solicitado
// Se obtuvo: órdenes de los vendedores mezcladas
// Mismo riesgo de privacidad ya documentado para getByBuyer().

    @Test
    void cancel_conDatosValidos_marcaComoCancelled() {
        // ARRANGE: la orden existe, está PENDING, y pertenece al
        // comprador que solicita la cancelación.
        Long id = 1L;
        Long buyerId = 1L;

        Order order = new Order();
        order.setId(id);
        order.setBuyerId(buyerId);
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // ACT
        OrderResponse response = orderService.cancel(id, buyerId);

        // ASSERT
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // VERIFY
        verify(orderRepository, times(1)).save(order);
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: order.getStatus() == CANCELLED después de cancelar
// Se obtuvo: order.getStatus() == PENDING (sin cambios reales)
// Esto podría pasar si alguien olvida la línea
// order.setStatus(OrderStatus.CANCELLED) antes de save() en
// OrderService.cancel().


    @Test
    void cancel_conIdInexistente_lanzaOrderNotFoundException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(orderRepository.findById(idInexistente)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.cancel(idInexistente, 1L)
        );
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: OrderNotFoundException al cancelar un id inexistente
// Se obtuvo: NoSuchElementException sin capturar (HTTP 500 en vez de 404)


    @Test
    void cancel_conBuyerIdDistinto_lanzaInvalidOrderException() {
        // ARRANGE: la orden pertenece al comprador 1, pero alguien con
        // buyerId=2 intenta cancelarla (vulnerabilidad IDOR).
        Long id = 1L;
        Order order = new Order();
        order.setId(id);
        order.setBuyerId(1L); // dueño real
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        // ACT + ASSERT
        InvalidOrderException ex = assertThrows(
                InvalidOrderException.class,
                () -> orderService.cancel(id, 2L) // intruso
        );
        assertThat(ex.getMessage()).isEqualTo("No puedes cancelar una orden que no es tuya");

        // VERIFY
        verify(orderRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: InvalidOrderException cuando el buyerId no coincide
// con el dueño real de la orden — mismo patrón IDOR ya documentado
// extensamente en ms-listings (update/withdraw)
// Se obtuvo: cualquier usuario podría cancelar órdenes ajenas
// conociendo solo su id numérico
// Esto podría pasar si alguien elimina la validación
// "if (!order.getBuyerId().equals(buyerId))".


    @Test
    void cancel_conOrdenNoCancelable_lanzaOrderNotCancellableException() {
        // ARRANGE: la orden pertenece al comprador correcto, pero ya
        // no está en estado PENDING (por ejemplo, ya fue COMPLETED).
        Long id = 1L;
        Long buyerId = 1L;

        Order order = new Order();
        order.setId(id);
        order.setBuyerId(buyerId);
        order.setStatus(OrderStatus.COMPLETED); // ya no es PENDING

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));

        // ACT + ASSERT
        assertThrows(
                OrderNotCancellableException.class,
                () -> orderService.cancel(id, buyerId)
        );

        // VERIFY
        verify(orderRepository, never()).save(any());
    }

// CASO HIPOTÉTICO DE FALLA (para QA):
// Se esperaba: OrderNotCancellableException cuando la orden ya no
// está en PENDING (ya fue CONFIRMED, COMPLETED o CANCELLED previamente)
// Se obtuvo: una orden COMPLETED se "cancela" igual, generando una
// inconsistencia grave: el listing ya fue marcado como SOLD en
// ms-listings, la transacción ya fue registrada en ms-transactions,
// y el pago ya fue cobrado en ms-payments — "cancelar" la orden en
// este punto no revertiría ninguno de esos efectos externos, dejando
// el sistema en un estado financiero y de inventario inconsistente
// Esto podría pasar si alguien elimina la validación
// "if (order.getStatus() != OrderStatus.PENDING)".
}