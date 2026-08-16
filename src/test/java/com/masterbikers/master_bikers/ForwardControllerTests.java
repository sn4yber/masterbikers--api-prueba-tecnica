package com.masterbikers.master_bikers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ForwardControllerTests {

	private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ForwardController()).build();

	@Test
	void forwardsAngularRoutesToIndex() throws Exception {
		mockMvc.perform(get("/products"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/index.html"));
		mockMvc.perform(get("/products/123"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/index.html"));
	}

	@Test
	void doesNotForwardBackendOrStaticRoutes() throws Exception {
		mockMvc.perform(get("/api/v1/products"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/styles.css"))
				.andExpect(status().isNotFound());
	}
}
