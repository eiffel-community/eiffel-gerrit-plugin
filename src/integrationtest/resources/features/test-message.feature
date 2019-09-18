Feature: Testing project's RabbitMQ settings.
    As a user I want to be able to test my RabbitMQ settings.

    Background:
        Given Eiffel plugin is installed, loaded and enabled for project X in Gerrit
        And I configured RabbitMQ settings for project X

    @test_send_button
    Scenario: Sending a test Eiffel message.
        Given I create a project X
        And I configured RabbitMQ settings for project X
        When I click "Send test Eiffel message" button in Gerrit's page of project X
        Then plugin should send a test message to the RabbitMQ server I configured