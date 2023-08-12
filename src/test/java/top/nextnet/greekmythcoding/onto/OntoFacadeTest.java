package top.nextnet.greekmythcoding.onto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OntoFacadeTest {

    @Test
    void getAllLocations() {
        OntoFacade facade = new OntoFacade();
        facade.getAllLocations();
    }
}