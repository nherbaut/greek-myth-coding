package top.nextnet.greekmythcoding.cmd;

import de.codeshelf.consoleui.elements.ConfirmChoice;
import de.codeshelf.consoleui.prompt.*;
import de.codeshelf.consoleui.prompt.builder.CheckboxPromptBuilder;
import de.codeshelf.consoleui.prompt.builder.ConfirmPromptBuilder;
import de.codeshelf.consoleui.prompt.builder.ListPromptBuilder;
import de.codeshelf.consoleui.prompt.builder.PromptBuilder;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;
import top.nextnet.greekmythcoding.onto.LabeledObject;
import top.nextnet.greekmythcoding.onto.LabeledResource;
import top.nextnet.greekmythcoding.onto.OntoFacade;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public abstract class AbstractShellComponentImpl {


    @Autowired
    protected OntoFacade ontoFacade;

    public AbstractShellComponentImpl() {
        AnsiConsole.systemInstall();
    }

    protected <T extends LabeledObject<?>> T simpleSingleMatchFromList2(String prompt, Collection<T> possibleValues) {
        try {
            ConsolePrompt p = new ConsolePrompt();

            PromptBuilder promptBuilder = p.getPromptBuilder();
            ListPromptBuilder lpb = promptBuilder.createListPrompt().name("prompt").message(prompt);
            possibleValues.stream().forEach(l -> lpb.newItem(l.resourceAsStr()).text(l.label()).add());
            lpb.addPrompt();
            Map<String, ? extends PromtResultItemIF> result = p.prompt(promptBuilder.build());
            ListResult res = (ListResult) result.get("prompt");
            return possibleValues.stream().filter(lr -> lr.resourceAsStr().equals(res.getSelectedId())).findAny().orElseThrow();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }


    protected Boolean simpleAskYNQuestion(String prompt, boolean defaultToYes) {
        try {
            ConsolePrompt p = new ConsolePrompt();
            PromptBuilder promptBuilder = p.getPromptBuilder();
            ConfirmPromptBuilder lpb = promptBuilder.createConfirmPromp();
            lpb.name("confirm").message(prompt).defaultValue(defaultToYes ? ConfirmChoice.ConfirmationValue.YES : ConfirmChoice.ConfirmationValue.NO);
            lpb.addPrompt();
            Map<String, ? extends PromtResultItemIF> result = p.prompt(promptBuilder.build());
            ConfirmResult res = (ConfirmResult) result.get("confirm");
            return res.getConfirmed().equals(ConfirmChoice.ConfirmationValue.YES) ? true : false;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    protected <T extends LabeledObject> Collection<T> simpleMultimatchSelector(String prompt, Collection<T> possibleResources, Collection<T> hints) {
        try {
            ConsolePrompt p = new ConsolePrompt();
            PromptBuilder promptBuilder = p.getPromptBuilder();
            CheckboxPromptBuilder lpb = promptBuilder.createCheckboxPrompt();
            lpb.name("selection").message(prompt);
            possibleResources.forEach(lr -> lpb.newItem().text(lr.label()).checked(hints.contains(lr)).name(lr.resourceAsStr()).add());
            lpb.addPrompt();
            Map<String, ? extends PromtResultItemIF> result = p.prompt(promptBuilder.build());
            CheckboxResult res = (CheckboxResult) result.get("selection");
            return possibleResources.stream().filter(lr -> res.getSelectedIds().contains(lr.resourceAsStr())).collect(Collectors.toList());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }


    protected List<EditEpisodeStateMachineConfigurer.Events> getLegalEventsForCurrentState(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context) {
        var sm = context.getStateMachine();
        return sm.getTransitions().stream().filter(t -> t.getSource().getId().equals(sm.getState().getId())
                )

                .filter(t -> t.getGuard() == null || t.getGuard().apply(context).block())
                .map(t -> t.getTrigger().getEvent())
                .collect(Collectors.toList());
    }


    public <T> T simpleInputString(String prompt, String defaultValue, Function<String, T> converter) {
        return simpleInputStringWithHints(prompt, Collections.<T>emptyList(), defaultValue, converter);
    }

    public <I> I simpleInputStringWithHints(String prompt, Collection<I> hints, String defaultValue, Function<String, I> consumeUnknownInput) {
        Collection<String> hintsStr = hints.stream().map(lr -> lr.toString()).collect(Collectors.toList());
        ConsolePrompt p = new ConsolePrompt();
        PromptBuilder promptBuilder = p.getPromptBuilder();
        var inputValueBuilder = promptBuilder.createInputPrompt()
                .name("input")
                .message(prompt)
                .addCompleter(new jline.console.completer.StringsCompleter(hintsStr));

        if (!defaultValue.equals(null)) {

            inputValueBuilder.defaultValue(defaultValue.toString());
        }

        Map<String, ? extends PromtResultItemIF> res = Collections.EMPTY_MAP;
        try {
            res = p.prompt(inputValueBuilder.addPrompt().build());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        String locationUserInput = ((InputResult) (res.get("input"))).getInput();

        I newResource = hints
                .stream()
                .filter(lr -> locationUserInput.equals(lr.toString()))
                .findAny()
                .orElseGet(() ->
                        consumeUnknownInput.apply(locationUserInput)
                );
        return newResource;
    }

}



