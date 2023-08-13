package top.nextnet.greekmythcoding.cmd;

import org.springframework.shell.command.annotation.Command;
import org.springframework.stereotype.Component;
import top.nextnet.greekmythcoding.onto.OntoFacade;

import javax.inject.Inject;

@Command
@Component
public class SaveOntologyCommand {
    @Inject
    OntoFacade facade;


    @Command(command = "save", group = "Components", description = "Sauvegarder l'ontologie")
    public void saveOntolgy() {
        facade.save();
        System.out.println("Ontology saved");
    }
}
