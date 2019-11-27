# SCC = EiffelSourceChangeCreatedEvent
# SCS = EiffelSourceChangeSubmittedEvent


@Linking
Feature: Test Event Sending Flow

  Scenario: First time use of plugin
    Given no SCS event was sent, branch: "refs/for/master"

    When user "1" creates a new change, branch: "refs/for/master"
    Then a "SCC" event with id "SCC1" is sent
    And no BASE link set

    When user "1" submits the change, branch: "refs/for/master"
    Then a "SCS" event with id "SCS1" is sent
    And CHANGE links to event "SCC1"

  Scenario: Normal scenario with one change
    Given a SCS event with id "SCS1" was sent, branch: "refs/for/master"

    When user "1" creates a new change, branch: "refs/for/master"
    Then a "SCC" event with id "SCC1" is sent
    And BASE links to event "SCS1"

    When user "1" submits the change, branch: "refs/for/master"
    Then a "SCS" event with id "SCS2" is sent
    And CHANGE links to event "SCC1"
    And PREVIOUS_VERSION links to event "SCS1"

  Scenario: Normal scenario with two changes
    Given a SCS event with id "SCS1" was sent, branch: "refs/for/master"

    When user "1" creates a new change, branch: "refs/for/master"
    Then a "SCC" event with id "SCC1" is sent
    And BASE links to event "SCS1"

    When user "1" uploads a new patchset, branch: "refs/for/master"
    Then a "SCC" event with id "SCC2" is sent
    And BASE links to event "SCS1"
    And PREVIOUS_VERSION links to event "SCC1"

    When user "1" submits the change, branch: "refs/for/master"
    Then a "SCS" event with id "SCS2" is sent
    And CHANGE links to event "SCC2"
    And PREVIOUS_VERSION links to event "SCS1"

  Scenario: Multiple patch sets created simultaneously
    Given a SCS event with id "SCS1" was sent, branch: "refs/for/master"

    When user "1" creates a new change, branch: "refs/for/master"
    Then a "SCC" event with id "SCC1" is sent
    And BASE links to event "SCS1"

    When user "2" creates a new change, branch: "refs/for/master"
    Then a "SCC" event with id "SCC2" is sent
    And BASE links to event "SCS1"

    When user "3" creates a new change, branch: "refs/for/master"
    Then a "SCC" event with id "SCC3" is sent
    And BASE links to event "SCS1"

    When user "2" submits the change, branch: "refs/for/master"
    Then a "SCS" event with id "SCS2" is sent
    And CHANGE links to event "SCC2"
    And PREVIOUS_VERSION links to event "SCS1"

    When user "1" uploads a new patchset, branch: "refs/for/master"
    Then a "SCC" event with id "SCC4" is sent
    And BASE links to event "SCS1"
    And PREVIOUS_VERSION links to event "SCC1"

    When user "3" rebases the change, branch: "refs/for/master"
    Then a "SCC" event with id "SCC5" is sent
    And BASE links to event "SCS2"
    And PREVIOUS_VERSION links to event "SCC3"

    When user "3" submits the change, branch: "refs/for/master"
    Then a "SCS" event with id "SCS3" is sent
    And CHANGE links to event "SCC5"
    And PREVIOUS_VERSION links to event "SCS2"

    When user "1" rebases the change, branch: "refs/for/master"
    Then a "SCC" event with id "SCC6" is sent
    And BASE links to event "SCS3"
    And PREVIOUS_VERSION links to event "SCC4"

    When user "1" submits the change, branch: "refs/for/master"
    Then a "SCS" event with id "SCS4" is sent
    And CHANGE links to event "SCC6"
    And PREVIOUS_VERSION links to event "SCS3"

  Scenario: Normal scenario submitting refs/for/other-branch than refs/for/master
    Given a SCS event with id "SCS1" was sent, branch: "refs/for/master"
    And a SCS event with id "SCS2" was sent, branch: "refs/for/other-branch"

    When user "1" creates a new change, branch: "refs/for/other-branch"
    Then a "SCC" event with id "SCC1" is sent
    And BASE links to event "SCS2"

    When user "1" creates a new change, branch: "refs/for/master"
    Then a "SCC" event with id "SCC2" is sent
    And BASE links to event "SCS1"

    When user "1" submits the change, branch: "refs/for/other-branch"
    Then a "SCS" event with id "SCS3" is sent
    And CHANGE links to event "SCC1"
    And PREVIOUS_VERSION links to event "SCS2"

    When user "1" submits the change, branch: "refs/for/master"
    Then a "SCS" event with id "SCS4" is sent
    And CHANGE links to event "SCC2"
    And PREVIOUS_VERSION links to event "SCS1"
