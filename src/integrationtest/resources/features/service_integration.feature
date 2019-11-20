Feature: Test plugin with surrounding services

  Scenario: Send eiffel event on submit
    Given a project is created
    And the project is configured to send eiffel events with publish url "http://publish:8080"
    When a change is created
    Then I should find an eiffel event in rabbitmq
