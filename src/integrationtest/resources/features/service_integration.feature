Feature: Test plugin with surrounding services

  Scenario: Send eiffel event on submit
    Given a project is created
    And the project is configured to send eiffel events with publish url "publish:8080"
    And a change is submitted
    Then I should find an eiffel event in rabbitmq
