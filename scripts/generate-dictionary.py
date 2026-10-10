from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
sql=(root/'src/main/resources/db/migration/V1__schema.sql').read_text(encoding='utf-8')
descriptions={
'id':'Identificador técnico de la fila.','user_id':'Usuario relacionado; FK a users.id.','role_id':'Rol asignado; FK a roles.id.','plan_id':'Plan de catálogo; FK a plans.id.','benefit_id':'Beneficio de catálogo; FK a benefits.id.','payment_id':'Pago relacionado; FK a payments.id.','request_id':'Solicitud relacionada; FK a freeze_requests.id.','reviewer_id':'Administrador revisor; FK a users.id.','actor_id':'Autor de la acción o NULL para sistema; FK a users.id.','owner_id':'Propietario; debe coincidir con el usuario del padre.','name':'Nombre del usuario, rol o plan según la relación.','dni':'DNI de ocho dígitos, usado como identificador.','email':'Correo normalizado a minúsculas.','phone':'Teléfono de contacto o número de WhatsApp en preferencias.','password_hash':'Hash BCrypt; nunca contraseña legible.','demo':'Marca de cuenta ficticia afectada por reinicio demo.','created_at':'Instante de creación/revisión/auditoría en UTC.','months':'Meses de catálogo o meses históricos contratados.','price':'Importe total actual del catálogo en soles.','amount':'Importe total histórico contratado en soles.','active':'Disponibilidad para nuevas contrataciones.','label':'Texto del beneficio actual o su instantánea contractual.','plan_name':'Nombre histórico del plan.','method':'CARD_TEST, WALLET o TRANSFER.','status':'Estado del flujo; limitado por CHECK.','operation_number':'Referencia de pago manual; NULL en tarjeta simulada.','idempotency_key':'UUID del resumen emitido por el servidor.','applied_from':'Base calendario al confirmar; NULL antes de confirmar.','started_on':'Inicio del período continuo de membresía.','expires_on':'Vencimiento exclusivo acumulado.','kind':'HEALTH (salud) o TRAVEL (viaje).','starts_on':'Primer día de congelamiento (inclusivo).','ends_on':'Último día solicitado (inclusivo).','comment':'Motivo o respuesta; máximo 1000 caracteres.','policy':'Política aplicada; NULL antes de aprobar.','storage_key':'UUID de almacenamiento; no una ruta del cliente.','original_name':'Nombre seguro según tipo, p. ej. documento.pdf; evita conservar nombres sensibles del equipo.','media_type':'application/pdf, image/png o image/jpeg, verificado.','bytes':'Tamaño de 1 a 5242880 bytes.','from_status':'Estado previo.','to_status':'Estado posterior.','consent':'Autorización opcional; inicialmente false.','notice7':'Aviso 7 días antes; no constituye consentimiento.','notice1':'Aviso 1 día antes; no constituye consentimiento.','expiry_date':'Vencimiento al que corresponde el aviso.','days_before':'Anticipación: 7 o 1 días.','processed_at':'Instante de intento/cancelación; NULL sin procesar.','action':'Tipo de acción auditada.','target_type':'Tipo lógico del destino auditado.','target_id':'ID histórico sin FK: puede sobrevivir al reinicio demo.','detail':'Descripción histórica, sin secretos.','token_hash':'SHA-256 del token; no se almacena el original.','expires_at':'Límite de validez del token.','used':'Token usado o invalidado.'
}
lines=['# Diccionario de datos','',
'Esquema final después de V1–V3. TIMESTAMP(6) guarda instantes UTC; DATE representa calendario de Lima. Importes DECIMAL(10,2), moneda PEN. Las PK implican NOT NULL. Las migraciones SQL son la definición ejecutable.','',
'Calculados sin columna: estado activo/vencido, días restantes, reminder.dueOn, payment.appliedUntil y request.extensionDays.','']
removed={'payments':{'applied_until'},'freeze_requests':{'extension_days'}}
for table,body in re.findall(r'CREATE TABLE (\w+)\s*\((.*?)\);',sql,re.S):
    pieces=[];depth=0;start=0
    for i,ch in enumerate(body):
        if ch=='(':depth+=1
        elif ch==')':depth-=1
        elif ch==',' and depth==0:pieces.append(body[start:i].strip());start=i+1
    pieces.append(body[start:].strip())
    columns=[];constraints=[]
    for piece in pieces:
        if piece.upper().startswith(('PRIMARY','FOREIGN','UNIQUE','CONSTRAINT','CHECK')):
            if 'ck_freeze_days' not in piece:constraints.append(piece)
        else:
            name,definition=piece.split(None,1)
            if name not in removed.get(table,set()):columns.append((name,definition))
    lines+=['## '+table,'','| Columna | Tipo / restricciones | Descripción |','|---|---|---|']
    for name,definition in columns:lines.append('| '+name+' | '+definition.replace('|',r'\|')+' | '+descriptions.get(name,'Atributo de entidad.')+' |')
    lines+=['']
    if constraints:lines+=['Claves y restricciones:','']+['- '+re.sub(r'\s+',' ',c) for c in constraints]+['']
lines+=['## Índices y justificación','',
'PK y UNIQUE crean índices para identidad, acceso por correo/DNI, idempotencia y unicidad. InnoDB añade índices para FK cuando no hay un prefijo compatible.','',
'| Índice | Uso |','|---|---|',
'| ix_payments_status_created | Cola por estado y antigüedad. |',
'| ix_payments_user_created | Historial de pagos del socio. |',
'| ix_membership_expiry | Conteos de activas/vencidas. |',
'| ix_freeze_status_date | Solicitudes por estado y período. |',
'| ix_freeze_user_created | Historial del cliente. |',
'| ix_review_payment / ix_review_request | Revisiones cronológicas por operación. |',
'| ix_reminder_due (status,expiry_date,days_before) | Avisos pendientes por vencimiento y anticipación sin persistir fecha derivada. |',
'| ix_audit_created | Auditoría cronológica. |',
'| ix_reset_user | Invalidación de tokens previos. |','',
'Las búsquedas con comodín inicial priorizan flexibilidad para este portal. Con gran volumen, medir planes de consulta antes de incorporar índices de texto específicos.','']
(root/'docs/DICCIONARIO.md').write_text('\n'.join(lines),encoding='utf-8')
print('Dictionary generated for 17 relations.')
