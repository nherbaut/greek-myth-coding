package top.nextnet.greekmythcoding;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import top.nextnet.greekmythcoding.cmd.EditEpisodeCommand;

@Component
public class MyApplicationRunner implements CommandLineRunner, ExitCodeGenerator {

    private final EditEpisodeCommand myCommand;

    private final IFactory factory; // auto-configured to inject PicocliSpringFactory

    private int exitCode;

    public MyApplicationRunner(EditEpisodeCommand myCommand, IFactory factory) {
        this.myCommand = myCommand;
        this.factory = factory;
    }

    @Override
    public void run(String... args) throws Exception {
        exitCode = new CommandLine(myCommand, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}