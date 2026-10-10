# Arquitectura y recorridos

Navegador → controladores Spring MVC → servicios transaccionales → Spring Data JPA / EntityManager → MySQL.
Thymeleaf produce HTML en el servidor. JavaScript mejora formularios, estados de envío y mostrar contraseña; nunca conecta a MySQL.
phpMyAdmin conecta solo a MySQL para administración; no participa en la navegación del portal.

Paquetes:
- domain: entidades con mapeos JPA y claves foráneas escalares; las FK reales están en SQL.
- data: repositorios Spring Data y operaciones de persistencia / bloqueo.
- service: contratación, revisiones, identidad, archivos, avisos y datos demo.
- web: controladores públicos, cliente, administrador y descarga autorizada.
- config: seguridad, inicializadores por perfil.
- resources/templates y static: interfaz Thymeleaf y estilos adaptados del código original.

## Seguridad y concurrencia
Spring Security requiere CLIENTE en /account y /checkout, ADMIN en /admin, sesión autenticada en /files. Las descargas y detalles comprueban propietario o rol en backend. CSRF permanece activo y Thymeleaf inserta el token en POST, incluidos logout y multipart.

BCrypt con coste 12 protege contraseñas. Recuperación usa 32 bytes aleatorios; solo se guarda SHA-256, caducidad y uso. No se guardan PAN/CVV. No hay logs de tokens, contraseñas ni datos bancarios. Los tipos permitidos de archivo se comprueban por MIME y firma, y PNG/JPEG se decodifican; máximo 5 MB. Se almacenan con UUID fuera de static. Los descargables tienen Content-Disposition attachment y Cache-Control no-store.

Los pagos usan clave de idempotencia emitida por el servidor y UNIQUE. Se serializan las operaciones de un socio bloqueando su usuario. Al aplicar una confirmación se lee la membresía con PESSIMISTIC_WRITE (lectura actual también bajo REPEATABLE READ de MySQL). Estado del pago, actualización de membresía, revisión, auditoría y reprogramación se confirman en la misma transacción. Los lectores de recordatorios toman también preferencias y membresía con bloqueo: una instantánea anterior no puede reactivar el consentimiento revocado.

Spring no confía en el precio enviado por un navegador: el resumen se guarda en la sesión con la instantánea seleccionada. Una segunda operación con otro identificador es otra contratación; duplicados de un mismo envío reutilizan su resultado. Los números de operación manual tienen UNIQUE(method,operation_number).

## Integraciones
Tarjeta CARD_TEST: simulación explícita, confirmación inmediata sin cobro.
WALLET/TRANSFER: registro de comprobante y revisión persistente por administrador, sin conexión bancaria ni conciliación automática.
WhatsAppProvider: contrato de integración. SimulatedWhatsAppProvider devuelve SIMULATED sin red. BusinessWhatsAppProvider necesita token, phone-id, versión Graph y plantilla aprobada es con tres parámetros: nombre, vencimiento y días. SENT indica aceptación HTTP por Meta, no confirmación de entrega al teléfono; no hay webhook de entrega. Fallos quedan FAILED, sin reintentos automáticos para no duplicar envíos inciertos. Una caída entre aceptación externa y commit necesita reconciliación antes de reintentar: una transacción SQL no garantiza exactly-once en un tercero.
SMTP: JavaMailSender cuando se configura host. Sin SMTP, dev/test muestra enlace de recuperación solo en la respuesta de ese navegador; producción no lo expone.

## Referencias técnicas consultadas
- [Requisitos Spring Boot 4.0](https://docs.spring.io/spring-boot/4.0/system-requirements.html): Java 21 como objetivo; verificado localmente con Java 26.
- [Dependencias administradas Spring Boot](https://docs.spring.io/spring-boot/4.0/appendix/dependency-versions/coordinates.html).
- [Formato de plantillas de WhatsApp publicado por Meta](https://whatsapp.github.io/WhatsApp-Nodejs-SDK/api-reference/messages/template/). El SDK enlazado es referencia del formato; el adaptador Java usa HTTP y una versión Graph configurable.

## Límites de operación
El servidor conserva sesiones en memoria: reiniciar requiere ingresar otra vez; contratos, pagos, preferencias, revisiones y archivos persisten. Varios nodos de aplicación requerirían sesión compartida para continuar el asistente entre nodos. El perfil dev usa la instancia local de MariaDB; las pruebas usan H2 en memoria por defecto. Para despliegue público se requiere proxy HTTPS, COOKIE_SECURE=true, secretos propios y desactivar perfil dev.
