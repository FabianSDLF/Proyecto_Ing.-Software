package com.ubb.practicas.service;
import com.ubb.practicas.controller.HelpController;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HelpContactTest {
    @Test void noArbitraryFallbackContact() {
        var contact=new HelpController("", "", "").contact();
        assertEquals(false,contact.get("configured"));assertEquals("",contact.get("email"));
    }
    @Test void explicitConfigurationIsTrimmed() {
        var contact=new HelpController(" Coordinación ", " coordinacion@example.test ", "123").contact();
        assertEquals(true,contact.get("configured"));assertEquals("Coordinación",contact.get("name"));
    }
}
