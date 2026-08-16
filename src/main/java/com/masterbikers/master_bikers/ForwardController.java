package com.masterbikers.master_bikers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ForwardController {

	@GetMapping({
			"/{path:(?!api$|api-docs$|swagger-ui$|actuator$)[^\\.]*}",
			"/{path:(?!api$|api-docs$|swagger-ui$|actuator$)[^\\.]*}/**"
	})
	public String forward() {
		return "forward:/index.html";
	}
}
