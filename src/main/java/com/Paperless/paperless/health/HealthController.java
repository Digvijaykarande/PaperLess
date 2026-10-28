package com.Paperless.paperless.health;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
	
	@RequestMapping("/")
	public String home() {
		return "server is running ";
	}
}
