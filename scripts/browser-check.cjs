const {chromium}=require('../.tools/qa/node_modules/playwright');
const fs=require('node:fs');
const assert=require('node:assert/strict');
const base=process.env.BASE_URL||'http://localhost:8090';
const stamp=Date.now().toString().slice(-7);
const dni='3'+stamp,email=dni+'@example.test',password='BrowserTest2026!';
const output='docs/screenshots';
fs.mkdirSync(output,{recursive:true});
const check=async(page,text)=>assert.ok((await page.locator('body').innerText()).toLocaleLowerCase().includes(text.toLocaleLowerCase()),text);
async function login(page,who,secret){await page.goto(base+'/login');await page.getByLabel('Correo o DNI').fill(who);await page.getByLabel('Contraseña',{exact:true}).fill(secret);await Promise.all([page.waitForURL(url=>!url.pathname.endsWith('/login')),page.getByRole('button',{name:'Ingresar'}).click()]);}
async function manual(page,operation){await page.locator('[name=branchId]').selectOption('1');await page.locator('[name=operation]').fill(operation);await page.locator('[name=proof]').setInputFiles({name:'comprobante.pdf',mimeType:'application/pdf',buffer:Buffer.from('%PDF-1.4\n1 0 obj <<>> endobj\n%%EOF')});await page.locator('[name=accepted]').check();await Promise.all([page.waitForURL(/\/account\/payments\/\d+$/),page.getByRole('button',{name:'Enviar pago'}).click()]);return page.url().split('/').pop();}
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 const context=await browser.newContext({viewport:{width:1440,height:1000}});
 const page=await context.newPage();const adminContext=await browser.newContext({viewport:{width:1440,height:1000}});const admin=await adminContext.newPage();
 const errors=[];for(const p of [page,admin])p.on('pageerror',e=>errors.push(e.message));
 await page.goto(base);await check(page,'Entrena con propósito.');await page.screenshot({path:output+'/inicio-desktop.png',fullPage:true});
 await page.locator('#planes .plan-card a').first().click();await page.waitForURL(/\/login/);await check(page,'Plan mensual');
 await page.getByRole('link',{name:'Registrarme',exact:true}).click();
 await page.getByLabel('Nombre completo').fill('Socio Navegador');await page.getByLabel('DNI',{exact:true}).fill(dni);await page.getByLabel('Teléfono',{exact:true}).fill('+51999000000');await page.getByLabel('Correo electrónico').fill(email);await page.getByLabel('Contraseña',{exact:true}).fill(password);await page.getByLabel('Confirma tu contraseña').fill(password);
 await page.getByRole('button',{name:'Crear cuenta'}).click();await page.waitForURL(/\/login/);
 await login(page,dni,password);await check(page,'89.99');
 const first=await manual(page,'WEB-'+stamp);await check(page,'Pendiente de validación');
 await login(admin,'admin@imperio.test',process.env.DEMO_PASSWORD||'Imperio2026!');
 await admin.screenshot({path:output+'/admin-dashboard.png',fullPage:true});
 await admin.goto(base+'/admin/payments/'+first);await admin.locator('[name=comment]').fill('Pago verificado en prueba de navegador');await admin.getByRole('button',{name:'Aprobar pago',exact:true}).click();await check(admin,'Pago confirmado');
 await page.goto(base+'/account');await check(page,'Membresía activa');const initial=await page.locator('.expiry-block>strong').innerText();
 await page.goto(base+'/account/renew');await page.locator('.plan-card a').nth(1).click();const second=await manual(page,'RENEW-'+stamp);
 await page.goto(base+'/account');assert.equal(await page.locator('.expiry-block>strong').innerText(),initial);
 await admin.goto(base+'/admin/payments/'+second);await admin.getByRole('button',{name:'Aprobar pago',exact:true}).click();await check(admin,'Pago confirmado');
 await page.goto(base+'/account');const renewed=await page.locator('.expiry-block>strong').innerText();assert.notEqual(renewed,initial);await page.screenshot({path:output+'/cliente-cuenta.png',fullPage:true});
 await admin.reload();assert.equal(await admin.getByRole('button',{name:'Aprobar pago',exact:true}).count(),0);
 await page.goto(base+'/account/requests');await page.locator('[name=kind]').selectOption('TRAVEL');
 const today=new Date(new Date().toLocaleString('en-US',{timeZone:'America/Lima'}));const iso=offset=>{const d=new Date(today);d.setDate(d.getDate()+offset);return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');};
 await page.locator('[name=startsOn]').fill(iso(2));await page.locator('[name=endsOn]').fill(iso(4));await page.locator('[name=comment]').fill('Viaje de prueba automatizada');await page.getByRole('button',{name:'Enviar solicitud'}).click();await page.waitForURL(/\/account\/requests\/\d+$/);const request=page.url().split('/').pop();
 await admin.goto(base+'/admin/requests/'+request);await admin.getByRole('button',{name:'Pasar a En revisión'}).click();await admin.locator('[name=comment]').fill('Solicitud aprobada en navegador');await admin.getByRole('button',{name:'Aprobar solicitud',exact:true}).click();
 await page.reload();await check(page,'Solicitud aprobada en navegador');
 await page.goto(base+'/account/preferences');await page.locator('[name=consent]').check();await page.getByRole('button',{name:'Guardar preferencias'}).click();await check(page,'Preferencias guardadas');assert.ok(await page.locator('[name=consent]').isChecked());
 await page.locator('[name=consent]').uncheck();await page.getByRole('button',{name:'Guardar preferencias'}).click();assert.ok(!await page.locator('[name=consent]').isChecked());
 let response=await page.goto(base+'/admin');assert.equal(response.status(),403);
 const outsiderContext=await browser.newContext();const outsider=await outsiderContext.newPage();await login(outsider,'ana.torres@ejemplo.com',process.env.DEMO_PASSWORD||'Imperio2026!');
 response=await outsider.goto(base+'/account/payments/'+first);assert.equal(response.status(),403);
 response=await outsider.goto(base+'/account/requests/'+request);assert.equal(response.status(),403);
 const mobile=await context.newPage();await mobile.setViewportSize({width:390,height:844});await mobile.goto(base);await mobile.screenshot({path:output+'/inicio-mobile.png',fullPage:true});assert.ok(await mobile.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+1));
 await mobile.goto(base+'/account');await mobile.screenshot({path:output+'/cuenta-mobile.png',fullPage:true});assert.ok(await mobile.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+1));
 for(const path of ['/admin/clients?q='+dni,'/admin/payments?status=PENDING','/admin/requests?kind=TRAVEL&status=APPROVED','/admin/plans','/admin/reminders','/admin/audit?page=1']){response=await admin.goto(base+path);assert.equal(response.status(),200,path);}
 fs.mkdirSync('.tools',{recursive:true});fs.writeFileSync('.tools/persistence-check.json',JSON.stringify({email,password,first,second,request,expiry:renewed}));
 assert.deepEqual(errors,[]);console.log('PASS: browser flows A-F, role checks, filters, desktop and mobile (390px).');await browser.close();
})().catch(e=>{console.error(e);process.exit(1);});
