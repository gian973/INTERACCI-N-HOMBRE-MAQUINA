# Verificación ejecutada · 10/10/2026

Entorno: Windows 11, Java 26.0.2 (compilación objetivo Java 21), Maven Wrapper 3.9.11, MySQL Community 8.0.46 en instancia aislada ligada a 127.0.0.1:3377, H2 de pruebas y Microsoft Edge sin ventana.

## Resultados automatizados

- Maven verify con H2: **14 pruebas, 0 fallos, 0 errores, 0 omitidas**, BUILD SUCCESS.
- Maven test con MySQL 8.0.46 y MariaDB 10.4.32: **14 pruebas por motor, 0 fallos, 0 errores, 0 omitidas**, BUILD SUCCESS.
- Arranque real en XAMPP/MariaDB, Flyway reconoció la línea base 3, Hibernate validó el esquema y la aplicación respondió HTTP 200 en localhost:8080.
- Flyway V1–V3 aplicado y Hibernate ddl-auto=validate correcto en ambos motores.
- Maven Wrapper ejecutado y distribución comprobada mediante SHA-256.
- [Conteos y nombres de las pruebas](test-results.json).

Cobertura: meses calendario (31 de enero y año bisiesto), selección conservada durante registro/login, pago simulado idempotente, aprobación/rechazo manual, renovación pendiente sin cambios, aprobación única, solicitudes con revisión y resolución visible, superposición/fechas/documentos inválidos, consentimiento/reprogramación/cancelación, permisos por rol/propietario, CSRF, descarga autorizada, token hash de un uso y vencido, historial contractual, envíos concurrentes idénticos y aprobaciones concurrentes de operaciones diferentes. La política EXTEND_DAYS se prueba además de NO_EXTENSION.

## Recorridos reales de navegador contra MySQL

| Caso solicitado | Verificación realizada | Resultado |
|---|---|---|
| A | Visitante elige plan, se registra, ingresa por DNI, conserva el precio y envía Yape/Plin con comprobante. | Correcto |
| B | Administrador revisa y confirma; cliente actualiza cuenta y ve membresía activa. | Correcto |
| C | Cliente elige renovación de 2 meses; pendiente conserva fecha; aprobación amplía vigencia y desaparece el botón de aprobación. Pruebas de servidor intentan duplicados/concurrencia. | Correcto |
| D | Cliente envía viaje; administrador pasa a En revisión, aprueba y registra respuesta; cliente la ve. | Correcto |
| E | Activar consentimiento, guardar, comprobar persistencia, desactivar y comprobar nuevamente. | Correcto |
| F | Cliente intenta /admin; otro cliente intenta pago y solicitud ajenos; HTTP 403. Suite también prueba archivo ajeno y ADMIN en /account. | Correcto |

No hubo errores JavaScript de página en los recorridos. Se verificaron filtros administrativos, escritorio 1440 px y móvil 390 px, sin desbordamiento horizontal de la página. Tablas conservan desplazamiento horizontal propio en móvil.

## Reinicio y perfiles

Se detuvo el proceso Spring Boot y se inició otra instancia sobre el mismo MySQL y directorio de archivos. Al volver a ingresar se comprobaron cuenta registrada, vencimiento, pagos confirmados, descarga del comprobante, resolución y consentimiento desactivado. Los datos se conservaron; la sesión se volvió a iniciar, como está documentado.

En dev, ADMIN ejecutó el reinicio de las cuentas marcadas demo. La cuenta creada en el recorrido de navegador y sus operaciones permanecieron intactas. En una instancia con perfil prod, el control no aparece y un POST autorizado con CSRF a /admin/dev/reset devuelve 404.

## Evidencia visual

- [Inicio escritorio](screenshots/inicio-desktop.png)
- [Inicio móvil](screenshots/inicio-mobile.png)
- [Cuenta cliente](screenshots/cliente-cuenta.png)
- [Cuenta móvil](screenshots/cuenta-mobile.png)
- [Dashboard administrador](screenshots/admin-dashboard.png)

## Lo que no se verificó

phpMyAdmin de XAMPP está disponible localmente en http://localhost/phpmyadmin/. La validación automatizada del backend se realizó con H2 y con MySQL real; no incluyó pruebas de interfaz de phpMyAdmin.

No se enviaron correos SMTP reales ni mensajes WhatsApp Business, y no se realizaron cargos bancarios. La tarjeta es exclusivamente simulada; los pagos manuales y sus revisiones son registros reales de la aplicación, sin conciliación externa. Los adaptadores SMTP/WhatsApp requieren credenciales y pruebas con los proveedores correspondientes.

No se probó un proxy HTTPS ni un despliegue público. No se ejecutó una auditoría formal de accesibilidad con lector de pantalla ni una prueba de carga. La interfaz incluye etiquetas persistentes, foco visible, controles nativos, enlace de salto y mensajes de estado.
