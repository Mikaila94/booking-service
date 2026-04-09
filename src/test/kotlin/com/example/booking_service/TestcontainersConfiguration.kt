package com.example.booking_service

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
//import org.testcontainers.mssqlserver.MSSQLServerContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	fun postgresContainer(): PostgreSQLContainer {
		return PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
	}

//	@Bean
//	@ServiceConnection
//	fun sqlServerContainer(): MSSQLServerContainer {
//		return MSSQLServerContainer(DockerImageName.parse("mcr.microsoft.com/mssql/server:latest"))
//	}

}
