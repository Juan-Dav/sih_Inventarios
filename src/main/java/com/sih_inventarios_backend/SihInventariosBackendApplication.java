package com.sih_inventarios_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class SihInventariosBackendApplication {

	public static void main(String[] args) {
		// Los instantes se guardan en UTC, sin importar la zona del servidor
		// (Render suele estar en UTC y un equipo local en hora de Colombia).
		// Así todos los registros son comparables y el navegador los convierte
		// a la hora local del usuario al mostrarlos.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		SpringApplication.run(SihInventariosBackendApplication.class, args);
	}

}
