# INTERACCI-N-HOMBRE-MAQUINA

**Imperio Gym · Portal Spring Boot**

Aplicación funcional basada en el código del prototipo adjunto. Portal público, cuenta del cliente y administración comparten la misma base de datos. Interfaz en español, escritorio y móvil.

## phpMyAdmin y MySQL local

Con XAMPP iniciado, abre phpMyAdmin en http://localhost/phpmyadmin/. En desarrollo, la aplicación conecta a MariaDB local, a la base `imperio`; las tablas y pruebas aparecen ahí. Flyway crea las tablas automáticamente cuando la base está vacía.

El usuario de la aplicación y su contraseña se configuran localmente en `.env.local`, ignorado por Git y cargado solo en el perfil `dev`. Configura `DB_URL`, `DB_USER` y `DB_PASSWORD` para un usuario con acceso solo a `imperio`; no uses `root`. El archivo sigue el formato:

```powershell
DB_URL=jdbc:mariadb://127.0.0.1:3306/imperio?serverTimezone=UTC
DB_USER=imperio_app
DB_PASSWORD=tu-clave-local
```

Para conectar una base preexistente con tablas creadas manualmente desde V1–V3, Flyway debe registrarlas como línea base en la versión 3 antes de iniciar; así no se vuelven a ejecutar sobre tablas existentes.

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

## Credenciales ficticias (perfil dev)

| Rol | Correo | DNI | Contraseña inicial |
|---|---|---|---|
| CLIENTE | juan.perez@ejemplo.com | 10000001 | Imperio2026! |
| CLIENTE | ana.torres@ejemplo.com | 10000002 | Imperio2026! |
| ADMIN | admin@imperio.test | 10000000 | Imperio2026! |

DEMO_PASSWORD permite cambiar la contraseña inicial antes del primer arranque. Cambiar esa variable no reemplaza hashes ya guardados. Juan tiene una membresía de tarjeta simulada; Ana empieza sin membresía. Solo ADMIN puede entrar a /admin. El DNI sirve para acceder junto con una contraseña, nunca como contraseña.

## Ejecución local

Requisitos: JDK 21 o posterior compatible con Spring Boot 4.0 (verificado con JDK 26), conexión inicial para Maven Wrapper. No necesitas instalar Node para usar la aplicación.

Al clonar el repositorio, copia `.env.example` a `.env` y configura tus credenciales de base de datos en `.env.local`, según el ejemplo anterior. Ambos archivos locales están excluidos de Git.

```powershell
Copy-Item .env.example .env
```

```powershell
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Linux/macOS: `chmod +x mvnw && ./mvnw verify`, luego `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.

El perfil `dev` usa MariaDB de XAMPP. El perfil de pruebas usa H2 en memoria por defecto. El inicio local expone http://localhost:8080. Para usar MySQL 8, cambia DB_URL por `jdbc:mysql://127.0.0.1:3306/imperio?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true` y configura DB_USER y DB_PASSWORD en `.env.local`; el controlador MySQL también está incluido.

Nunca apuntes la suite de pruebas a una base con datos reales.

También puedes construir con `mvnw.cmd package` y ejecutar:
`java -jar target/imperio-gym-1.0.0.jar --spring.profiles.active=dev`.

## Qué funciona

- Inicio, servicios, sedes y planes conservan identidad, contenido y precios del prototipo: S/ 89.99, S/ 114.99 y S/ 179.99.
- Registro con validación, acceso por correo/DNI, BCrypt, mostrar contraseña y recuperación con token temporal de un uso.
- Selección de plan conservada durante registro e ingreso; resumen, meses calendario y fechas propuestas antes de pagar.
- Selección obligatoria de sede al pagar, visible en el contrato y la cuenta; el administrador consulta socios activos por sede y filtra clientes y pagos. Ver [reglas de sedes](docs/SEDES.md).
- Control administrativo de entradas/salidas en `/admin/access`, únicamente para socios con membresía vigente y asignada a esa sede. El aforo inicial es de 30 personas por sede; al llenarse se bloquean nuevas entradas. Cada movimiento queda en auditoría.
- Tarjeta de prueba; pagos Yape/Plin/transferencia con referencia y archivo privado, pendientes de revisión.
- Membresía, renovación/cambio, historial de pagos, solicitudes y preferencias persistentes.
- Revisión administrativa, rechazo con motivo, historial de actor/fecha, filtros y paginación, edición/desactivación de planes sin alterar contratos.
- Consentimiento WhatsApp desactivado inicialmente; recordatorios 7/1 días, cancelación de los anteriores al renovar, simulador y adaptador configurable.
- Reinicio de las operaciones de las cuentas marcadas demo desde ADMIN → Dashboard → Herramientas de desarrollo, escribiendo REINICIAR DEMO. Solo existe con perfil dev; conserva usuarios registrados, catálogo y auditoría. Los archivos demo desvinculados permanecen privados en disco y pueden retirarse con una política de retención explícita.
- CSRF, autorizaciones por rol/propietario, validación de archivos y transacciones con bloqueo para evitar dobles aplicaciones.

## Simulaciones y credenciales externas

La **tarjeta siempre es de prueba**: no hay pasarela real, no solicita ni guarda PAN/CVV. Los pagos manuales registran una operación declarada y su comprobante; no consultan al banco, Yape o Plin, ni se incluyen cuentas de cobro inventadas.

WhatsApp usa `WHATSAPP_PROVIDER=simulated` por defecto: no envía mensajes. Para Business se requieren WHATSAPP_TOKEN, WHATSAPP_PHONE_ID, WHATSAPP_API_VERSION y WHATSAPP_TEMPLATE, una plantilla aprobada en español con parámetros nombre/fecha/días y consentimiento vigente. El adaptador no ha sido verificado con credenciales reales. SENT significa aceptación de la API, no entrega confirmada; no hay webhook. No reintenta automáticamente errores o resultados inciertos.

Recuperación: con SMTP_HOST/PORT/USER/PASSWORD y MAIL_FROM utiliza correo; sin SMTP el perfil dev muestra un enlace de desarrollo en el mismo navegador. En producción no expone ese enlace. El envío SMTP real no se probó con un proveedor externo.

Fuentes de Google Fonts se cargan opcionalmente desde Internet; hay tipografías de respaldo. La fotografía del prototipo está incluida localmente.

## Políticas y datos

Leer [POLITICAS.md](docs/POLITICAS.md) antes de habilitar efectos de congelamiento:
- NO_EXTENSION, predeterminada: aprobar no amplía fechas.
- EXTEND_DAYS: propuesta configurable que suma días inclusivos después de revisión; no representa una política comercial acordada.
- Renovación desde max(hoy Lima, vencimiento actual), suma única de meses calendario, fin exclusivo. 31/01 + un mes termina el último día de febrero.
- Precio, nombre, meses y beneficios del contrato se preservan al editar catálogo.
- Fecha final de la operación, días de extensión y fecha de aviso se calculan desde los hechos guardados para evitar redundancias.

[Diagrama ER](docs/ER.svg) · [Mermaid editable](docs/ER.mmd) · [Diccionario, claves e índices](docs/DICCIONARIO.md) · [Análisis 1FN–5FN](docs/NORMALIZACION.md) · [Arquitectura](docs/ARQUITECTURA.md).

## Pruebas

```powershell
.\mvnw.cmd verify
```

La suite usa H2 en memoria por defecto. Para MySQL, definir TEST_DB_URL, TEST_DB_USER y TEST_DB_PASSWORD con una base exclusiva de pruebas, y ejecutar el mismo comando. Flyway aplica V1–V5; Hibernate valida el esquema.

Incluye pruebas de integración para recorridos cliente/administrador, idempotencia, concurrencia, permisos y CSRF, archivos, calendario, recuperación, políticas de congelamiento, precio histórico, recordatorios y control de entradas/salidas con aforo por sede.

Pruebas de navegador opcionales (Node y Microsoft Edge instalados):
```powershell
npm install --prefix .tools/qa playwright mermaid --no-audit --no-fund
$env:BASE_URL="http://localhost:8080"
node scripts/browser-check.cjs
# Reiniciar aplicación conservando base y uploads antes del siguiente comando:
node scripts/persistence-check.cjs
```

Estos scripts crean cuentas ficticias en la instancia indicada; usar solo desarrollo. Guardan una credencial ficticia temporal en .tools, excluida del ZIP. Capturas en docs/screenshots. [Resultados ejecutados y límites](docs/VALIDACION.md).

## Configuración fuera de desarrollo

Usar SPRING_PROFILES_ACTIVE=prod, MySQL, secretos propios, HTTPS y COOKIE_SECURE=true. No se crean cuentas demo ni existe el endpoint de reinicio. En una base nueva, BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_DNI y BOOTSTRAP_ADMIN_PASSWORD crean el primer administrador; después retirar esas variables. DNI de 8 dígitos y contraseña entre 10 y 72 caracteres. PORT, PUBLIC_URL, UPLOAD_DIR y parámetros SMTP/WhatsApp son configurables.

Las sesiones son en memoria: reiniciar requiere iniciar sesión otra vez. Para varias instancias se necesita una sesión compartida. La base y los comprobantes sí persisten. Revisar las limitaciones de entrega externa de mensajes en la arquitectura.

## Estructura

```text
src/main/java/pe/imperio/gym/   configuración, dominio, persistencia, servicios, MVC
src/main/resources/           Thymeleaf, CSS/JS, configuración y Flyway
src/test/                     pruebas de integración
prototype/                    código visual original conservado como referencia
docs/                         políticas, modelo, normalización y evidencias
scripts/                      validación de navegador y generación de documentación
.mvn/, mvnw*                   Maven Wrapper
```
