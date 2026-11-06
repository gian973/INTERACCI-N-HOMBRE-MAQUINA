document.querySelectorAll('[data-password]').forEach(button=>button.addEventListener('click',()=>{const input=document.getElementById(button.dataset.password);input.type=input.type==='password'?'text':'password';button.textContent=input.type==='password'?'Mostrar contraseña':'Ocultar contraseña';button.setAttribute('aria-pressed',input.type==='text');}));
document.querySelectorAll('[data-back]').forEach(b=>b.addEventListener('click',()=>history.back()));
const payment=document.getElementById('payment-form');
if(payment){
 const token=payment.elements.culqiToken;
 const message=document.getElementById('culqi-message');
 const update=()=>{const method=payment.querySelector('[name=method]:checked').value;const test=method==='CARD_TEST';const culqi=method==='CULQI';document.getElementById('manual-payment').hidden=test||culqi;document.getElementById('card-test').hidden=!test;document.getElementById('culqi-info').hidden=!culqi;payment.querySelectorAll('[data-payment-method]').forEach(details=>{details.hidden=details.dataset.paymentMethod!==method;});for(const name of ['operation','proof']){payment.elements[name].required=!test&&!culqi;payment.elements[name].disabled=test||culqi;}};
 payment.querySelectorAll('[name=method]').forEach(r=>r.addEventListener('change',update));update();
 if(payment.dataset.culqiEnabled==='true'){
  let culqiConfigured=false;
  const configureCulqi=()=>{
   if(!window.Culqi)return false;
   if(!culqiConfigured){
    Culqi.publicKey=payment.dataset.culqiPublicKey;
    Culqi.settings({title:'Imperio Gym',currency:'PEN',amount:Number(payment.dataset.culqiAmount)});
    Culqi.options({lang:'es',installments:false});
    culqiConfigured=true;
   }
   return true;
  };
  payment.addEventListener('submit',event=>{
   if(payment.querySelector('[name=method]:checked').value!=='CULQI'||token.value)return;
   event.preventDefault();
   if(!payment.reportValidity())return;
   if(!configureCulqi()){
    message.textContent='No se pudo cargar Culqi. Actualiza la página o intenta más tarde.';
    message.hidden=false;
    return;
   }
   message.hidden=true;
   Culqi.open();
  });
  window.culqi=()=>{
   if(Culqi.token){
    token.value=Culqi.token.id;
    payment.requestSubmit(payment.querySelector('button[type=submit]'));
   }else if(Culqi.error){
    message.textContent=Culqi.error.user_message||'Culqi no pudo validar el medio de pago. Inténtalo nuevamente.';
    message.hidden=false;
   }
  };
 }
}
document.querySelectorAll('form[method=post]').forEach(form=>form.addEventListener('submit',event=>{if(event.defaultPrevented)return;if(form.dataset.submitting){event.preventDefault();return;}form.dataset.submitting='true';form.setAttribute('aria-busy','true');const button=event.submitter;if(button){if(button.name){const hidden=document.createElement('input');hidden.type='hidden';hidden.name=button.name;hidden.value=button.value;form.append(hidden);}button.disabled=true;button.textContent='Procesando…';}}));
window.addEventListener('pageshow',e=>{if(e.persisted)location.reload();});
const mobileMenu=document.querySelector('.mobile-nav');
if(mobileMenu){
 const closeMenu=()=>{mobileMenu.open=false;};
 mobileMenu.querySelectorAll('a').forEach(link=>link.addEventListener('click',closeMenu));
 document.addEventListener('click',event=>{if(mobileMenu.open&&!mobileMenu.contains(event.target))closeMenu();});
 document.addEventListener('keydown',event=>{if(event.key==='Escape'&&mobileMenu.open){closeMenu();mobileMenu.querySelector('summary').focus();}});
 window.matchMedia('(max-width: 900px)').addEventListener('change',closeMenu);
}
// Retain only non-sensitive checkout fields when moving back to plan selection.
if(payment){
 const storageKey='imperio-checkout-fields';
 try{const previous=JSON.parse(sessionStorage.getItem(storageKey)||'null');if(previous){
  if(!payment.elements.operation.value)payment.elements.operation.value=previous.operation||'';
  if(payment.elements.branchId&&!payment.elements.branchId.value&&[...payment.elements.branchId.options].some(option=>option.value===previous.branchId))payment.elements.branchId.value=previous.branchId;
  const radio=[...payment.querySelectorAll('[name=method]')].find(r=>r.value===previous.method);
  if(radio){radio.checked=true;radio.dispatchEvent(new Event('change'));}
 }}catch(ignored){}
 const branch=payment.elements.branchId;const branchSummary=document.getElementById('checkout-branch-summary');
 if(branch&&branchSummary){const showBranch=()=>{branchSummary.textContent=branch.selectedOptions[0]?.textContent||'Selecciona una sede';};branch.addEventListener('change',showBranch);showBranch();}
 const remember=()=>{try{sessionStorage.setItem(storageKey,JSON.stringify({method:payment.querySelector('[name=method]:checked').value,operation:payment.elements.operation.value,branchId:branch?.value||''}));}catch(ignored){}};
 payment.addEventListener('input',remember);payment.addEventListener('change',remember);
}else if(document.querySelector('.confirmation-card')){try{sessionStorage.removeItem('imperio-checkout-fields');}catch(ignored){}}
