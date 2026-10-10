# Análisis relacional de 1FN a 5FN

## Alcance y supuestos
El análisis cubre las relaciones persistentes después de V1–V5; Flyway mantiene aparte su tabla técnica. No se equiparan formas normales con cantidad de tablas. Las dependencias se deducen de las reglas del dominio, no de coincidencias del catálogo de demostración. Las claves artificiales no prueban por sí solas 3FN, 4FN o 5FN.

Cada usuario tiene correo y DNI únicos; puede tener varios roles. Un plan puede tener varios beneficios compartidos con otros planes. Cada pago corresponde a un usuario y un plan; su nombre, duración, importe y beneficios son una instantánea contractual que permanece aunque cambie el catálogo. El cambio de plan se aplica al confirmarse. Hay una sola membresía corriente por usuario y operaciones históricas separadas. No existe una relación ternaria independiente entre usuario, sede y plan.

## Dependencias funcionales y claves por relación
En la tabla, K → resto significa que cada clave candidata indicada determina todos los restantes atributos de esa relación. No se supone una DF solo por una FK: por ejemplo plan_id no determina el importe histórico de un pago.

| Relación | Claves candidatas / dependencias funcionales relevantes |
|---|---|
| users | id, dni y email son claves; cada una → name, phone, password_hash, demo, created_at y las otras claves. |
| roles | id y name son claves alternativas. |
| user_roles | (user_id,role_id) es la clave completa; no hay atributos no clave. |
| plans | id → name,months,price,active. Ni nombre ni meses se consideran únicos. |
| benefits | id y label son claves alternativas. |
| plan_benefits | (plan_id,benefit_id) es la clave completa; no hay atributos no clave. |
| payments | id e idempotency_key son claves. En el subconjunto manual, (method,operation_number) también es clave. Cada una → user_id,plan_id,instantánea contractual,estado,fechas. plan_id NO → plan_name/months/amount: dos contratos del mismo plan pueden tener términos distintos. |
| contract_benefits | id y (payment_id,label) son claves. Se conserva el texto contratado; no depende del texto actual de benefits. |
| memberships | id, user_id y payment_id son claves alternativas. Cada una → branch_id,started_on,expires_on y las demás claves. expires_on no es derivable del último pago porque acumula períodos anteriores y extensiones aprobadas. |
| branches | id y name son claves alternativas; cada una → active,capacity. |
| attendance | id → branch_id,user_id,checked_in_at,checked_out_at. La visita es un evento; las validaciones de membresía vigente, sede coincidente y entrada abierta pertenecen al servicio transaccional. |
| freeze_requests | id → user_id,kind,starts_on,ends_on,comment,status,created_at,policy,extension_days. Política y extensión son resultado histórico de una decisión, no función universal del período: antes de aprobar son null/0 y distintas políticas producen resultados distintos. |
| private_files | id y storage_key son claves globales. Para el subtipo comprobante, payment_id no nulo es clave; para el subtipo adjunto, request_id no nulo es clave. Estas claves determinan owner_id y metadatos. No hay varios archivos por el mismo padre. |
| reviews | id → padre exclusivo,reviewer_id,from_status,to_status,comment,created_at. Un padre admite varias revisiones; no determina actor ni respuesta. |
| preferences | id y user_id son claves alternativas; determinan phone,consent,notice7,notice1. El número de WhatsApp puede diferir del teléfono de registro, por lo que no se almacena una copia dependiente del teléfono de users. |
| reminders | id y (user_id,expiry_date,days_before) son claves. Determinan status,processed_at. La fecha de envío se CALCULA (expiry_date - days_before): no se almacena due_on, evitando la DF no clave (expiry_date,days_before) → due_on. |
| audit_events | id → actor_id,action,target_type,target_id,detail,created_at. El destino no determina la descripción histórica ni el actor. detail es un mensaje atómico de auditoría, no una lista usada como sustituto de relaciones. |
| reset_tokens | id y token_hash son claves; determinan user_id,expires_at,used. Un usuario puede tener varios tokens históricos; user_id no es clave aquí. |

## 1FN
Los atributos son escalares dentro del dominio definido: fechas, importes DECIMAL, booleanos y textos. Roles, beneficios, pagos, revisiones y recordatorios no se guardan como CSV o JSON en una celda. El comentario es una unidad textual de negocio. Los dos interruptores de recordatorio son dos opciones fijas booleanas (no conjuntos de valores ni columnas para eventos repetidos).

## 2FN
Las únicas claves compuestas sin surrogate son user_roles y plan_benefits: no tienen atributos que dependan de solo parte de la clave. En contract_benefits la pareja completa identifica la instantánea, y label por sí sola no determina id ni payment_id. En reminders el estado depende del usuario, vencimiento y anticipación completos: quitar cualquiera permite avisos diferentes. En pagos manuales una referencia puede repetirse entre métodos, por lo que la clave requiere ambos. En las restantes relaciones cada clave es simple; no aparecen dependencias parciales.

## 3FN y BCNF
Con las dependencias declaradas, todos los determinantes no triviales internos son superclaves, bajo los subdominios indicados. Los nombres de usuarios y revisores se consultan en users; los roles y beneficios de catálogo se separan de sus asignaciones. No se guarda estado activo/vencido ni días restantes de membresía: se derivan del vencimiento y fecha Lima. Tampoco se guarda fecha programada de recordatorio. V3 elimina applied_until (derivable de applied_from y meses contratados) y extension_days (derivable de fechas y pol?tica aplicada); ambos se muestran mediante getters calculados. As? se evitan dos dependencias de determinantes no clave.

La instantánea de pago es un hecho histórico y no una duplicación accidental de la fila mutable del catálogo. No se puede reconstruir a partir de su plan_id tras una edición. El snapshot de beneficios tampoco puede reemplazarse por un join al catálogo mutable.

SQL admite NULL para datos opcionales. La teoría clásica presupone relaciones sin NULL; en payments, private_files y reviews las conclusiones formales se aplican a sus subdominios (pago manual/tarjeta; archivo de pago/solicitud; revisión de pago/solicitud). CHECK garantiza exclusión de padres. La unión etiquetada es disjunta y se reconstruye sin pérdida. No se afirma que un UNIQUE nullable sea una clave candidata global.

## 4FN: dependencias multivaluadas
En una tabla universal de usuario, rol, pago y preferencia habría hechos multivaluados independientes: usuario ↠ rol y usuario ↠ pago. Juntarlos repetiría cada pago por cada rol. Se separaron users, user_roles, payments y preferences; el mismo criterio separa planes/beneficios y pagos/revisiones.

user_roles y plan_benefits son relaciones binarias: sus MVD evidentes son triviales, pues X ∪ Y abarca toda la relación. Las demás representan un único hecho por clave, sin dos conjuntos independientes en una fila. Los atributos de revisión se vinculan entre sí como un evento, no como listas independientes de autores y comentarios. Los beneficios históricos se separan de sus revisiones. Bajo estos supuestos no queda una MVD no trivial cuyo determinante no sea superclave.

## 5FN: dependencias de unión
No basta decir que no hay tablas ternarias. Para cada grupo:

- user_roles y plan_benefits: descomponer en proyecciones unarias y volver a unir produce el producto cartesiano y asignaciones inexistentes. No hay una JD no trivial admitida por el negocio; el hecho es la pareja.
- contract_benefits: (payment_id,label) es un hecho irreducible. Proyectar ambos por separado inventaría beneficios en otros contratos. El id es una clave alternativa, no un tercer hecho independiente.
- payments, freeze_requests, reviews, attendance y audit_events: cada fila representa una operación/evento. Unir proyecciones que omiten su clave combina importes, estados, actores o respuestas de eventos diferentes y puede crear filas falsas. No existe regla que valide tales combinaciones. Proyecciones que conservan la clave tienen joins sin pérdida implicados por esa clave.
- reminders: no se admite la JD de pares (usuario,vencimiento), (usuario,anticipación), (vencimiento,anticipación). Contraejemplo permitido: (u1,e1,7), (u1,e2,1), (u2,e1,1). La unión de pares produciría (u1,e1,1) que no estaba programado. Por tanto esa JD no es una restricción del dominio.
- users, roles, plans, benefits, memberships, preferences, private_files y reset_tokens: atributos escalares describen un objeto identificado por una clave; no hay regla que reconstruya combinaciones sin ella. En archivos el padre alternativo único identifica el hecho dentro del subtipo. Las JD conocidas son triviales o consecuencias de las claves candidatas.

Así, el diseño satisface 5FN RELATIVA al conjunto de dependencias del dominio documentado (y a los subdominios SQL con NULL mencionados). No se pretende demostrar ausencia de futuras reglas comerciales desconocidas. Si se incorporan asociaciones ternarias independientes —por ejemplo habilitaciones usuario/sede/plan— habrá que volver a analizar sus JD con ejemplos de combinaciones válidas.

## Descomposiciones y conservación de información
Separar users y user_roles conserva el identificador del usuario en el vínculo: la intersección user_id determina la fila de users, por lo que el join de los hechos asociados es sin pérdida. Lo mismo vale para plans ↔ plan_benefits ↔ benefits y payments ↔ contract_benefits. Las FK impiden asociaciones huérfanas. Los usuarios sin roles o planes sin beneficios se conservan en sus tablas maestras; un LEFT JOIN presenta esos casos sin confundirlos con hechos de asociación.

Separar pagos de revisiones evita repetir la instantánea por cada revisión; payment_id determina la operación. El CHECK de padre exclusivo hace disjuntas las particiones de archivos/revisiones por tipo: su UNION ALL vuelve a formar exactamente la relación original. Estas descomposiciones no crean asociaciones nuevas ni pierden entidades sin hijos. No se construyen tablas artificiales para sumar “cinco formas”.

## Integridad que además comprueba la aplicación
Las FK no expresan que owner_id del archivo coincida con el usuario del padre; el servicio fija ambos valores, sin aceptar owner_id desde el navegador. Las transiciones, consentimiento vigente, ausencia de superposición de congelamientos y aplicación única requieren transacciones y lecturas con bloqueo. Las claves e índices SQL complementan estas reglas, no las sustituyen.
