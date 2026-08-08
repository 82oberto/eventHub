package com.eventhub.catalog_service.controller;

import com.eventhub.catalog_service.AbstractIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VenueControllerIT extends AbstractIntegrationTest {

    /** V3 extracts 6 distinct venues from the seeded events. */
    private static final int SEEDED_VENUES = 6;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /venues returns the seeded venues sorted by city and name")
    void getVenuesReturnsSeededVenues() throws Exception {
        mockMvc.perform(get("/venues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(SEEDED_VENUES))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].city").value("Salzburg"))
                .andExpect(jsonPath("$[0].name").value("Red Bull Arena"))
                .andExpect(jsonPath("$[*].city").value(Matchers.hasItem("Vienna")));
    }
}
