Feature: Sending an Eiffel SCE message on change merge in Gerrit.
    As a user I want the plugin to send an Eiffel SCE message every time I
    submit an approved change in Gerrit, and if previous SCM events exist they
    should be linked to.

    @test_scm_event_linking
    Scenario: Test SCM events contain proper links to previous events
        Given I create a project named "main_repo"
        And I create a branch named "not_master" in project "main_repo"
        And I create a project named "second_repo"

        When I submit an approved change on branch "master" in project "main_repo"
        And I submit an approved change on branch "not_master" in project "main_repo"
        And I submit an approved change on branch "master" in project "second_repo"
        And I submit an approved change on branch "master" in project "main_repo"

        Then the RabbitMQ server contains "4" Eiffel SCE messages
        And message "4" links to message "1"

    Scenario: Test SCM events contain proper links to previous events when using parent/child project structure
        Given I create a project named "parent"
        And I create a child project of "parent" named "parent/child"
        And I create a child project of "parent" named "parent/child2"
        And I create a branch named "not_master" in project "parent/child"
        And I create a project named "not_important"

        When I submit an approved change on branch "master" in project "parent/child"
        And I submit an approved change on branch "not_master" in project "parent/child"
        And I submit an approved change on branch "master" in project "not_important"
        And I submit an approved change on branch "master" in project "parent/child"
        And I submit an approved change on branch "master" in project "parent/child2"

        Then the RabbitMQ server contains "5" Eiffel SCE messages 
        And message "4" links to message "1"
        #And message "2" has no links to previous event



