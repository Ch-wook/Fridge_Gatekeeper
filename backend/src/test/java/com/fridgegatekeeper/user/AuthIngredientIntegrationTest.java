package com.fridgegatekeeper.user;

import com.fridgegatekeeper.ingredient.IngredientRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 실제 보안 필터·세션·CSRF·JPA를 함께 통과하여 사용자 데이터 격리를 검증합니다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthIngredientIntegrationTest.FixedClock.class)
class AuthIngredientIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired IngredientRepository ingredients;

    @TestConfiguration
    static class FixedClock {
        @Bean @Primary Clock testClock() {
            // UTC 8일 15시이지만 서울에서는 9일 0시입니다.
            return Clock.fixed(Instant.parse("2026-09-08T15:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    @BeforeEach void clearUsers() {
        ingredients.deleteAll();
        users.deleteAll();
    }

    @Test void registrationLoginAndLogoutPreserveSecurityBoundaries() throws Exception {
        Browser browser = anonymous();
        register(browser, "COOK@example.com", "fridge1234!", "요리사");
        assertThat(users.findByEmail("cook@example.com")).isPresent();
        assertThat(users.findByEmail("cook@example.com").orElseThrow().getPasswordHash()).startsWith("$2");
        mvc.perform(get("/api/auth/me").session(browser.session())).andExpect(status().isUnauthorized());
        String previousId = browser.session().getId();
        String previousToken = browser.token();
        mvc.perform(secure(post("/api/auth/login"), browser)
            .content("{\"email\":\"cook@example.com\",\"password\":\"fridge1234!\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("cook@example.com"))
            .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(browser.session().getId()).isNotEqualTo(previousId);
        mvc.perform(secure(post("/api/ingredients"), browser).content(ingredient("계란", "2026-09-09", null)))
            .andExpect(status().isForbidden());
        browser = refresh(browser.session());
        assertThat(browser.token()).isNotEqualTo(previousToken);
        mvc.perform(get("/api/auth/me").session(browser.session())).andExpect(status().isOk());
        mvc.perform(secure(post("/api/auth/logout"), browser)).andExpect(status().isNoContent())
            .andExpect(cookie().maxAge("JSESSIONID", 0));
        assertThat(browser.session().isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test void writesRequireCsrfAndIngredientEndpointsRequireLogin() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"cook@example.com\",\"password\":\"fridge1234!\",\"nickname\":\"요리사\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/ingredients")).andExpect(status().isUnauthorized());
        Browser browser = anonymous();
        mvc.perform(secure(post("/api/ingredients"), browser).content(ingredient("계란", "2026-09-09", null)))
            .andExpect(status().isUnauthorized());
    }

    @Test void duplicateEmailAndInvalidPasswordsHaveSafeErrors() throws Exception {
        Browser browser = anonymous();
        register(browser, "cook@example.com", "fridge1234!", "요리사");
        mvc.perform(secure(post("/api/auth/register"), browser)
            .content("{\"email\":\"COOK@example.com\",\"password\":\"fridge1234!\",\"nickname\":\"두번째\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
        mvc.perform(secure(post("/api/auth/register"), browser)
            .content("{\"email\":\"unicode@example.com\",\"password\":\"" + "가".repeat(25) + "\",\"nickname\":\"길이검사\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.password").exists());
        for (String email : new String[] {"cook@example.com", "missing@example.com"}) {
            mvc.perform(secure(post("/api/auth/login"), browser)
                .content("{\"email\":\"" + email + "\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
    }

    @Test void everyIngredientOperationChecksOwnership() throws Exception {
        Browser owner = login("owner@example.com");
        Browser stranger = login("stranger@example.com");
        long id = create(owner, "계란", "2026-09-12");
        mvc.perform(get("/api/ingredients").session(stranger.session())).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/ingredients/" + id).session(stranger.session())).andExpect(status().isNotFound());
        mvc.perform(secure(put("/api/ingredients/" + id), stranger)
            .content(ingredient("훔친재료", "2026-09-12", 0L))).andExpect(status().isNotFound());
        mvc.perform(secure(delete("/api/ingredients/" + id), stranger)).andExpect(status().isNotFound());
        mvc.perform(get("/api/ingredients/" + id).session(owner.session())).andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("계란"));
        mvc.perform(secure(delete("/api/ingredients/" + id), owner)).andExpect(status().isNoContent());
        mvc.perform(get("/api/ingredients/" + id).session(owner.session())).andExpect(status().isNotFound());
    }

    @Test void staleUpdatesReturnConflictAndRequireVersion() throws Exception {
        Browser owner = login("owner@example.com");
        long id = create(owner, "계란", "2026-09-12");
        mvc.perform(secure(put("/api/ingredients/" + id), owner)
            .content(ingredient("달걀", "2026-09-12", 0L))).andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(1));
        mvc.perform(secure(put("/api/ingredients/" + id), owner)
            .content(ingredient("오래된수정", "2026-09-12", 0L))).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("STALE_VERSION"));
        mvc.perform(secure(put("/api/ingredients/" + id), owner)
            .content(ingredient("버전없음", "2026-09-12", null))).andExpect(status().isBadRequest());
    }

    @Test void invalidDatesNumbersAndEnumsReturnValidationErrors() throws Exception {
        Browser owner = login("owner@example.com");
        String valid = ingredient("계란", "2026-09-12", null);
        for (String invalid : new String[] {
            valid.replace("2026-09-01", "2026-09-10"),
            valid.replace("2026-09-12", "2026-08-31"),
            valid.replace("\"quantity\":6", "\"quantity\":0"),
            valid.replace("\"quantity\":6", "\"quantity\":1.0001"),
            valid.replace("DAIRY", "INVALID"), valid.replace("2026-09-12", "2026-02-30")
        }) {
            mvc.perform(secure(post("/api/ingredients"), owner).content(invalid))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors").exists());
        }
        mvc.perform(get("/api/ingredients?sort=invalid").session(owner.session())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/ingredients/not-a-number").session(owner.session())).andExpect(status().isBadRequest());
    }

    @Test void dashboardUsesKoreanDateAndInclusiveThreeDayWindow() throws Exception {
        Browser owner = login("owner@example.com");
        create(owner, "만료", "2026-09-08");
        create(owner, "오늘", "2026-09-09");
        create(owner, "삼일후", "2026-09-12");
        create(owner, "사일후", "2026-09-13");
        Browser stranger = login("stranger@example.com");
        create(stranger, "다른사용자", "2026-09-09");
        mvc.perform(get("/api/dashboard").session(owner.session())).andExpect(status().isOk())
            .andExpect(jsonPath("$.today").value("2026-09-09"))
            .andExpect(jsonPath("$.total").value(4)).andExpect(jsonPath("$.safeCount").value(1))
            .andExpect(jsonPath("$.soonCount").value(2)).andExpect(jsonPath("$.expiredCount").value(1))
            .andExpect(jsonPath("$.todayCount").value(1))
            .andExpect(jsonPath("$.expiringIngredients[0].name").value("오늘"))
            .andExpect(jsonPath("$.expiringIngredients[0].daysUntilExpiration").value(0))
            .andExpect(jsonPath("$.expiringIngredients[1].daysUntilExpiration").value(3))
            .andExpect(jsonPath("$.expiredIngredients[0].daysUntilExpiration").value(-1));
        mvc.perform(get("/api/ingredients").session(owner.session())).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("EXPIRED"))
            .andExpect(jsonPath("$[3].status").value("SAFE"));
    }

    @Test void emptyDashboardHasZeroCountsAndEmptyArrays() throws Exception {
        Browser owner = login("owner@example.com");
        mvc.perform(get("/api/dashboard").session(owner.session())).andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.todayCount").value(0))
            .andExpect(jsonPath("$.expiringIngredients.length()").value(0))
            .andExpect(jsonPath("$.expiredIngredients.length()").value(0));
    }

    private record Browser(MockHttpSession session, String token, String headerName) { }

    private Browser anonymous() throws Exception { return refresh(new MockHttpSession()); }
    private Browser refresh(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        return new Browser((MockHttpSession) result.getRequest().getSession(),
            JsonPath.read(body, "$.token"), JsonPath.read(body, "$.headerName"));
    }

    private MockHttpServletRequestBuilder secure(MockHttpServletRequestBuilder request, Browser browser) {
        return request.session(browser.session()).header(browser.headerName(), browser.token())
            .contentType(MediaType.APPLICATION_JSON);
    }

    private void register(Browser browser, String email, String password, String nickname) throws Exception {
        mvc.perform(secure(post("/api/auth/register"), browser).content(
            "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"nickname\":\"" + nickname + "\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private Browser login(String email) throws Exception {
        Browser browser = anonymous();
        register(browser, email, "fridge1234!", "요리사");
        mvc.perform(secure(post("/api/auth/login"), browser)
            .content("{\"email\":\"" + email + "\",\"password\":\"fridge1234!\"}"))
            .andExpect(status().isOk());
        return refresh(browser.session());
    }

    private long create(Browser owner, String name, String expirationDate) throws Exception {
        MvcResult result = mvc.perform(secure(post("/api/ingredients"), owner)
            .content(ingredient(name, expirationDate, null))).andExpect(status().isCreated()).andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String ingredient(String name, String expirationDate, Long version) {
        return "{\"name\":\"" + name + "\",\"category\":\"DAIRY\",\"quantity\":6,\"unit\":\"PIECE\","
            + "\"purchaseDate\":\"2026-09-01\",\"expirationDate\":\"" + expirationDate
            + "\",\"storageType\":\"FRIDGE\"" + (version == null ? "" : ",\"version\":" + version) + "}";
    }
}
