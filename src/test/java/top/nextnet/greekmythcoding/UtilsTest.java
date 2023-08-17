package top.nextnet.greekmythcoding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void sanitizeURI() {
        assertEquals("https://qsdfqsdf_qsdfqsdf____",Utils.sanitizeURI("https://qsdfqsdf qsdfqsdfààà "));
    }
}