package pe.imperio.gym;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import pe.imperio.gym.data.Store;
import pe.imperio.gym.data.UserRepository;
import pe.imperio.gym.domain.Attendance;
import pe.imperio.gym.domain.Branch;
import pe.imperio.gym.domain.Payment;
import pe.imperio.gym.domain.PrivateFile;
import pe.imperio.gym.domain.Reminder;
import pe.imperio.gym.domain.ResetToken;
import pe.imperio.gym.domain.UserAccount;
import pe.imperio.gym.service.BranchService;
import pe.imperio.gym.service.GymService;
import pe.imperio.gym.service.IdentityService;
import pe.imperio.gym.service.ReminderService;
import pe.imperio.gym.web.Forms;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GymIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    GymService gym;
    @Autowired
    IdentityService identity;
    @Autowired
    Store db;
    @Autowired
    UserRepository users;
    @Autowired
    ReminderService reminders;
    @Autowired
    BranchService branches;
    static final AtomicInteger sequence = new AtomicInteger(
            20000000 + new java.security.SecureRandom().nextInt(9000000));

    UserAccount customer() {
        String dni = Integer.toString(sequence.incrementAndGet());
        return identity.register(new Forms.Registration("Cliente Test", dni, dni + "@example.test", "+51999000000",
                "PasswordTest2026!", "PasswordTest2026!"));
    }

    Long admin() {
        return users.findByEmailIgnoreCaseOrDni("admin@imperio.test", "10000000").orElseThrow().id;
    }

    MockMultipartFile proof() {
        return new MockMultipartFile("proof", "receipt.pdf", "application/pdf",
                "%PDF-1.4\n1 0 obj <<>> endobj\n%%EOF".getBytes());
    }

    @Test
    void calendarMonthsClampWithoutOverflow() {
        assertThat(GymService.addMonths(LocalDate.of(2026, 1, 31), 1)).isEqualTo("2026-02-28");
        assertThat(GymService.addMonths(LocalDate.of(2024, 1, 31), 1)).isEqualTo("2024-02-29");
        assertThat(GymService.addMonths(LocalDate.of(2026, 1, 31), 2)).isEqualTo("2026-03-31");
    }

    @Test
    void visitorRegistrationPreservesPlanAndCardPaymentIsIdempotent() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(get("/choose/2").session(session)).andExpect(redirectedUrl("/checkout"));
        mvc.perform(get("/checkout").session(session)).andExpect(status().is3xxRedirection());
        String dni = Integer.toString(sequence.incrementAndGet());
        mvc.perform(post("/register").session(session).with(csrf()).param("name", "Nuevo Socio").param("dni", dni)
                .param("email", dni + "@example.test").param("phone", "+51999000000")
                .param("password", "PasswordTest2026!").param("confirmation", "PasswordTest2026!"))
                .andExpect(redirectedUrl("/login"));
        mvc.perform(
                post("/login").session(session).with(csrf()).param("login", dni).param("password", "PasswordTest2026!"))
                .andExpect(redirectedUrl("/checkout"));
        String email = dni + "@example.test";
        mvc.perform(get("/checkout").session(session).with(user(email).roles("CLIENTE"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("114.99")));
        var draft = (GymService.Draft) session.getAttribute("draft");
        var first = mvc
                .perform(multipart("/checkout").session(session).with(user(email).roles("CLIENTE")).with(csrf())
                        .param("key", draft.key()).param("branchId", "1").param("method", "CARD_TEST")
                        .param("accepted", "true"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        mvc.perform(multipart("/checkout").session(session).with(user(email).roles("CLIENTE")).with(csrf())
                .param("key", draft.key()).param("branchId", "1").param("method", "CARD_TEST")
                .param("accepted", "true")).andExpect(redirectedUrl(first));
        var u = users.findByEmailIgnoreCaseOrDni(email, dni).orElseThrow();
        assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(gym.today().plusMonths(2));
        assertThat(db.count("select count(p) from Payment p where p.userId=?1", u.id)).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "admin@imperio.test", roles = "ADMIN")
    void manualConfirmationRenewalAndRejectionAreAtomic() throws Exception {
        var u = customer();
        var first = gym.pay(u.id, 1L, gym.draft(1L), "WALLET", "OP-" + UUID.randomUUID(), proof());
        assertThat(gym.membership(u.id)).isEmpty();
        gym.reviewPayment(first.id, admin(), true, "Validado");
        LocalDate expiry = gym.membership(u.id).orElseThrow().expiresOn;
        var renewal = gym.pay(u.id, 1L, gym.draft(2L), "TRANSFER", "OP-" + UUID.randomUUID(), proof());
        assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(expiry);
        gym.reviewPayment(renewal.id, admin(), true, "Renovación validada");
        assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(expiry.plusMonths(2));
        assertThatThrownBy(() -> gym.reviewPayment(renewal.id, admin(), true, "Otra vez"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(expiry.plusMonths(2));
        var rejected = gym.pay(u.id, 1L, gym.draft(3L), "WALLET", "OP-" + UUID.randomUUID(), proof());
        assertThatThrownBy(() -> gym.reviewPayment(rejected.id, admin(), false, ""))
                .isInstanceOf(IllegalArgumentException.class);
        gym.reviewPayment(rejected.id, admin(), false, "Comprobante no válido");
        assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(expiry.plusMonths(2));
        mvc.perform(get("/account/payments/" + renewal.id).with(user(u.email).roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Confirmado")));
    }

    @Test
    @WithMockUser(username = "admin@imperio.test", roles = "ADMIN")
    void freezeRequiresReviewAndResponseIsVisible() throws Exception {
        var u = customer();
        gym.pay(u.id, 1L, gym.draft(1L), "CARD_TEST", null, null);
        var expiry = gym.membership(u.id).orElseThrow().expiresOn;
        var r = gym.request(u.id,
                new Forms.Freeze("TRAVEL", gym.today().plusDays(1), gym.today().plusDays(3), "Viaje de prueba"), null);
        assertThatThrownBy(() -> gym.reviewRequest(r.id, admin(), "APPROVED", ""))
                .isInstanceOf(IllegalArgumentException.class);
        gym.reviewRequest(r.id, admin(), "IN_REVIEW", "Documentos en revisión");
        gym.reviewRequest(r.id, admin(), "APPROVED", "Solicitud aprobada");
        assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(expiry);
        assertThatThrownBy(() -> gym.reviewRequest(r.id, admin(), "APPROVED", ""))
                .isInstanceOf(IllegalArgumentException.class);
        mvc.perform(get("/account/requests/" + r.id).with(user(u.email).roles("CLIENTE"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Solicitud aprobada")));
        assertThatThrownBy(() -> gym.request(u.id,
                new Forms.Freeze("TRAVEL", gym.today().plusDays(2), gym.today().plusDays(4), "Superpuesto"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void preferencesRescheduleAndCancelOldExpiry() {
        var u = customer();
        gym.pay(u.id, 1L, gym.draft(1L), "CARD_TEST", null, null);
        var old = gym.membership(u.id).orElseThrow().expiresOn;
        assertThat(db.count("select count(r) from Reminder r where r.userId=?1", u.id)).isZero();
        gym.preferences(u.id, new Forms.Preferences("+51999000000", true, true, true));
        assertThat(db.count("select count(r) from Reminder r where r.userId=?1 and r.status='SCHEDULED'", u.id))
                .isEqualTo(2);
        gym.pay(u.id, 1L, gym.draft(2L), "CARD_TEST", null, null);
        assertThat(db.count(
                "select count(r) from Reminder r where r.userId=?1 and r.expiryDate=?2 and r.status='CANCELLED'", u.id,
                old)).isEqualTo(2);
        gym.preferences(u.id, new Forms.Preferences("+51999000000", false, true, true));
        assertThat(db.count("select count(r) from Reminder r where r.userId=?1 and r.status='SCHEDULED'", u.id))
                .isZero();
    }

    @Test
    void permissionsCsrfAndFilesAreEnforced() throws Exception {
        var u = customer();
        var another = customer();
        var p = gym.pay(u.id, 1L, gym.draft(1L), "WALLET", "OP-" + UUID.randomUUID(), proof());
        var f = db.one(PrivateFile.class, "select f from PrivateFile f where f.paymentId=?1", p.id).orElseThrow();
        mvc.perform(get("/admin").with(user(u.email).roles("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/account/payments/" + p.id).with(user(another.email).roles("CLIENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/files/" + f.id).with(user(another.email).roles("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/files/" + f.id).with(user(u.email).roles("CLIENTE"))).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(post("/account/preferences").with(user(u.email).roles("CLIENTE")).param("phone", "+51999000000"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/account").with(user("admin@imperio.test").roles("ADMIN"))).andExpect(status().isForbidden());
    }

    @Test
    void uploadsAndDatesAreValidatedAndRolledBack() {
        var u = customer();
        var invalid = new MockMultipartFile("proof", "fake.pdf", "application/pdf",
                "<script>alert(1)</script>".getBytes());
        assertThatThrownBy(() -> gym.pay(u.id, 1L, gym.draft(1L), "WALLET", "OP-" + UUID.randomUUID(), invalid))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(db.count("select count(p) from Payment p where p.userId=?1", u.id)).isZero();
        gym.pay(u.id, 1L, gym.draft(1L), "CARD_TEST", null, null);
        assertThatThrownBy(() -> gym.request(u.id,
                new Forms.Freeze("TRAVEL", gym.today().minusDays(1), gym.today(), "Pasado"), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gym.request(u.id,
                new Forms.Freeze("HEALTH", gym.today(), gym.today().plusDays(1), "Sin documento"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recoveryTokenIsSingleUseAndHashed() {
        var u = customer();
        String link = identity.forgot(u.email);
        String raw = link.substring(link.indexOf("token=") + 6);
        var token = db.one(ResetToken.class, "select t from ResetToken t where t.userId=?1", u.id).orElseThrow();
        assertThat(token.tokenHash).isNotEqualTo(raw);
        assertThat(token.expiresAt).isAfter(Instant.now());
        identity.reset(raw, "ChangedPassword2026!", "ChangedPassword2026!");
        assertThatThrownBy(() -> identity.reset(raw, "ChangedAgain2026!", "ChangedAgain2026!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @WithMockUser(username = "admin@imperio.test", roles = "ADMIN")
    void catalogChangesKeepContractHistory() {
        var u = customer();
        var plan = new Forms.PlanEdit("Plan test", 2, new BigDecimal("114.99"), true);
        var p = gym.savePlan(null, plan, List.of(1L, 2L), admin());
        var payment = gym.pay(u.id, 1L, gym.draft(p.id), "CARD_TEST", null, null);
        gym.savePlan(p.id, new Forms.PlanEdit("Editado", 3, new BigDecimal("200.00"), false), List.of(3L), admin());
        var historical = db.get(Payment.class, payment.id);
        assertThat(historical.amount).isEqualByComparingTo("114.99");
        assertThat(historical.months).isEqualTo(2);
        assertThat(historical.planName).isEqualTo("Plan test");
        assertThat(db.count("select count(b) from ContractBenefit b where b.paymentId=?1", payment.id)).isEqualTo(2);
        assertThatThrownBy(() -> gym.draft(p.id)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concurrentIdenticalPaymentAppliesOnce() throws Exception {
        var u = customer();
        var draft = gym.draft(1L);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> gym.pay(u.id, 1L, draft, "CARD_TEST", null, null));
            var b = pool.submit(() -> gym.pay(u.id, 1L, draft, "CARD_TEST", null, null));
            assertThat(a.get(15, TimeUnit.SECONDS).id).isEqualTo(b.get(15, TimeUnit.SECONDS).id);
        }
        assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(gym.today().plusMonths(1));
    }

    @Test
    void dueRemindersAreSimulatedOnceAndExpiredTokensFail() {
        var u = customer();
        gym.pay(u.id, 1L, gym.draft(1L), "CARD_TEST", null, null);
        db.execute("update Membership m set m.expiresOn=?1 where m.userId=?2", gym.today().plusDays(7), u.id);
        gym.preferences(u.id, new Forms.Preferences("+51999000000", true, true, true));
        var r = db.one(Reminder.class, "select r from Reminder r where r.userId=?1 and r.daysBefore=7", u.id)
                .orElseThrow();
        reminders.deliver(r.id);
        assertThat(db.get(Reminder.class, r.id).status).isEqualTo("SIMULATED");
        var processed = db.get(Reminder.class, r.id).processedAt;
        reminders.deliver(r.id);
        assertThat(db.get(Reminder.class, r.id).processedAt).isEqualTo(processed);
        String link = identity.forgot(u.email);
        String raw = link.substring(link.indexOf("token=") + 6);
        db.execute("update ResetToken t set t.expiresAt=?1 where t.userId=?2", Instant.now().minusSeconds(60), u.id);
        assertThatThrownBy(() -> identity.reset(raw, "ExpiredToken2026!", "ExpiredToken2026!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @WithMockUser(username = "admin@imperio.test", roles = "ADMIN")
    void configurableFreezeExtendsExactlyOnce() throws Exception {
        Object target = org.springframework.test.util.AopTestUtils.getTargetObject(gym);
        org.springframework.test.util.ReflectionTestUtils.setField(target, "freezePolicy", "EXTEND_DAYS");
        try {
            var u = customer();
            gym.pay(u.id, 1L, gym.draft(1L), "CARD_TEST", null, null);
            var original = gym.membership(u.id).orElseThrow().expiresOn;
            var r = gym.request(u.id,
                    new Forms.Freeze("TRAVEL", gym.today().plusDays(1), gym.today().plusDays(3), "Prueba de política"),
                    null);
            gym.reviewRequest(r.id, admin(), "IN_REVIEW", "Revisando");
            gym.reviewRequest(r.id, admin(), "APPROVED", "Aprobado");
            assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(original.plusDays(3));
            assertThatThrownBy(() -> gym.reviewRequest(r.id, admin(), "APPROVED", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(target, "freezePolicy", "NO_EXTENSION");
        }
    }

    @Test
    @WithMockUser(username = "admin@imperio.test", roles = "ADMIN")
    void concurrentDifferentManualApprovalsDoNotLoseMonths() throws Exception {
        var u = customer();
        gym.pay(u.id, 1L, gym.draft(1L), "CARD_TEST", null, null);
        var original = gym.membership(u.id).orElseThrow().expiresOn;
        var a = gym.pay(u.id, 1L, gym.draft(1L), "WALLET", "CON-" + UUID.randomUUID(), proof());
        var b = gym.pay(u.id, 1L, gym.draft(2L), "WALLET", "CON-" + UUID.randomUUID(), proof());
        var context = org.springframework.security.core.context.SecurityContextHolder.getContext();
        var actor = admin();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(
                    new org.springframework.security.concurrent.DelegatingSecurityContextCallable<Boolean>(() -> {
                        gym.reviewPayment(a.id, actor, true, "A");
                        return true;
                    }, context));
            var second = pool.submit(
                    new org.springframework.security.concurrent.DelegatingSecurityContextCallable<Boolean>(() -> {
                        gym.reviewPayment(b.id, actor, true, "B");
                        return true;
                    }, context));
            assertThat(first.get(20, TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(20, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(gym.membership(u.id).orElseThrow().expiresOn).isIn(original.plusMonths(1).plusMonths(2),
                original.plusMonths(2).plusMonths(1));
    }

    @Test
    void duplicateDniIsRejectedWithoutCreatingAnotherCustomer() throws Exception {
        var existing = customer();
        String otherEmail = "duplicate-" + existing.dni + "@example.test";
        mvc.perform(post("/register").with(csrf()).param("name", "Otro nombre").param("dni", existing.dni)
                .param("email", otherEmail).param("phone", "+51999000000").param("password", "PasswordTest2026!")
                .param("confirmation", "PasswordTest2026!"))
                .andExpect(status().isOk()).andExpect(view().name("register"))
                .andExpect(model().attributeHasErrors("form"))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("El correo o DNI ya está registrado.")));
        assertThat(db.count("select count(u) from UserAccount u where u.dni=?1", existing.dni)).isEqualTo(1);
        assertThat(users.existsByEmailIgnoreCaseOrDni(otherEmail, "unused")).isFalse();
    }

    @Test
    void duplicateEmailIsRejectedRegardlessOfCase() throws Exception {
        var existing = customer();
        String otherDni = Integer.toString(sequence.incrementAndGet());
        mvc.perform(post("/register").with(csrf()).param("name", existing.name).param("dni", otherDni)
                .param("email", existing.email.toUpperCase(Locale.ROOT)).param("phone", "+51999000000")
                .param("password", "PasswordTest2026!").param("confirmation", "PasswordTest2026!"))
                .andExpect(status().isOk()).andExpect(view().name("register"))
                .andExpect(model().attributeHasErrors("form"));
        assertThat(db.count("select count(u) from UserAccount u where lower(u.email)=?1", existing.email)).isEqualTo(1);
        assertThat(users.existsByEmailIgnoreCaseOrDni("unused", otherDni)).isFalse();
    }

    @Test
    void simultaneousRegistrationsCannotDuplicateDniOrEmail() throws Exception {
        for (boolean sameDni : List.of(true, false)) {
            String firstDni = Integer.toString(sequence.incrementAndGet());
            String secondDni = sameDni ? firstDni : Integer.toString(sequence.incrementAndGet());
            String firstEmail = "race-" + firstDni + "@example.test";
            String secondEmail = sameDni ? "other-" + firstDni + "@example.test" : firstEmail.toUpperCase(Locale.ROOT);
            var ready = new CyclicBarrier(2);
            try (var pool = Executors.newFixedThreadPool(2)) {
                var attempts = new ArrayList<Future<Integer>>();
                for (var identifiers : List.of(List.of(firstDni, firstEmail), List.of(secondDni, secondEmail))) {
                    attempts.add(pool.submit(() -> {
                        ready.await(10, TimeUnit.SECONDS);
                        return mvc
                                .perform(post("/register").with(csrf()).param("name", "Cliente simultaneo")
                                        .param("dni", identifiers.get(0)).param("email", identifiers.get(1))
                                        .param("phone", "+51999000000").param("password", "PasswordTest2026!")
                                        .param("confirmation", "PasswordTest2026!"))
                                .andReturn().getResponse().getStatus();
                    }));
                }
                var statuses = List.of(attempts.get(0).get(30, TimeUnit.SECONDS),
                        attempts.get(1).get(30, TimeUnit.SECONDS));
                assertThat(statuses.stream().filter(code -> code == 302).count()).isEqualTo(1);
                assertThat(statuses.stream().filter(code -> code != 302).toList()).containsExactly(200);
            }
            String condition = sameDni ? "u.dni=?1" : "lower(u.email)=?1";
            String identifier = sameDni ? firstDni : firstEmail;
            var winner = db.one(UserAccount.class, "select u from UserAccount u where " + condition, identifier)
                    .orElseThrow();
            assertThat(db.count("select count(u) from UserAccount u where " + condition, identifier)).isEqualTo(1);
            assertThat(db.count("select count(p) from Preference p where p.userId=?1", winner.id)).isEqualTo(1);
            assertThat(db.count("select count(r) from UserRoleEntry r where r.userId=?1", winner.id)).isEqualTo(1);
        }
    }

    @Test
    void paymentRequiresAnAvailableBranch() {
        var u = customer();
        var draft = gym.draft(1L);
        assertThatThrownBy(() -> gym.pay(u.id, null, draft, "CARD_TEST", null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sede");
        assertThatThrownBy(() -> gym.pay(u.id, 999999L, draft, "CARD_TEST", null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("sede");
        var closed = new Branch();
        closed.name = "Sede cerrada " + UUID.randomUUID();
        closed.active = false;
        db.save(closed);
        assertThatThrownBy(() -> gym.pay(u.id, closed.id, draft, "CARD_TEST", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(db.count("select count(p) from Payment p where p.userId=?1", u.id)).isZero();
        assertThat(gym.membership(u.id)).isEmpty();
    }

    @Test
    void checkoutRetainsBranchOnValidationAndStoresItWithPayment() throws Exception {
        var u = customer();
        var session = new MockHttpSession();
        var draft = gym.draft(1L);
        session.setAttribute("draft", draft);
        session.setAttribute("selectedPlan", 1L);
        mvc.perform(get("/checkout").session(session).with(user(u.email).roles("CLIENTE"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sede donde entrenarás")));
        mvc.perform(post("/checkout").session(session).with(user(u.email).roles("CLIENTE")).with(csrf())
                .param("key", draft.key()).param("method", "CARD_TEST").param("accepted", "true"))
                .andExpect(status().isOk()).andExpect(view().name("checkout"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Selecciona la sede")));
        mvc.perform(post("/checkout").session(session).with(user(u.email).roles("CLIENTE")).with(csrf())
                .param("key", draft.key()).param("branchId", "2").param("method", "CARD_TEST"))
                .andExpect(status().isOk()).andExpect(model().attribute("branchId", 2L))
                .andExpect(model().attribute("selectedBranchName", "Pueblo Libre"));
        mvc.perform(post("/checkout").session(session).with(user(u.email).roles("CLIENTE")).with(csrf())
                .param("key", draft.key()).param("branchId", "999999").param("method", "CARD_TEST")
                .param("accepted", "true"))
                .andExpect(status().isOk()).andExpect(view().name("checkout"));
        mvc.perform(multipart("/checkout").file(proof()).session(session).with(user(u.email).roles("CLIENTE"))
                .with(csrf()).param("key", draft.key()).param("branchId", "2").param("method", "WALLET")
                .param("operation", "SEDE-" + UUID.randomUUID()).param("accepted", "true"))
                .andExpect(status().is3xxRedirection());
        var payment = db.one(Payment.class, "select p from Payment p where p.userId=?1", u.id).orElseThrow();
        assertThat(payment.branchId).isEqualTo(2L);
        assertThat(payment.branchName).isEqualTo("Pueblo Libre");
        assertThat(gym.membership(u.id)).isEmpty();
        mvc.perform(get("/account/payments/" + payment.id).with(user(u.email).roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Pueblo Libre")));
        mvc.perform(get("/admin/payments").param("branchId", "2").with(user("admin@imperio.test").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(model().attribute("branchId", 2L));
    }

    @Test
    @WithMockUser(username = "admin@imperio.test", roles = "ADMIN")
    void branchCountsFollowConfirmedMembershipsWithoutCountingRenewalsTwice() {
        long firstBefore = branchSummary(1L).activeMembers(), secondBefore = branchSummary(2L).activeMembers();
        var u = customer();
        var first = gym.pay(u.id, 1L, gym.draft(1L), "CARD_TEST", null, null);
        assertThat(branchSummary(1L).activeMembers()).isEqualTo(firstBefore + 1);
        var draft = gym.draft(2L);
        var renewal = gym.pay(u.id, 2L, draft, "WALLET", "SEDE-" + UUID.randomUUID(), proof());
        assertThat(branchSummary(1L).activeMembers()).isEqualTo(firstBefore + 1);
        assertThat(branchSummary(2L).activeMembers()).isEqualTo(secondBefore);
        var originalExpiry = gym.membership(u.id).orElseThrow().expiresOn;
        gym.reviewPayment(renewal.id, admin(), true, "Confirmado para Pueblo Libre");
        assertThat(gym.membership(u.id).orElseThrow().branchId).isEqualTo(2L);
        assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(originalExpiry.plusMonths(2));
        assertThat(branchSummary(1L).activeMembers()).isEqualTo(firstBefore);
        assertThat(branchSummary(2L).activeMembers()).isEqualTo(secondBefore + 1);
        assertThat(gym.pay(u.id, 3L, draft, "CARD_TEST", null, null).id).isEqualTo(renewal.id);
        assertThat(db.get(Payment.class, first.id).branchName).isEqualTo("San Miguel");
        assertThat(gym.membership(u.id).orElseThrow().branchId).isEqualTo(2L);
        gym.pay(u.id, 2L, gym.draft(1L), "CARD_TEST", null, null);
        assertThat(branchSummary(2L).activeMembers()).isEqualTo(secondBefore + 1);
        var rejected = gym.pay(u.id, 3L, gym.draft(1L), "TRANSFER", "REJ-" + UUID.randomUUID(), proof());
        gym.reviewPayment(rejected.id, admin(), false, "Comprobante no válido");
        assertThat(gym.membership(u.id).orElseThrow().branchId).isEqualTo(2L);
    }

    BranchService.Summary branchSummary(Long id) {
        return branches.summaries().stream().filter(s -> s.branch().id.equals(id)).findFirst().orElseThrow();
    }

    @Test
    void branchSummaryExcludesExpiredAndReportsUnassignedMembers() throws Exception {
        long before = branchSummary(3L).activeMembers(), expiredBefore = branchSummary(3L).expiredMembers(),
                unassignedBefore = branches.unassignedActiveMembers();
        var u = customer();
        gym.pay(u.id, 3L, gym.draft(1L), "CARD_TEST", null, null);
        db.execute("update Membership m set m.expiresOn=?1 where m.userId=?2", gym.today(), u.id);
        assertThat(branchSummary(3L).activeMembers()).isEqualTo(before);
        assertThat(branchSummary(3L).expiredMembers()).isEqualTo(expiredBefore + 1);
        var legacy = customer();
        gym.pay(legacy.id, 3L, gym.draft(1L), "CARD_TEST", null, null);
        db.execute("update Membership m set m.branchId=null where m.userId=?1", legacy.id);
        assertThat(branches.unassignedActiveMembers()).isEqualTo(unassignedBefore + 1);
        mvc.perform(get("/admin").with(user("admin@imperio.test").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Socios activos por sede")));
        mvc.perform(get("/admin/clients").param("branchId", "0").with(user("admin@imperio.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(legacy.email)));
        mvc.perform(get("/admin").with(user(u.email).roles("CLIENTE"))).andExpect(status().isForbidden());
    }

    @Test
    void branchMigrationPreservesExistingPaymentsAndMemberships() throws Exception {
        String url = "jdbc:h2:mem:branch_migration_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (var source = new com.zaxxer.hikari.HikariDataSource()) {
            source.setJdbcUrl(url);
            source.setUsername("sa");
            source.setPassword("");
            source.setMaximumPoolSize(4);
            org.flywaydb.core.Flyway.configure().dataSource(source).target("3").load().migrate();
            try (var connection = source.getConnection(); var statement = connection.createStatement()) {
                statement.executeUpdate(
                        "insert into users(id,name,dni,email,phone,password_hash,demo,created_at) values(1,'Anterior','87654321','legacy@example.test','+51999000000','test-only',false,current_timestamp)");
                statement.executeUpdate(
                        "insert into payments(id,user_id,plan_id,plan_name,months,amount,method,status,idempotency_key,created_at,applied_from) values(1,1,1,'Plan anterior',1,89.99,'CARD_TEST','CONFIRMED','legacy-key',current_timestamp,current_date)");
                statement.executeUpdate(
                        "insert into memberships(id,user_id,payment_id,started_on,expires_on) values(1,1,1,current_date,current_date)");
                org.flywaydb.core.Flyway.configure().dataSource(source).load().migrate();
                try (var result = statement.executeQuery(
                        "select p.branch_id,p.branch_name,m.branch_id from payments p join memberships m on m.payment_id=p.id")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getObject(1)).isNull();
                    assertThat(result.getObject(2)).isNull();
                    assertThat(result.getObject(3)).isNull();
                    assertThat(result.next()).isFalse();
                }
                try (var result = statement.executeQuery("select count(*) from branches")) {
                    result.next();
                    assertThat(result.getInt(1)).isEqualTo(3);
                }
            }
        }
    }

    @Test
    @WithMockUser(username = "admin@imperio.test", roles = "ADMIN")
    void branchAccessRequiresActiveMembershipEnforcesCapacityAndTracksExit() throws Exception {
        var first = customer();
        gym.pay(first.id, 1L, gym.draft(1L), "CARD_TEST", null, null);
        var second = customer();
        gym.pay(second.id, 1L, gym.draft(1L), "CARD_TEST", null, null);
        var branch = db.get(Branch.class, 1L);
        int originalCapacity = branch.capacity;
        db.execute("update Branch b set b.capacity=?1 where b.id=?2", branchSummary(1L).present() + 1, 1L);
        try {
            assertThat(branchSummary(1L).available()).isEqualTo(1);
            branches.checkIn(first.dni, 1L, admin());
            assertThat(branchSummary(1L).present()).isEqualTo(1);
            assertThat(branchSummary(1L).available()).isZero();
            assertThatThrownBy(() -> branches.checkIn(first.dni, 1L, admin()))
                    .hasMessageContaining("ya tiene una entrada");
            assertThatThrownBy(() -> branches.checkIn(second.dni, 1L, admin())).hasMessageContaining("está llena");
            var attendance = db.one(Attendance.class,
                    "select a from Attendance a where a.userId=?1 and a.checkedOutAt is null", first.id).orElseThrow();
            mvc.perform(get("/admin/access").param("branchId", "1").with(user("admin@imperio.test").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Entradas, salidas y aforo")));
            mvc.perform(post("/admin/access/" + attendance.id + "/checkout")
                    .with(user("admin@imperio.test").roles("ADMIN")).with(csrf()).param("branchId", "1"))
                    .andExpect(redirectedUrl("/admin/access?branchId=1"));
            assertThat(db.get(Attendance.class, attendance.id).checkedOutAt).isNotNull();
            assertThat(branchSummary(1L).present()).isZero();
            assertThat(db.count("select count(e) from AuditEvent e where e.targetType='attendance' and e.targetId=?1",
                    attendance.id)).isEqualTo(2);
            mvc.perform(get("/admin/access").with(user(first.email).roles("CLIENTE")))
                    .andExpect(status().isForbidden());
        } finally {
            db.execute("update Branch b set b.capacity=?1 where b.id=?2", originalCapacity, 1L);
        }
    }

    @Test
    @WithMockUser(username = "admin@imperio.test", roles = "ADMIN")
    void branchAccessRejectsExpiredMembershipAndWrongBranch() {
        var expired = customer();
        gym.pay(expired.id, 1L, gym.draft(1L), "CARD_TEST", null, null);
        db.execute("update Membership m set m.expiresOn=?1 where m.userId=?2", gym.today(), expired.id);
        assertThatThrownBy(() -> branches.checkIn(expired.dni, 1L, admin())).hasMessageContaining("no está vigente");
        var otherBranch = customer();
        gym.pay(otherBranch.id, 2L, gym.draft(1L), "CARD_TEST", null, null);
        assertThatThrownBy(() -> branches.checkIn(otherBranch.dni, 1L, admin()))
                .hasMessageContaining("no corresponde a esta sede");
        assertThat(branches.presentAt(1L)).isEmpty();
    }

    @Test
    void pagesRenderForBothRoles() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("name", "Datos incompletos")
                .param("email", "incompleto@example.test").param("confirmation", "NoPassword2026!"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "dni", "phone", "password"));
        var u = customer();
        for (String path : List.of("/", "/login", "/register", "/forgot", "/reset?token=invalid"))
            mvc.perform(get(path)).andExpect(status().isOk());
        for (String path : List.of("/account", "/account/renew", "/account/payments", "/account/requests",
                "/account/preferences"))
            mvc.perform(get(path).with(user(u.email).roles("CLIENTE"))).andExpect(status().isOk());
        for (String path : List.of("/admin", "/admin/access", "/admin/clients", "/admin/payments", "/admin/requests",
                "/admin/plans", "/admin/reminders", "/admin/audit", "/admin/plan", "/admin/plans/1",
                "/admin/clients/" + u.id))
            mvc.perform(get(path).with(user("admin@imperio.test").roles("ADMIN"))).andExpect(status().isOk());
    }
}
