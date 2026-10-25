package util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import model.Command;
import model.CurrentAccount;
import model.SavingsAccount;
import model.TransactionType;
import org.junit.jupiter.api.Test;

class ValidatorTest {

    @Test
    void mobile() {
        assertTrue(Validator.isValidMobile("9876543210"));
        assertFalse(Validator.isValidMobile("12345"));
        assertFalse(Validator.isValidMobile("5876543210"));
        assertFalse(Validator.isValidMobile(null));
    }

    @Test
    void email() {
        assertTrue(Validator.isValidEmail("abc@xyz.com"));
        assertFalse(Validator.isValidEmail("abc@xyz"));
        assertFalse(Validator.isValidEmail("abc.com"));
    }

    @Test
    void panAndIfsc() {
        assertTrue(Validator.isValidPan("ABCDE1234F"));
        assertFalse(Validator.isValidPan("abcde1234f"));
        assertTrue(Validator.isValidIfsc("SBIN0001234"));
        assertFalse(Validator.isValidIfsc("SBIN1001234"));
    }

    @Test
    void amount() {
        assertTrue(Validator.isValidAmount("500"));
        assertFalse(Validator.isValidAmount("0"));
        assertFalse(Validator.isValidAmount("-5"));
        assertFalse(Validator.isValidAmount("5.5"));
    }

    @Test
    void parseCommand() {
        Command c = CommandParser.parse("DEPOSIT AC0001 500");
        assertEquals(TransactionType.DEPOSIT, c.type());
        assertEquals("AC0001", c.accountNumber());
        assertEquals(500, c.amount());
    }

    @Test
    void parseRejectsBadLines() {
        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("DEPOSIT AC0001"));
        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("HELLO AC0001 5"));
        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("DEPOSIT AC0001 abc"));
    }

    @Test
    void annotationValidatorFindsErrors() {
        String[] errors = AnnotationValidator.validate(new SavingsAccount("AC7101", "Riya", 0, 0));
        assertEquals(1, errors.length);
        assertEquals("must be > 0", errors[0]);
        assertEquals(0, AnnotationValidator.validate(new CurrentAccount("AC7102", "Riya", 100, 0)).length);
    }

    @Test
    void statementContainsDetails() {
        String s = StatementFormatter.buildStatement(new SavingsAccount("AC7201", "Riya", 4000, 0));
        assertTrue(s.contains("AC7201"));
        assertTrue(s.contains("Riya"));
        assertTrue(s.contains("4000"));
    }
}
