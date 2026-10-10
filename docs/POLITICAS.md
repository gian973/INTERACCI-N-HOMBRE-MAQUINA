# Criterios técnicos sujetos a validación comercial

## Clientes sin duplicados

Cada cliente tiene una sola cuenta por DNI y correo electrónico. Se rechaza el registro si cualquiera de esos datos ya existe, aunque el correo se escriba con distintas mayúsculas y minúsculas. Las restricciones únicas de la base de datos también impiden duplicados cuando llegan registros simultáneos. Para renovar una membresía se utiliza la cuenta existente.

El nombre no es único: distintas personas pueden llamarse igual. El teléfono tampoco se usa como identificador único.

## Membresías y pagos

Cada pago nuevo debe indicar una sede activa. El pago conserva la sede elegida y su nombre histórico. La membresía cambia de sede solo al confirmar el pago; si se renueva en otra ubicación, el cambio entra en vigor desde esa confirmación conservando los días pendientes. Una renovación no crea otro cliente ni otra membresía. El conteo administrativo por sede representa socios con membresía activa, no personas presentes en el gimnasio. Véanse las reglas y la migración en [SEDES.md](SEDES.md).

Antes de implementar: el congelamiento no extiende la vigencia por defecto (FREEZE_POLICY=NO_EXTENSION). La aprobación registra resolución, revisor y fecha. No existe aprobación automática.
Opción configurable EXTEND_DAYS: después de pasar a En revisión, aprobar suma al vencimiento los días inclusivos entre inicio y fin. Se rechazan períodos fuera de la vigencia actual y superpuestos con otra solicitud enviada, en revisión o aprobada. Es una propuesta técnica, no una condición comercial definitiva. La política aplicada y los días quedan registrados por solicitud; cambiar configuración no altera decisiones anteriores.
Renovación/cambio: fecha base=max(hoy Lima, vencimiento actual), fin exclusivo=base.plusMonths(meses). Se mantienen días pendientes; el plan nuevo queda aplicado al confirmar y el vencimiento se amplía desde esa base. No hay prorrateos, descuentos ni cargos adicionales.
Los meses son calendario: 31/01 + 1 mes = 28/02 (29 en año bisiesto); 31/01 + 2 meses = 31/03. Se aplica una sola suma a la base, no iteraciones mensuales. La vista previa se recalcula al confirmar porque otro pago puede haber sido aprobado entretanto.
Los precios y nombres/duración/beneficios contratados se copian a cada operación. Un pago pendiente no modifica la membresía; rechazar tampoco. Tarjeta es un simulador explícito sin campos PAN/CVV; los pagos manuales esperan revisión real dentro de la aplicación.
