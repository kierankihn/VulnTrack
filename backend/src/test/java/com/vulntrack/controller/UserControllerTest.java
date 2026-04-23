package com.vulntrack.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    @Test
    void createRejectsMissingRequiredProfileField() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(null))
            .setValidator(validator())
            .build();

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "alice",
                      "email": "alice@example.com"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void setActiveRejectsMissingActiveFlag() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(null))
            .setValidator(validator())
            .build();

        mockMvc.perform(patch("/api/v1/users/1/active")
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
