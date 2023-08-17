package top.nextnet.greekmythcoding.cmd;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import top.nextnet.greekmythcoding.onto.OntoFacade;



@Component
public class SaveOntologyCommand {
    @Autowired
    OntoFacade facade;

    public void saveOntolgy() {
        facade.save();
        System.out.println("Ontology saved");
    }
}
