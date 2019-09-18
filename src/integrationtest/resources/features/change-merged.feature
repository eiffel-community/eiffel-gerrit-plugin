Feature: Sending an Eiffel SCE message on change merge in Gerrit.
    As a user I want the plugin to send an Eiffel SCE message every time I submit an approved change in Gerrit.

    Background:
        Given Eiffel plugin is installed, loaded and enabled for project X in Gerrit
        And I configured RabbitMQ settings for project X

    @test_default_config
    Scenario: Default configuration.
        Given I create a project X
        And I left branch filter option empty
        And I did not enter any work item filters
        When I submit an approved change in project X
        Then plugin should send Eiffel messages

    @test_matching_branch
    Scenario: Matching branch.
        Given I create a project X
        And I set a branch filter that matches branch B
        When I submit an approved change in branch B of project X
        Then plugin should send an Eiffel SCE message containing the commit id, project name, branch name B and repoURL

    @test_non_matching_branch
    Scenario: Non matching branch.
        Given I create a project X
        And I set a branch filter that does NOT match branch B
        When I submit an approved change in branch B of project X
        Then plugin should NOT send any Eiffel message

    @test_mhweb_ref
    Scenario: Recognize Mhweb references by default.
        Given I create a project X
        And I did not enter any work item filters
        When I submit an approved change in project X with the commit message containing a "Mhweb" reference "MhWeb:HS12345:PARTIAL"
        Then plugin should send an Eiffel SCE message to the RabbitMQ server I configured
        And the message should contain a "Mhweb" work item named "HS12345" with state "PARTIAL"

    @test_eforge_ref
    Scenario: Recognize eForge references by default.
        Given I create a project X
        And I did not enter any work item filters
        When I submit an approved change in project X with the commit message containing a "eForge" reference "TR:artf123456:COMPLETE"
        Then plugin should send an Eiffel SCE message to the RabbitMQ server I configured
        And the message should contain a "Eforge" work item named "artf123456" with state "COMPLETE"

    @test_custom_ref
    Scenario: Recognize custom references.
        Given I create a project X
        And I entered a work item filter that matches references of type "OSS Jira"
        When I submit an approved change in project X with the commit message containing a "OSS JIRA" reference "OSS Jira:DCE-309:REMOVED"
        Then plugin should send an Eiffel SCE message to the RabbitMQ server I configured
        And the message should contain a "OSS Jira" work item named "DCE-309" with state "REMOVED"

    @test_disable_plugin
    Scenario: Disabled plugin.
        Given I create a project X
        And I have disabled plugin for project X
        When I submit an approved change in project X
        Then plugin should NOT send any Eiffel message
