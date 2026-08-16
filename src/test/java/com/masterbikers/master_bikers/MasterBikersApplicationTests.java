package com.masterbikers.master_bikers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class MasterBikersApplicationTests {

	@Test
	void applicationIsConfiguredForSpringBoot() {
		assertTrue(MasterBikersApplication.class.isAnnotationPresent(SpringBootApplication.class));
	}
}
