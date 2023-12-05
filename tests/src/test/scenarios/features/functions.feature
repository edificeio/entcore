@functions-feature
Feature: Functions feature
  Make sure that functions are securely managed

  Scenario: ADML users cannot give themselves ADML rights on a structure they do not administrate
    Given A structure "s1"
      And A structure "s2"
      And A user "user1" on the structure "s1"
      And User "user1" is ADML on the structure "s1"
    When "user1" adds the ADML function to "user1" on structure "s2"
    Then I get a 401 response

  Scenario: ADML users can give ADML rights to other users on the structure they administrate
    Given A structure "s1"
    And A user "user1" on the structure "s1"
    And User "user1" is ADML on the structure "s1"
    And A user "user2" on the structure "s1"
    When "user1" adds the ADML function to "user2" on structure "s1"
    Then I get a 200 response

  Scenario: Non-ADML users cannot give ADML rights to other users on a structure
    Given A structure "s1"
    And A user "user1" on the structure "s1"
    And A user "user2" on the structure "s1"
    When "user1" adds the ADML function to "user2" on structure "s1"
    Then I get a 401 response

  Scenario: ADMC users can give ADML rights to other users on any structure
    Given A structure "s1"
    And A user "user1" on the structure "s1"
    When "ADMC" adds the ADML function to "user1" on structure "s1"
    Then I get a 200 response

