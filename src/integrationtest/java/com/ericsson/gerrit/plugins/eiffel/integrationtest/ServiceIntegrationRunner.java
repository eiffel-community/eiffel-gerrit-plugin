package com.ericsson.gerrit.plugins.eiffel.integrationtest;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = "pretty", features = "src/integrationtest/resources/features/service_integration.feature")
public class ServiceIntegrationRunner {

}
