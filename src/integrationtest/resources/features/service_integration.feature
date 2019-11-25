Feature: Test plugin with surrounding services

  Scenario: Send eiffel event on submit
    Given a project is created
    And the project is configured to send eiffel events with publish url "http://publish:8080"

    When a change is created
    Then I should find an eiffel event in rabbitmq
    And "0" links are found in the event

    When a change is submitted
    Then I should find an eiffel event in rabbitmq
    And "1" links are found in the event

    When a change is created
    Then I should find an eiffel event in rabbitmq
    And "1" links are found in the event

    When a change is submitted
    Then I should find an eiffel event in rabbitmq
    And "2" links are found in the event
