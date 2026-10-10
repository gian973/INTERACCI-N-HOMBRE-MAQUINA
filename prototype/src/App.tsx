import { FormEvent, ReactNode, useEffect, useRef, useState } from "react";

type View = "home" | "payment" | "confirmation" | "account" | "renewal" | "requests" | "preferences";
type PaymentMethod = "card" | "wallet" | "transfer";
type PaymentStatus = "Pendiente de validación" | "Confirmado";
type RequestStatus = "Enviada" | "En revisión" | "Aprobada" | "Rechazada";

type Plan = {
  id: string;
  name: string;
  months: number;
  duration: string;
  price: string;
};

type FreezeRequest = {
  id: string;
  kind: string;
  start: string;
  end: string;
  reason: string;
  status: RequestStatus;
  rejection?: string;
};

const PLANS: Plan[] = [
  { id: "monthly", name: "Plan mensual", months: 1, duration: "1 mes", price: "89.99" },
  { id: "two", name: "Plan 2 meses", months: 2, duration: "2 meses", price: "114.99" },
  { id: "three", name: "Plan 3 meses", months: 3, duration: "3 meses", price: "179.99" },
];

const HERO_IMAGE =
  "https://images.unsplash.com/photo-1659331150328-e6c822ae77b2?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixlib=rb-4.1.0&q=80&w=1600";

const Icon = ({ name, size = 20 }: { name: string; size?: number }) => {
  const paths: Record<string, ReactNode> = {
    arrow: <path d="M5 12h14m-5-5 5 5-5 5" />,
    back: <path d="m15 18-6-6 6-6" />,
    check: <path d="m5 12 4 4L19 6" />,
    eye: <><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z" /><circle cx="12" cy="12" r="2.5" /></>,
    eyeOff: <><path d="m3 3 18 18M10.6 10.7a2 2 0 0 0 2.7 2.7M9.9 5.2A10 10 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-2.1 3M6.6 6.6C3.6 8.4 2 12 2 12s3.5 7 10 7a9 9 0 0 0 4.1-1" /></>,
    user: <><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></>,
    card: <><rect x="2" y="5" width="20" height="14" rx="2" /><path d="M2 10h20" /></>,
    phone: <><rect x="7" y="2" width="10" height="20" rx="2" /><path d="M11 18h2" /></>,
    bank: <><path d="m3 10 9-6 9 6M5 10v8m5-8v8m4-8v8m5-8v8M3 21h18" /></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M16 3v4M8 3v4M3 10h18" /></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></>,
    file: <><path d="M6 2h9l4 4v16H6zM14 2v5h5" /></>,
    refresh: <><path d="M20 11a8 8 0 1 0-2 5.5M20 4v7h-7" /></>,
    close: <path d="M6 6l12 12M18 6 6 18" />,
    logout: <><path d="M10 17l5-5-5-5M15 12H3M15 4h5v16h-5" /></>,
    info: <><circle cx="12" cy="12" r="9" /><path d="M12 11v6M12 7h.01" /></>,
  };
  return (
    <svg aria-hidden="true" className="icon" fill="none" height={size} viewBox="0 0 24 24" width={size}>
      <g stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8">{paths[name]}</g>
    </svg>
  );
};

function Button({
  children,
  variant = "primary",
  className = "",
  icon,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost" | "danger";
  icon?: string;
}) {
  return (
    <button className={`btn btn-${variant} ${className}`} {...props}>
      {icon && <Icon name={icon} />}
      <span>{children}</span>
    </button>
  );
}

function Field({
  label,
  error,
  hint,
  children,
}: {
  label: string;
  error?: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <label className={`field ${error ? "field-error" : ""}`}>
      <span className="field-label">{label}</span>
      {children}
      {hint && !error && <span className="field-hint">{hint}</span>}
      {error && <span className="error-text">{error}</span>}
    </label>
  );
}

function Status({ children, tone = "neutral" }: { children: ReactNode; tone?: "success" | "warning" | "danger" | "neutral" }) {
  return <span className={`status status-${tone}`}><span className="status-dot" />{children}</span>;
}

function Logo() {
  return (
    <div className="logo" aria-label="Imperio Gym">
      <span className="logo-mark">IG</span>
      <span>IMPERIO <b>GYM</b></span>
    </div>
  );
}

function Progress({ step }: { step: number }) {
  const steps = ["Plan", "Identificación", "Pago", "Confirmación"];
  return (
    <div className="progress" aria-label={`Paso ${step} de 4`}>
      {steps.map((item, index) => (
        <div className={`progress-step ${index + 1 <= step ? "is-active" : ""}`} key={item}>
          <span>{index + 1 < step ? <Icon name="check" size={15} /> : index + 1}</span>
          <b>{item}</b>
        </div>
      ))}
    </div>
  );
}

const formatDate = (iso: string) => {
  const [year, month, day] = iso.split("-");
  return `${day}/${month}/${year}`;
};

const addMonths = (iso: string, months: number) => {
  const date = new Date(`${iso}T12:00:00`);
  date.setMonth(date.getMonth() + months);
  return date.toISOString().slice(0, 10);
};

const daysFromReference = (iso: string) => {
  const reference = new Date("2026-10-05T12:00:00").getTime();
  const target = new Date(`${iso}T12:00:00`).getTime();
  return Math.ceil((target - reference) / 86_400_000);
};

export default function App() {
  const [view, setView] = useState<View>("home");
  const [selectedPlan, setSelectedPlan] = useState<Plan | null>(null);
  const [authOpen, setAuthOpen] = useState(false);
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [loggedIn, setLoggedIn] = useState(false);
  const [memberName, setMemberName] = useState("Juan Pérez");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("wallet");
  const [paymentStatus, setPaymentStatus] = useState<PaymentStatus>("Pendiente de validación");
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [paymentErrors, setPaymentErrors] = useState<Record<string, string>>({});
  const [operation, setOperation] = useState("");
  const [proof, setProof] = useState("");
  const [card, setCard] = useState({ number: "", expiry: "", cvv: "" });
  const [terms, setTerms] = useState(false);
  const [requestNumber, setRequestNumber] = useState("SOL-2026-1042");
  const [currentPlan, setCurrentPlan] = useState(PLANS[0]);
  const [expiry, setExpiry] = useState("2026-10-10");
  const [renewPlan, setRenewPlan] = useState(PLANS[0]);
  const [renewReview, setRenewReview] = useState(false);
  const [isRenewal, setIsRenewal] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");
  const [requests, setRequests] = useState<FreezeRequest[]>([
    {
      id: "SOL-2026-0831",
      kind: "Viaje",
      start: "2026-08-12",
      end: "2026-08-18",
      reason: "Viaje fuera de la ciudad",
      status: "Rechazada",
      rejection: "La solicitud se registró después de la fecha de inicio.",
    },
  ]);
  const [freeze, setFreeze] = useState({ kind: "Salud", start: "", end: "", reason: "", file: "" });
  const [freezeErrors, setFreezeErrors] = useState<Record<string, string>>({});
  const [waEnabled, setWaEnabled] = useState(false);
  const [waPhone, setWaPhone] = useState("+51 999 000 000");
  const [notice7, setNotice7] = useState(true);
  const [notice1, setNotice1] = useState(true);
  const [prefsSaved, setPrefsSaved] = useState(false);
  const [adminOpen, setAdminOpen] = useState(false);
  const mainRef = useRef<HTMLElement>(null);

  useEffect(() => {
    mainRef.current?.focus();
    window.scrollTo({ top: 0, behavior: "smooth" });
  }, [view]);

  const go = (next: View) => setView(next);

  const choosePlan = (plan: Plan) => {
    setSelectedPlan(plan);
    if (loggedIn) {
      setIsRenewal(false);
      go("payment");
    } else {
      setAuthMode("register");
      setAuthOpen(true);
    }
  };

  const resetDemo = () => {
    setView("home");
    setSelectedPlan(null);
    setAuthOpen(false);
    setLoggedIn(false);
    setMemberName("Juan Pérez");
    setCurrentPlan(PLANS[0]);
    setExpiry("2026-10-10");
    setPaymentStatus("Pendiente de validación");
    setRequests([{
      id: "SOL-2026-0831",
      kind: "Viaje",
      start: "2026-08-12",
      end: "2026-08-18",
      reason: "Viaje fuera de la ciudad",
      status: "Rechazada",
      rejection: "La solicitud se registró después de la fecha de inicio.",
    }]);
    setWaEnabled(false);
    setPrefsSaved(false);
    setSuccessMessage("");
    setAdminOpen(false);
    setOperation("");
    setProof("");
    setTerms(false);
  };

  const openLogin = () => {
    setSelectedPlan(null);
    setAuthMode("login");
    setAuthOpen(true);
  };

  const logout = () => {
    setLoggedIn(false);
    setView("home");
    setSelectedPlan(null);
  };

  const submitPayment = (event: FormEvent) => {
    event.preventDefault();
    const errors: Record<string, string> = {};
    if (paymentMethod === "card") {
      if (card.number.replace(/\s/g, "").length < 12) errors.number = "Ingresa un número de tarjeta de prueba válido.";
      if (!card.expiry) errors.expiry = "Indica el vencimiento de prueba.";
      if (card.cvv.length < 3) errors.cvv = "Ingresa 3 dígitos de prueba.";
    } else {
      if (operation.trim().length < 5) errors.operation = "Ingresa el número de operación.";
      if (!proof) errors.proof = "Adjunta un comprobante para continuar.";
    }
    if (!terms) errors.terms = "Debes aceptar las condiciones.";
    setPaymentErrors(errors);
    if (Object.keys(errors).length) return;
    setPaymentLoading(true);
    window.setTimeout(() => {
      const status: PaymentStatus = paymentMethod === "card" ? "Confirmado" : "Pendiente de validación";
      setPaymentStatus(status);
      setRequestNumber(`SOL-2026-${Math.floor(1100 + Math.random() * 800)}`);
      if (status === "Confirmado" && !isRenewal && selectedPlan) {
        setCurrentPlan(selectedPlan);
        setExpiry(addMonths("2026-10-05", selectedPlan.months));
      }
      setPaymentLoading(false);
      go("confirmation");
    }, 900);
  };

  const confirmPayment = () => {
    setPaymentStatus("Confirmado");
    const plan = isRenewal ? renewPlan : selectedPlan;
    if (plan) {
      setCurrentPlan(plan);
      setExpiry(addMonths(expiry, plan.months));
      setSuccessMessage(
        isRenewal
          ? `Renovación confirmada. La nueva vigencia termina el ${formatDate(addMonths(expiry, plan.months))}. Se cancelaron los avisos del vencimiento anterior.`
          : "Pago confirmado. Tu membresía ya está activa.",
      );
    }
  };

  const startRenewal = (change = false) => {
    setRenewPlan(change ? PLANS[1] : currentPlan);
    setRenewReview(false);
    setIsRenewal(true);
    setSuccessMessage("");
    go("renewal");
  };

  const requestRenewal = () => {
    if (!renewReview) {
      setRenewReview(true);
      return;
    }
    if (!window.confirm(`¿Confirmas la solicitud para ${renewPlan.name} por S/ ${renewPlan.price}?`)) return;
    setSelectedPlan(renewPlan);
    setPaymentMethod("wallet");
    setOperation("");
    setProof("");
    setTerms(false);
    go("payment");
  };

  const submitFreeze = (event: FormEvent) => {
    event.preventDefault();
    const errors: Record<string, string> = {};
    if (!freeze.start) errors.start = "Selecciona una fecha de inicio.";
    if (!freeze.end) errors.end = "Selecciona una fecha final.";
    if (freeze.start && freeze.start < "2026-10-05") errors.start = "La fecha no puede ser anterior al 05/10/2026.";
    if (freeze.start && freeze.end && freeze.end <= freeze.start) errors.end = "La fecha final debe ser posterior a la inicial.";
    if (freeze.reason.trim().length < 10) errors.reason = "Explica el motivo en al menos 10 caracteres.";
    setFreezeErrors(errors);
    if (Object.keys(errors).length) return;
    setRequests((items) => [
      {
        id: `SOL-2026-${1200 + items.length}`,
        kind: freeze.kind,
        start: freeze.start,
        end: freeze.end,
        reason: freeze.reason,
        status: "Enviada",
      },
      ...items,
    ]);
    setFreeze({ kind: "Salud", start: "", end: "", reason: "", file: "" });
    setSuccessMessage("Solicitud enviada. Requiere revisión administrativa antes de ser aprobada.");
  };

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main">Saltar al contenido</a>
      <Header
        loggedIn={loggedIn}
        onAccount={() => go("account")}
        onLogin={openLogin}
        onLogout={logout}
        onReset={resetDemo}
        onHome={() => go("home")}
        view={view}
      />

      <main id="main" ref={mainRef} tabIndex={-1}>
        {view === "home" && <Home onChoose={choosePlan} onLogin={openLogin} />}
        {view === "payment" && selectedPlan && (
          <Payment
            card={card}
            errors={paymentErrors}
            isRenewal={isRenewal}
            loading={paymentLoading}
            memberName={memberName}
            method={paymentMethod}
            onBack={() => go(isRenewal ? "renewal" : "home")}
            onCard={setCard}
            onMethod={setPaymentMethod}
            onOperation={setOperation}
            onProof={setProof}
            onSubmit={submitPayment}
            onTerms={setTerms}
            operation={operation}
            plan={selectedPlan}
            proof={proof}
            terms={terms}
          />
        )}
        {view === "confirmation" && selectedPlan && (
          <Confirmation
            isRenewal={isRenewal}
            onAccount={() => go("account")}
            plan={selectedPlan}
            requestNumber={requestNumber}
            status={paymentStatus}
          />
        )}
        {view === "account" && (
          <Account
            currentPlan={currentPlan}
            expiry={expiry}
            memberName={memberName}
            onChange={() => startRenewal(true)}
            onPreferences={() => go("preferences")}
            onRenew={() => startRenewal(false)}
            onRequests={() => go("requests")}
            paymentStatus={paymentStatus}
            successMessage={successMessage}
          />
        )}
        {view === "renewal" && (
          <Renewal
            currentPlan={currentPlan}
            expiry={expiry}
            onBack={() => go("account")}
            onChoose={(plan) => { setRenewPlan(plan); setRenewReview(false); }}
            onContinue={requestRenewal}
            plan={renewPlan}
            review={renewReview}
          />
        )}
        {view === "requests" && (
          <Requests
            errors={freezeErrors}
            form={freeze}
            onBack={() => go("account")}
            onChange={setFreeze}
            onSubmit={submitFreeze}
            requests={requests}
            successMessage={successMessage}
          />
        )}
        {view === "preferences" && (
          <Preferences
            enabled={waEnabled}
            expiry={expiry}
            memberName={memberName}
            notice1={notice1}
            notice7={notice7}
            onBack={() => go("account")}
            onEnabled={(value) => { setWaEnabled(value); setPrefsSaved(false); }}
            onNotice1={setNotice1}
            onNotice7={setNotice7}
            onPhone={setWaPhone}
            onSave={() => setPrefsSaved(true)}
            phone={waPhone}
            saved={prefsSaved}
          />
        )}
      </main>

      {view !== "home" && (
        <section className="admin-dock" aria-label="Controles de simulación">
          <button className="admin-toggle" onClick={() => setAdminOpen(!adminOpen)} type="button">
            <Icon name="info" size={18} />
            <span>Controles de simulación</span>
            <span aria-hidden="true">{adminOpen ? "−" : "+"}</span>
          </button>
          {adminOpen && (
            <div className="admin-content">
              <p><b>Solo para demostrar estados.</b> No forma parte de la interfaz del socio.</p>
              {(view === "confirmation" || view === "account") && (
                <div className="admin-actions">
                  <Button onClick={() => setPaymentStatus("Pendiente de validación")} variant="secondary">Pago pendiente</Button>
                  <Button onClick={confirmPayment}>Confirmar pago</Button>
                </div>
              )}
              {view === "requests" && requests.length > 0 && (
                <div className="admin-actions">
                  {(["Enviada", "En revisión", "Aprobada", "Rechazada"] as RequestStatus[]).map((status) => (
                    <Button
                      key={status}
                      onClick={() => setRequests((items) => items.map((item, index) => index === 0 ? {
                        ...item,
                        status,
                        rejection: status === "Rechazada" ? "El documento adjunto no permite validar el periodo solicitado." : undefined,
                      } : item))}
                      variant="secondary"
                    >
                      {status}
                    </Button>
                  ))}
                </div>
              )}
            </div>
          )}
        </section>
      )}

      <AuthModal
        mode={authMode}
        onClose={() => setAuthOpen(false)}
        onMode={setAuthMode}
        onSuccess={(name) => {
          setLoggedIn(true);
          setMemberName(name);
          setAuthOpen(false);
          if (selectedPlan) {
            setIsRenewal(false);
            go("payment");
          } else {
            go("account");
          }
        }}
        open={authOpen}
        plan={selectedPlan}
      />
    </div>
  );
}

function Header({
  loggedIn,
  onAccount,
  onHome,
  onLogin,
  onLogout,
  onReset,
  view,
}: {
  loggedIn: boolean;
  onAccount: () => void;
  onHome: () => void;
  onLogin: () => void;
  onLogout: () => void;
  onReset: () => void;
  view: View;
}) {
  const navigateHomeSection = (id: string) => {
    onHome();
    window.setTimeout(() => document.getElementById(id)?.scrollIntoView({ behavior: "smooth" }), 30);
  };
  return (
    <header className="site-header">
      <div className="header-inner">
        <button className="logo-button" onClick={onHome} type="button"><Logo /></button>
        {view === "home" ? (
          <nav aria-label="Navegación principal">
            <button onClick={() => navigateHomeSection("inicio")} type="button">Inicio</button>
            <button onClick={() => navigateHomeSection("servicios")} type="button">Servicios</button>
            <button onClick={() => navigateHomeSection("sedes")} type="button">Sedes</button>
            <button onClick={() => navigateHomeSection("planes")} type="button">Planes</button>
          </nav>
        ) : (
          <div className="portal-label"><span /> Portal del socio</div>
        )}
        <div className="header-actions">
          <Button icon="refresh" onClick={onReset} variant="ghost">Restablecer demo</Button>
          {loggedIn ? (
            <>
              <Button icon="user" onClick={onAccount} variant="secondary">Mi cuenta</Button>
              <button aria-label="Cerrar sesión" className="icon-button" onClick={onLogout} title="Cerrar sesión" type="button"><Icon name="logout" /></button>
            </>
          ) : (
            <Button className="header-login" icon="user" onClick={onLogin} variant="secondary">Ingresar / Registrarse</Button>
          )}
        </div>
      </div>
    </header>
  );
}

function Home({ onChoose, onLogin }: { onChoose: (plan: Plan) => void; onLogin: () => void }) {
  return (
    <>
      <section className="hero" id="inicio">
        <div className="hero-inner">
          <div className="hero-copy">
            <span className="eyebrow">FUERZA · ENERGÍA · COMUNIDAD</span>
            <h1>Entrena con propósito.<br /><em>Supera tus límites.</em></h1>
            <p>Instalaciones completas, entrenadores preparados y planes claros. Todo lo que necesitas para avanzar, ahora también desde tu portal.</p>
            <div className="hero-actions">
              <Button icon="arrow" onClick={() => document.getElementById("planes")?.scrollIntoView({ behavior: "smooth" })}>Ver planes</Button>
              <Button onClick={onLogin} variant="secondary">Ya soy socio</Button>
            </div>
            <div className="hero-proof">
              <div><b>3</b><span>Sedes en Lima</span></div>
              <div><b>6:00–23:00</b><span>Horario de atención</span></div>
              <div><b>100%</b><span>Gestión en línea</span></div>
            </div>
          </div>
          <div className="hero-media">
            <img alt="Zona de pesas de un gimnasio moderno" src={HERO_IMAGE} />
            <div className="hero-card"><span>HOY</span><b>Es un buen día<br />para empezar.</b></div>
          </div>
        </div>
      </section>

      <section className="plans-section" id="planes">
        <div className="section-heading">
          <div>
            <span className="eyebrow">MEMBRESÍAS</span>
            <h2>Elige el ritmo que va contigo</h2>
          </div>
          <p>Compara duración, precio total y beneficios. Sin cargos ni condiciones adicionales en esta demostración.</p>
        </div>
        <div className="plan-grid">
          {PLANS.map((plan, index) => (
            <article className={`plan-card ${index === 1 ? "featured" : ""}`} key={plan.id}>
              {index === 1 && <span className="plan-tag">MÁS ELEGIDO</span>}
              <span className="plan-number">0{index + 1}</span>
              <h3>{plan.name}</h3>
              <p className="duration">{plan.duration} de acceso</p>
              <div className="price"><small>S/</small><strong>{plan.price}</strong></div>
              <p className="price-note">Importe total</p>
              <ul>
                <li><Icon name="check" size={17} /> Sala de musculación</li>
                <li><Icon name="check" size={17} /> Clases grupales</li>
                <li><Icon name="check" size={17} /> Acceso a todas las sedes</li>
                <li><Icon name="check" size={17} /> Portal de autoservicio</li>
              </ul>
              <Button icon="arrow" onClick={() => onChoose(plan)} variant={index === 1 ? "primary" : "secondary"}>Contratar</Button>
            </article>
          ))}
        </div>
      </section>

      <section className="services" id="servicios">
        <div>
          <span className="eyebrow">TODO EN UN SOLO LUGAR</span>
          <h2>Entrenamiento sin distracciones</h2>
          <p>Espacios diseñados para que cada sesión cuente, desde fuerza y cardio hasta clases dirigidas.</p>
        </div>
        <div className="service-list">
          <article><span>01</span><h3>Musculación</h3><p>Equipamiento completo para una progresión segura.</p></article>
          <article><span>02</span><h3>Clases grupales</h3><p>Sesiones guiadas para entrenar con energía.</p></article>
          <article><span>03</span><h3>Acompañamiento</h3><p>Orientación de entrenadores en sala.</p></article>
        </div>
      </section>

      <section className="locations" id="sedes">
        <div>
          <span className="eyebrow">NUESTRAS SEDES</span>
          <h2>Tu entrenamiento, más cerca</h2>
        </div>
        <div className="location-grid">
          {["San Miguel", "Pueblo Libre", "Magdalena"].map((location) => (
            <article key={location}><span>LIMA</span><h3>{location}</h3><p>Lunes a domingo · 6:00 a 23:00</p></article>
          ))}
        </div>
      </section>

      <section className="demo-guide">
        <div>
          <span className="eyebrow">GUÍA DE LA DEMO</span>
          <h2>Recorre el prototipo</h2>
        </div>
        <div className="guide-grid">
          <article><b>A</b><p>Elige un plan, crea una cuenta y completa el pago.</p></article>
          <article><b>B</b><p>Ingresa como socio, consulta tu vencimiento y renueva.</p></article>
          <article><b>C</b><p>Solicita un congelamiento y sigue su revisión.</p></article>
          <article><b>D</b><p>Activa, previsualiza y desactiva recordatorios.</p></article>
        </div>
        <p className="demo-data"><b>Acceso ficticio:</b> juan.perez@ejemplo.com · contraseña: Imperio2026 &nbsp;|&nbsp; Fecha de referencia: 05/10/2026. Pagos, revisiones y WhatsApp son simulados.</p>
      </section>
      <footer><Logo /><p>Prototipo académico · Datos ficticios · Imperio Gym 2026</p></footer>
    </>
  );
}

function AuthModal({
  mode,
  onClose,
  onMode,
  onSuccess,
  open,
  plan,
}: {
  mode: "login" | "register";
  onClose: () => void;
  onMode: (mode: "login" | "register") => void;
  onSuccess: (name: string) => void;
  open: boolean;
  plan: Plan | null;
}) {
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [recovery, setRecovery] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [form, setForm] = useState({ name: "", dni: "", email: "", phone: "", password: "", confirm: "", identifier: "" });
  const titleRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    if (!open) return;
    titleRef.current?.focus();
    const handleEscape = (event: KeyboardEvent) => event.key === "Escape" && onClose();
    document.addEventListener("keydown", handleEscape);
    return () => document.removeEventListener("keydown", handleEscape);
  }, [open, onClose]);

  if (!open) return null;

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const next: Record<string, string> = {};
    if (mode === "login") {
      if (!form.identifier.trim()) next.identifier = "Ingresa tu correo o DNI.";
      if (!form.password) next.password = "Ingresa tu contraseña.";
      if (form.identifier && !["juan.perez@ejemplo.com", "74125896"].includes(form.identifier)) next.identifier = "Usa el acceso ficticio indicado debajo.";
      if (form.password && form.password !== "Imperio2026") next.password = "La contraseña de demostración no coincide.";
    } else {
      if (form.name.trim().length < 3) next.name = "Ingresa tu nombre completo.";
      if (!/^\d{8}$/.test(form.dni)) next.dni = "El DNI debe tener 8 dígitos.";
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) next.email = "Ingresa un correo válido.";
      if (!/^\d{9}$/.test(form.phone)) next.phone = "Ingresa 9 dígitos.";
      if (form.password.length < 8) next.password = "Usa al menos 8 caracteres.";
      if (form.confirm !== form.password) next.confirm = "Las contraseñas no coinciden.";
    }
    setErrors(next);
    if (Object.keys(next).length) return;
    setLoading(true);
    window.setTimeout(() => {
      setLoading(false);
      onSuccess(mode === "register" ? form.name : "Juan Pérez");
    }, 700);
  };

  return (
    <div aria-modal="true" className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()} role="dialog">
      <div className="auth-modal">
        <button aria-label="Cerrar" className="modal-close" onClick={onClose} type="button"><Icon name="close" /></button>
        <div className="modal-brand"><Logo /></div>
        <h2 ref={titleRef} tabIndex={-1}>{plan ? "Continúa con tu plan" : "Bienvenido a tu portal"}</h2>
        <p className="modal-subtitle">{plan ? "Identifícate para mantener tu selección y continuar al pago." : "Consulta y gestiona tu membresía en un solo lugar."}</p>
        {plan && <div className="selected-plan"><span>PLAN SELECCIONADO</span><b>{plan.name}</b><strong>S/ {plan.price}</strong></div>}
        <div className="auth-tabs" role="tablist">
          <button aria-selected={mode === "login"} className={mode === "login" ? "active" : ""} onClick={() => { onMode("login"); setErrors({}); }} role="tab" type="button">Iniciar sesión</button>
          <button aria-selected={mode === "register"} className={mode === "register" ? "active" : ""} onClick={() => { onMode("register"); setErrors({}); }} role="tab" type="button">Crear cuenta</button>
        </div>
        <form onSubmit={submit}>
          {mode === "register" && (
            <div className="form-grid">
              <Field error={errors.name} label="Nombre completo"><input autoComplete="name" onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Ej. Ana Torres" value={form.name} /></Field>
              <Field error={errors.dni} label="DNI"><input inputMode="numeric" maxLength={8} onChange={(e) => setForm({ ...form, dni: e.target.value.replace(/\D/g, "") })} placeholder="8 dígitos" value={form.dni} /></Field>
              <Field error={errors.email} label="Correo electrónico"><input autoComplete="email" onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="nombre@correo.com" type="email" value={form.email} /></Field>
              <Field error={errors.phone} label="Teléfono"><input inputMode="tel" maxLength={9} onChange={(e) => setForm({ ...form, phone: e.target.value.replace(/\D/g, "") })} placeholder="999 000 000" value={form.phone} /></Field>
            </div>
          )}
          {mode === "login" && (
            <Field error={errors.identifier} label="Correo o DNI (identificador)">
              <input autoComplete="username" onChange={(e) => setForm({ ...form, identifier: e.target.value })} placeholder="juan.perez@ejemplo.com" value={form.identifier} />
            </Field>
          )}
          <Field error={errors.password} hint="El DNI nunca se usa como contraseña." label="Contraseña">
            <div className="password-wrap">
              <input autoComplete={mode === "login" ? "current-password" : "new-password"} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="Mínimo 8 caracteres" type={showPassword ? "text" : "password"} value={form.password} />
              <button aria-label={showPassword ? "Ocultar contraseña" : "Mostrar contraseña"} onClick={() => setShowPassword(!showPassword)} type="button"><Icon name={showPassword ? "eyeOff" : "eye"} /></button>
            </div>
          </Field>
          {mode === "register" && (
            <Field error={errors.confirm} label="Confirmar contraseña">
              <input autoComplete="new-password" onChange={(e) => setForm({ ...form, confirm: e.target.value })} placeholder="Repite tu contraseña" type={showPassword ? "text" : "password"} value={form.confirm} />
            </Field>
          )}
          {mode === "login" && (
            <>
              <button className="text-link" onClick={() => setRecovery(true)} type="button">¿Olvidaste tu contraseña?</button>
              {recovery && <div className="inline-message">Recuperación simulada: te enviamos instrucciones al correo registrado.</div>}
              <div className="demo-credentials"><b>Acceso de demostración</b><span>juan.perez@ejemplo.com</span><span>Contraseña: Imperio2026</span></div>
            </>
          )}
          <Button className="full" disabled={loading} type="submit">{loading ? "Validando…" : mode === "login" ? "Ingresar a mi cuenta" : "Crear cuenta y continuar"}</Button>
        </form>
        <button className="modal-back" onClick={onClose} type="button"><Icon name="back" size={16} /> Volver sin perder mi selección</button>
      </div>
    </div>
  );
}

function Payment(props: {
  card: { number: string; expiry: string; cvv: string };
  errors: Record<string, string>;
  isRenewal: boolean;
  loading: boolean;
  memberName: string;
  method: PaymentMethod;
  onBack: () => void;
  onCard: (value: { number: string; expiry: string; cvv: string }) => void;
  onMethod: (method: PaymentMethod) => void;
  onOperation: (value: string) => void;
  onProof: (value: string) => void;
  onSubmit: (event: FormEvent) => void;
  onTerms: (value: boolean) => void;
  operation: string;
  plan: Plan;
  proof: string;
  terms: boolean;
}) {
  const { card, errors, loading, memberName, method, operation, plan, proof, terms } = props;
  return (
    <section className="portal-page">
      <div className="portal-container">
        <Progress step={3} />
        <div className="page-title">
          <div><span className="eyebrow">{props.isRenewal ? "RENOVACIÓN" : "CONTRATACIÓN"}</span><h1>Completa tu pago</h1><p>Revisa la selección y utiliza únicamente datos de prueba.</p></div>
          <Status tone="neutral">Entorno de demostración</Status>
        </div>
        <div className="checkout-grid">
          <form className="payment-panel" onSubmit={props.onSubmit}>
            <h2>Método de pago</h2>
            <div className="payment-methods">
              {[
                { id: "card" as PaymentMethod, icon: "card", label: "Tarjeta" },
                { id: "wallet" as PaymentMethod, icon: "phone", label: "Yape / Plin" },
                { id: "transfer" as PaymentMethod, icon: "bank", label: "Transferencia" },
              ].map((item) => (
                <button aria-pressed={method === item.id} className={method === item.id ? "active" : ""} key={item.id} onClick={() => { props.onMethod(item.id); }} type="button">
                  <Icon name={item.icon} /><span>{item.label}</span>
                </button>
              ))}
            </div>
            <div className="test-notice"><Icon name="info" /><p><b>Datos de prueba.</b> Este prototipo no procesa ni almacena información bancaria real.</p></div>
            {method === "card" ? (
              <div className="form-grid payment-fields">
                <Field error={errors.number} label="Número de tarjeta de prueba">
                  <input inputMode="numeric" maxLength={19} onChange={(e) => props.onCard({ ...card, number: e.target.value })} placeholder="4242 4242 4242 4242" value={card.number} />
                </Field>
                <Field error={errors.expiry} label="Vencimiento de prueba"><input onChange={(e) => props.onCard({ ...card, expiry: e.target.value })} placeholder="MM/AA" value={card.expiry} /></Field>
                <Field error={errors.cvv} label="CVV de prueba"><input inputMode="numeric" maxLength={3} onChange={(e) => props.onCard({ ...card, cvv: e.target.value.replace(/\D/g, "") })} placeholder="123" value={card.cvv} /></Field>
              </div>
            ) : (
              <div className="form-grid payment-fields">
                <Field error={errors.operation} label="Número de operación">
                  <input onChange={(e) => props.onOperation(e.target.value)} placeholder={method === "wallet" ? "Ej. YP-123456" : "Ej. TR-123456"} value={operation} />
                </Field>
                <Field error={errors.proof} hint={proof ? `Archivo seleccionado: ${proof}` : undefined} label="Comprobante">
                  <input accept=".jpg,.jpeg,.png,.pdf" onChange={(e) => props.onProof(e.target.files?.[0]?.name || "")} type="file" />
                </Field>
              </div>
            )}
            {method !== "card" && <p className="pending-note"><Icon name="info" size={18} /> El pago quedará pendiente de validación. Tu membresía no cambiará hasta su confirmación.</p>}
            <label className={`check-row ${errors.terms ? "has-error" : ""}`}>
              <input checked={terms} onChange={(e) => props.onTerms(e.target.checked)} type="checkbox" />
              <span>Acepto las condiciones de la membresía y el tratamiento de mis datos para esta solicitud.</span>
            </label>
            {errors.terms && <span className="error-text">{errors.terms}</span>}
            <div className="form-actions">
              <Button disabled={loading} icon="back" onClick={props.onBack} type="button" variant="secondary">Volver</Button>
              <Button disabled={loading} icon="arrow" type="submit">{loading ? "Procesando solicitud…" : "Continuar a confirmación"}</Button>
            </div>
          </form>
          <aside className="order-summary">
            <span className="eyebrow">RESUMEN</span>
            <h2>{plan.name}</h2>
            <dl><div><dt>Duración</dt><dd>{plan.duration}</dd></div><div><dt>Socio</dt><dd>{memberName}</dd></div><div><dt>Inicio</dt><dd>{props.isRenewal ? formatDate("2026-10-10") : "Al confirmar el pago"}</dd></div></dl>
            <div className="summary-total"><span>Importe total</span><strong>S/ {plan.price}</strong></div>
            <p>No se agregan descuentos, cargos ni condiciones de permanencia.</p>
          </aside>
        </div>
      </div>
    </section>
  );
}

function Confirmation({ isRenewal, onAccount, plan, requestNumber, status }: { isRenewal: boolean; onAccount: () => void; plan: Plan; requestNumber: string; status: PaymentStatus }) {
  const confirmed = status === "Confirmado";
  return (
    <section className="portal-page">
      <div className="portal-container narrow">
        <Progress step={4} />
        <div className={`confirmation-card ${confirmed ? "confirmed" : "pending"}`}>
          <div className="confirmation-icon"><Icon name={confirmed ? "check" : "calendar"} size={30} /></div>
          <span className="eyebrow">{confirmed ? "PAGO CONFIRMADO" : "SOLICITUD RECIBIDA"}</span>
          <h1>{confirmed ? (isRenewal ? "Tu renovación fue confirmada" : "Tu membresía está activa") : "Pago pendiente de validación"}</h1>
          <p>{confirmed ? "El cambio ya se refleja en tu cuenta y puedes consultar la nueva vigencia." : "Recibimos tu comprobante. Lo revisaremos antes de activar o renovar tu membresía."}</p>
          <div className="confirmation-details">
            <div><span>N.º de solicitud</span><b>{requestNumber}</b></div>
            <div><span>Plan</span><b>{plan.name}</b></div>
            <div><span>Importe</span><b>S/ {plan.price}</b></div>
            <div><span>Estado</span><Status tone={confirmed ? "success" : "warning"}>{status}</Status></div>
          </div>
          <div className="next-step"><b>Siguiente paso</b><p>{confirmed ? "Consulta tu nueva vigencia y comprobante en Mi cuenta." : "Espera la validación. La fecha actual se mantiene sin cambios mientras el pago esté pendiente."}</p></div>
          <Button icon="arrow" onClick={onAccount}>Ir a mi cuenta</Button>
        </div>
      </div>
    </section>
  );
}

function Account(props: {
  currentPlan: Plan;
  expiry: string;
  memberName: string;
  onChange: () => void;
  onPreferences: () => void;
  onRenew: () => void;
  onRequests: () => void;
  paymentStatus: PaymentStatus;
  successMessage: string;
}) {
  const [historyFilter, setHistoryFilter] = useState<"all" | "failed">("all");
  const daysRemaining = daysFromReference(props.expiry);
  const membershipState = daysRemaining <= 0 ? "Vencida" : daysRemaining <= 7 ? "Próxima a vencer" : "Activa";
  const stateTone = membershipState === "Activa" ? "success" : membershipState === "Vencida" ? "danger" : "warning";
  return (
    <section className="portal-page account-page">
      <div className="portal-container">
        {props.successMessage && <div className="success-banner"><Icon name="check" /><span>{props.successMessage}</span></div>}
        <div className="account-heading"><div><span className="eyebrow">MI CUENTA</span><h1>Hola, {props.memberName.split(" ")[0]}</h1><p>Esta es la situación de tu membresía al 05/10/2026.</p></div><div className="member-id">SOCIO <b>#IG-20418</b></div></div>
        <div className="membership-card">
          <div className="membership-main">
            <Status tone={stateTone}>{membershipState}</Status>
            <h2>{props.currentPlan.name}</h2>
            <p>Tu acceso está activo en todas nuestras sedes.</p>
            <div className="membership-actions"><Button onClick={props.onRenew}>Renovar membresía</Button><Button onClick={props.onChange} variant="secondary">Cambiar plan</Button></div>
          </div>
          <div className="expiry-block"><span>VENCE EL</span><strong>{formatDate(props.expiry)}</strong><div><b>{Math.max(0, daysRemaining)}</b><span>{daysRemaining === 1 ? "día" : "días"}<br />restantes</span></div></div>
        </div>
        <div className="quick-actions">
          <button onClick={() => document.getElementById("history")?.scrollIntoView({ behavior: "smooth" })} type="button"><span><Icon name="card" /></span><div><b>Historial de pagos</b><small>Consulta tus movimientos</small></div><Icon name="arrow" /></button>
          <button onClick={props.onRequests} type="button"><span><Icon name="file" /></span><div><b>Mis solicitudes</b><small>Congelamiento y seguimiento</small></div><Icon name="arrow" /></button>
          <button onClick={props.onPreferences} type="button"><span><Icon name="bell" /></span><div><b>Notificaciones</b><small>Configura tus recordatorios</small></div><Icon name="arrow" /></button>
        </div>
        <section className="history-card" id="history">
          <div className="card-heading"><div><span className="eyebrow">MOVIMIENTOS</span><h2>Historial de pagos</h2></div><div className="mini-tabs"><button className={historyFilter === "all" ? "active" : ""} onClick={() => setHistoryFilter("all")} type="button">Todos</button><button className={historyFilter === "failed" ? "active" : ""} onClick={() => setHistoryFilter("failed")} type="button">Observados</button></div></div>
          {historyFilter === "failed" ? (
            <div className="empty-state"><Icon name="file" size={28} /><b>No hay pagos observados</b><p>Cuando exista un movimiento con incidencias, aparecerá aquí.</p></div>
          ) : (
            <div className="table-wrap"><table><thead><tr><th>Fecha</th><th>Concepto</th><th>Importe</th><th>Estado</th></tr></thead><tbody><tr><td>05/09/2026</td><td>Plan mensual</td><td>S/ 89.99</td><td><Status tone="success">Confirmado</Status></td></tr><tr><td>05/10/2026</td><td>Solicitud actual</td><td>S/ {props.currentPlan.price}</td><td><Status tone={props.paymentStatus === "Confirmado" ? "success" : "warning"}>{props.paymentStatus}</Status></td></tr></tbody></table></div>
          )}
        </section>
      </div>
    </section>
  );
}

function Renewal({ currentPlan, expiry, onBack, onChoose, onContinue, plan, review }: { currentPlan: Plan; expiry: string; onBack: () => void; onChoose: (plan: Plan) => void; onContinue: () => void; plan: Plan; review: boolean }) {
  const proposedEnd = addMonths(expiry, plan.months);
  const daysRemaining = Math.max(0, daysFromReference(expiry));
  const membershipState = daysRemaining <= 0 ? "Vencida" : daysRemaining <= 7 ? "Próxima a vencer" : "Activa";
  const stateTone = membershipState === "Activa" ? "success" : membershipState === "Vencida" ? "danger" : "warning";
  return (
    <section className="portal-page">
      <div className="portal-container">
        <button className="back-link" onClick={onBack} type="button"><Icon name="back" /> Volver a Mi cuenta</button>
        <div className="page-title"><div><span className="eyebrow">MEMBRESÍA</span><h1>Renueva o cambia tu plan</h1><p>Los {daysRemaining} días pendientes se conservan: la nueva vigencia inicia al terminar la actual.</p></div></div>
        <div className="current-plan-strip"><div><span>PLAN ACTUAL</span><b>{currentPlan.name}</b></div><div><span>VENCIMIENTO</span><b>{formatDate(expiry)}</b></div><Status tone={stateTone}>{membershipState}</Status></div>
        <h2 className="selection-title">Selecciona una opción</h2>
        <div className="renew-options">
          {PLANS.map((item) => <button aria-pressed={plan.id === item.id} className={plan.id === item.id ? "active" : ""} key={item.id} onClick={() => onChoose(item)} type="button"><span>{item.duration}</span><b>{item.name}</b><strong>S/ {item.price}</strong><small>Importe total</small><i>{plan.id === item.id ? <Icon name="check" size={16} /> : null}</i></button>)}
        </div>
        <div className={`renew-summary ${review ? "reviewing" : ""}`}>
          <div><span className="eyebrow">{review ? "REVISA ANTES DE CONFIRMAR" : "RESUMEN PROPUESTO"}</span><h2>{plan.name}</h2><p>Inicio: <b>{formatDate(expiry)}</b> · Fin: <b>{formatDate(proposedEnd)}</b></p><small>Mientras la solicitud esté pendiente, tu vencimiento actual no cambiará.</small></div>
          <div className="renew-total"><span>Total</span><b>S/ {plan.price}</b><Button onClick={onContinue}>{review ? "Confirmar y elegir pago" : "Revisar solicitud"}</Button></div>
        </div>
      </div>
    </section>
  );
}

function Requests(props: {
  errors: Record<string, string>;
  form: { kind: string; start: string; end: string; reason: string; file: string };
  onBack: () => void;
  onChange: (value: { kind: string; start: string; end: string; reason: string; file: string }) => void;
  onSubmit: (event: FormEvent) => void;
  requests: FreezeRequest[];
  successMessage: string;
}) {
  const { errors, form } = props;
  return (
    <section className="portal-page">
      <div className="portal-container">
        <button className="back-link" onClick={props.onBack} type="button"><Icon name="back" /> Volver a Mi cuenta</button>
        <div className="page-title"><div><span className="eyebrow">SOLICITUDES</span><h1>Gestiona un congelamiento</h1><p>Registra el periodo y consulta cada etapa de la revisión.</p></div></div>
        {props.successMessage && props.successMessage.startsWith("Solicitud") && <div className="success-banner"><Icon name="check" /><span>{props.successMessage}</span></div>}
        <div className="requests-grid">
          <form className="request-form" onSubmit={props.onSubmit}>
            <h2>Nueva solicitud</h2>
            <Field label="Tipo de solicitud"><div className="segmented"><button className={form.kind === "Salud" ? "active" : ""} onClick={() => props.onChange({ ...form, kind: "Salud" })} type="button">Por salud</button><button className={form.kind === "Viaje" ? "active" : ""} onClick={() => props.onChange({ ...form, kind: "Viaje" })} type="button">Por viaje</button></div></Field>
            <div className="form-grid">
              <Field error={errors.start} label="Fecha de inicio"><input min="2026-10-05" onChange={(e) => props.onChange({ ...form, start: e.target.value })} type="date" value={form.start} /></Field>
              <Field error={errors.end} label="Fecha final"><input min={form.start || "2026-10-06"} onChange={(e) => props.onChange({ ...form, end: e.target.value })} type="date" value={form.end} /></Field>
            </div>
            <Field error={errors.reason} label="Motivo"><textarea onChange={(e) => props.onChange({ ...form, reason: e.target.value })} placeholder="Cuéntanos brevemente por qué necesitas congelar tu membresía." rows={4} value={form.reason} /></Field>
            <Field hint={form.file ? `Archivo seleccionado: ${form.file}` : "PDF, JPG o PNG. Adjúntalo cuando corresponda."} label="Documento de sustento (opcional)"><input accept=".pdf,.jpg,.jpeg,.png" onChange={(e) => props.onChange({ ...form, file: e.target.files?.[0]?.name || "" })} type="file" /></Field>
            <Button className="full" type="submit">Enviar solicitud para revisión</Button>
            <p className="form-disclaimer"><Icon name="info" size={18} /> El envío no implica aprobación automática.</p>
          </form>
          <section className="tracking-panel">
            <div><span className="eyebrow">SEGUIMIENTO</span><h2>Mis solicitudes</h2></div>
            {props.requests.length === 0 ? <div className="empty-state"><Icon name="file" /><b>Aún no tienes solicitudes</b><p>Las solicitudes que envíes aparecerán aquí.</p></div> : props.requests.map((request) => (
              <article className="request-item" key={request.id}>
                <div className="request-top"><div><small>{request.id}</small><h3>Congelamiento por {request.kind.toLowerCase()}</h3></div><Status tone={request.status === "Aprobada" ? "success" : request.status === "Rechazada" ? "danger" : "warning"}>{request.status}</Status></div>
                <p>{formatDate(request.start)} — {formatDate(request.end)}</p>
                <div className="timeline">
                  {["Enviada", "En revisión", request.status === "Rechazada" ? "Rechazada" : "Aprobada"].map((step, index) => {
                    const current = request.status === step || (request.status === "Aprobada" && index < 3) || (request.status === "En revisión" && index < 2) || (request.status === "Rechazada" && index < 3);
                    return <span className={current ? "done" : ""} key={step}><i>{current ? <Icon name="check" size={12} /> : index + 1}</i><b>{step}</b></span>;
                  })}
                </div>
                {request.rejection && <div className="rejection"><b>Motivo del rechazo</b><p>{request.rejection}</p></div>}
              </article>
            ))}
          </section>
        </div>
      </div>
    </section>
  );
}

function Preferences(props: {
  enabled: boolean;
  expiry: string;
  memberName: string;
  notice1: boolean;
  notice7: boolean;
  onBack: () => void;
  onEnabled: (value: boolean) => void;
  onNotice1: (value: boolean) => void;
  onNotice7: (value: boolean) => void;
  onPhone: (value: string) => void;
  onSave: () => void;
  phone: string;
  saved: boolean;
}) {
  return (
    <section className="portal-page">
      <div className="portal-container narrow">
        <button className="back-link" onClick={props.onBack} type="button"><Icon name="back" /> Volver a Mi cuenta</button>
        <div className="page-title"><div><span className="eyebrow">PREFERENCIAS</span><h1>Recordatorios de vencimiento</h1><p>WhatsApp es un apoyo opcional. Todas las gestiones continúan dentro del portal.</p></div></div>
        {props.saved && <div className="success-banner"><Icon name="check" /><span>{props.enabled ? "Preferencias guardadas. Los recordatorios están activos." : "Preferencias guardadas. Los recordatorios están desactivados."}</span></div>}
        <div className="preferences-card">
          <div className="preference-toggle">
            <span className="whatsapp-mark"><Icon name="phone" /></span>
            <div><h2>Recordatorios por WhatsApp</h2><p>Recibe avisos antes del vencimiento de tu membresía.</p></div>
            <label className="switch"><input checked={props.enabled} onChange={(e) => props.onEnabled(e.target.checked)} type="checkbox" /><span /><b>{props.enabled ? "Activados" : "Desactivados"}</b></label>
          </div>
          <div className={`preference-fields ${!props.enabled ? "disabled" : ""}`}>
            <Field label="Número de WhatsApp"><input disabled={!props.enabled} onChange={(e) => props.onPhone(e.target.value)} value={props.phone} /></Field>
            <fieldset disabled={!props.enabled}><legend>¿Cuándo quieres recibirlos?</legend><label><input checked={props.notice7} onChange={(e) => props.onNotice7(e.target.checked)} type="checkbox" /> 7 días antes</label><label><input checked={props.notice1} onChange={(e) => props.onNotice1(e.target.checked)} type="checkbox" /> 1 día antes</label></fieldset>
          </div>
          {props.enabled && (
            <div className="message-preview">
              <div className="preview-heading"><span>VISTA PREVIA DEL MENSAJE</span><small>Simulación</small></div>
              <div className="message-bubble"><p>Imperio Gym: Hola, {props.memberName.split(" ")[0]}. Tu membresía vence el <b>{formatDate(props.expiry)}</b>. Puedes consultar tu estado y renovar desde Mi cuenta.</p><button onClick={() => window.alert("En la experiencia real, este enlace abre la renovación dentro del portal.")} type="button">Renovar en el portal →</button></div>
            </div>
          )}
          <div className="integration-note"><Icon name="info" /><p>El envío real requiere integración con WhatsApp Business Platform. Esta demo solo simula la configuración y el mensaje.</p></div>
          <Button className="full" disabled={props.enabled && (!props.phone || (!props.notice1 && !props.notice7))} onClick={props.onSave}>Guardar preferencias</Button>
        </div>
      </div>
    </section>
  );
}
