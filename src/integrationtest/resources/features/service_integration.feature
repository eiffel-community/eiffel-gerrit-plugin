Feature: Test plugin with surrounding services

  Scenario Outline: Send eiffel event on submit with <submitType>
    Given a project is created
    And the project is configured to send eiffel events and submit type is "<submitType>" and publish url "http://publish:8080"

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

    Examples:
    | submitType          |
    | FAST_FORWARD_ONLY   |
    | REBASE_ALWAYS       |
    | MERGE_ALWAYS        |
    #| CHERRY_PICK         | # WE currently don't support cherry pick
    | MERGE_IF_NECESSARY  |
    | REBASE_IF_NECESSARY |
