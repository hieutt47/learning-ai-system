package vn.edu.learningai.controller;

import org.junit.jupiter.api.Test;
import vn.edu.learningai.support.IntegrationTestBase;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarketplaceControllerTest extends IntegrationTestBase {
    private static final String COURSES = "/api/v1/courses";
    @Test
    void guestListsOnlyPublishedUnarchivedCourses() throws Exception {
        mockMvc.perform(get(COURSES))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.page_size").value(20))
            .andExpect(jsonPath("$.items[*].id").value(org.hamcrest.Matchers.containsInAnyOrder("2", "4")));
    }
    @Test
    void listDoesNotLeakInternalOrPrivateFields() throws Exception {
        mockMvc.perform(get(COURSES))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Khoa draft"))))
            .andExpect(content().string(not(containsString("transcript"))))
            .andExpect(content().string(not(containsString("youtu.be"))))
            .andExpect(content().string(not(containsString("archived_at"))))
            .andExpect(content().string(not(containsString("storage_key"))))
            .andExpect(jsonPath("$.items[*].outline").value(everyItem(org.hamcrest.Matchers.nullValue())));
    }
    @Test
    void publicShapeMatchesContract() throws Exception {
        mockMvc.perform(get(COURSES + "/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("2"))
            .andExpect(jsonPath("$.title").value("Khoa publish cua A"))
            .andExpect(jsonPath("$.description").value("Mo ta"))
            .andExpect(jsonPath("$.category.id").value("1"))
            .andExpect(jsonPath("$.category.name").value("Lap trinh"))
            .andExpect(jsonPath("$.teacher.id").value("2"))
            .andExpect(jsonPath("$.teacher.full_name").value("Teacher A"))
            .andExpect(jsonPath("$.price").value("0.00"))
            .andExpect(jsonPath("$.currency").value("VND"))
            .andExpect(jsonPath("$.total_students").value(1));
    }
    @Test
    void detailHasOutlineWithoutVideoOrTranscript() throws Exception {
        mockMvc.perform(get(COURSES + "/2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outline.length()").value(2))
            .andExpect(jsonPath("$.outline[0].id").value("1"))
            .andExpect(jsonPath("$.outline[0].title").value("Bai 1: Gioi thieu"))
            .andExpect(jsonPath("$.outline[0].order_index").value(1))
            .andExpect(jsonPath("$.outline[1].order_index").value(2))
            .andExpect(content().string(not(containsString("secret-a1"))))
            .andExpect(content().string(not(containsString("transcript bi mat"))));
    }
    @Test
    void draftDetailIsNotFoundNotForbidden() throws Exception {
        mockMvc.perform(get(COURSES + "/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
    @Test
    void archivedDetailIsNotFoundEvenThoughPublished() throws Exception {
        mockMvc.perform(get(COURSES + "/5"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
    @Test
    void unknownDetailIsNotFound() throws Exception {
        mockMvc.perform(get(COURSES + "/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
    @Test
    void ownerTeachingRouteStillReachesDraftAndArchivedCourses() throws Exception {
        mockMvc.perform(get("/api/v1/teaching/courses/1").header("Authorization", "Bearer " + tokenFor(2)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/teaching/courses/5").header("Authorization", "Bearer " + tokenFor(3)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.archived_at").isNotEmpty());
    }
    @Test
    void filtersByCategory() throws Exception {
        mockMvc.perform(get(COURSES).param("category_id", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].id").value("4"));
    }
    @Test
    void unknownCategoryFilterIsNotFound() throws Exception {
        mockMvc.perform(get(COURSES).param("category_id", "999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
    @Test
    void searchesTitleCaseInsensitivelyAndIgnoresDrafts() throws Exception {
        mockMvc.perform(get(COURSES).param("q", "PUBLISH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.items[*].id").value(org.hamcrest.Matchers.containsInAnyOrder("2", "4")));
        mockMvc.perform(get(COURSES).param("q", "draft"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void noResultsIsAnEmptyPageNotAnError() throws Exception {
        mockMvc.perform(get(COURSES).param("q", "khong co khoa nao nhu vay"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.items.length()").value(0));
    }
    @Test
    void likeWildcardsInTheQueryAreLiteralCharacters() throws Exception {
        mockMvc.perform(get(COURSES).param("q", "%"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(get(COURSES).param("q", "_"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0));
    }
    @Test
    void queryIsCombinedWithCategory() throws Exception {
        mockMvc.perform(get(COURSES).param("q", "publish").param("category_id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].id").value("2"));
    }
    @Test
    void pageBeyondTheEndIsEmptyButKeepsTotal() throws Exception {
        mockMvc.perform(get(COURSES).param("page", "5").param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.page").value(5));
    }
    @Test
    void paginatesWithoutRepeatingRows() throws Exception {
        mockMvc.perform(get(COURSES).param("page", "1").param("page_size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.items.length()").value(1));
        mockMvc.perform(get(COURSES).param("page", "2").param("page_size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].id").value(not(org.hamcrest.Matchers.is("4"))));
    }

    @Test
    void sortsByPriceAscending() throws Exception {
        mockMvc.perform(get(COURSES).param("sort", "price,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value("2"))
            .andExpect(jsonPath("$.items[1].id").value("4"));
    }
    @Test
    void sortsByPriceDescending() throws Exception {
        mockMvc.perform(get(COURSES).param("sort", "price,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value("4"))
            .andExpect(jsonPath("$.items[1].id").value("2"));
    }
    @Test
    void unknownSortFieldFallsBackInsteadOfFailing() throws Exception {
        mockMvc.perform(get(COURSES).param("sort", "password_hash,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2));
    }
    @Test
    void invalidPaginationIsValidationError() throws Exception {
        mockMvc.perform(get(COURSES).param("page", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get(COURSES).param("page_size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get(COURSES).param("page", "abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.request_id").exists());
    }

    @Test
    void totalStudentsCountsEnrollmentsOnly() throws Exception {
        mockMvc.perform(get(COURSES + "/4"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total_students").value(1))
            .andExpect(jsonPath("$.outline.length()").value(1));
    }
}
