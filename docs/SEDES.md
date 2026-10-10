# Suscripciones por sede

Cada pago nuevo exige elegir una sede activa: San Miguel, Pueblo Libre o Magdalena. El servidor valida la selección y guarda el identificador de la sede y su nombre en el pago. El nombre guardado es una instantánea del contrato; el historial conserva ese nombre aunque el catálogo se modifique después.

Al confirmar el pago, la membresía queda asignada a esa sede. Un pago pendiente o rechazado no cambia la sede actual. Al renovar en otra sede, el cambio se aplica desde la confirmación y se conservan los días pendientes. Reenviar el mismo pago devuelve la operación original y no cambia su sede.

## Consulta administrativa

El panel cuenta socios con membresía vigente por sede: inicio menor o igual a la fecha actual y vencimiento posterior a ella, según el calendario de Lima. Cada socio tiene una sola membresía corriente, por lo que renovar no incrementa el número de socios. Los pagos pendientes y las membresías vencidas se muestran por separado. Los listados de clientes y pagos permiten filtrar por sede.

Este indicador representa membresías activas, no asistencia ni aforo físico en tiempo real. No se establece un límite de inscripciones ni se rechazan pagos por capacidad. Para medir ocupación física se necesita un registro de entradas y salidas; para comparar con un cupo se necesita definir ese cupo por sede.

## Actualización de datos existentes

Flyway aplica `V4__membership_branches.sql` al iniciar la aplicación. Crea el catálogo `branches`, añade `branch_id` y `branch_name` a `payments`, y añade `branch_id` a `memberships`, con claves foráneas e índices para las consultas.

Los pagos y membresías anteriores conservan esos campos en `NULL` y aparecen como «Sin sede registrada». No se deduce su ubicación ni se alteran sus fechas. El panel muestra aparte los socios activos sin sede. Una nueva renovación confirmada asigna la sede elegida a la membresía corriente y mantiene intactos los pagos anteriores.

Las nuevas cuentas de demostración se suscriben explícitamente a San Miguel; no se reasignan cuentas existentes durante el arranque.
