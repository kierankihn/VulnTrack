package com.vulntrack.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TicketControllerTest {

    @Test
    void updateAssigneeRejectsMissingAssigneeId() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TicketController(null))
            .setValidator(validator())
            .build();

        mockMvc.perform(put("/api/v1/tickets/1/assignee")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    private LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return validator;
    }
}
