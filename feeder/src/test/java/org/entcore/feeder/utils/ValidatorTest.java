package org.entcore.feeder.utils;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ValidatorTest {


    @Test
    public void testGenerateFormattedLogin(final TestContext context) {
        // Test with regular names
        context.assertEquals("john.doe", Validator.generateFormattedLogin("John", "Doe"));
            
        // Test with names containing digits
        context.assertEquals("john123.doe456", Validator.generateFormattedLogin("John123", "Doe456"));

        // Test with names containing special characters
        context.assertEquals("alice.wonder", Validator.generateFormattedLogin("Alice!!", "Wonder@"));

        // Test with names containing spaces and apostrophes
        context.assertEquals("maryanne.oconnor", Validator.generateFormattedLogin("Mary Anne", "O'Connor"));

        // Test with names containing accents
        context.assertEquals("jose.garcia", Validator.generateFormattedLogin("José", "García"));

        // Test with names containing digits and special characters and spaces and accents
        context.assertEquals("john123fdzccdhhdr.doe456c3odsqfez", Validator.generateFormattedLogin("John123@^^€?fdzÇ+Cd HHd'r", "Doe456&&&!!?;/*$C3ôdsq«f %ez"));

        // Test with long names
        context.assertEquals("superlongfirstnamethatexceedst.longlastnameexceedinglimit", Validator.generateFormattedLogin("SuperLongFirstNameThatExceedsTheLimit", "LongLastNameExceedingLimit"));

        // Test with names exceeding the maximum length
        context.assertEquals("alice.wonderwithalonglastnameexceedi", Validator.generateFormattedLogin("Alice!!", "Wonder@WithALongLastNameExceedingLimit"));

        // Test with names exceeding the maximum length
        context.assertEquals("wonderwithalonglastnameexceedi.alice", Validator.generateFormattedLogin("Wonder@WithALongLastNameExceedingLimit", "Alice!!"));
        
        // Test with names exceeding maximum length after formatting
        context.assertEquals("supersupersupersupersupersuper.longlonglonglonglonglonglonglo", Validator.generateFormattedLogin("SuperSuperSuperSuperSuperSuperSuperSuperSuperSuperLongFirstName", "LongLongLongLongLongLongLongLongLongLongLongLastName"));
    }

}