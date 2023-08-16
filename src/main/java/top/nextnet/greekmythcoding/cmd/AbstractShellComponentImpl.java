package top.nextnet.greekmythcoding.cmd;

import de.codeshelf.consoleui.prompt.ConsolePrompt;
import de.codeshelf.consoleui.prompt.InputResult;
import de.codeshelf.consoleui.prompt.ListResult;
import de.codeshelf.consoleui.prompt.PromtResultItemIF;
import de.codeshelf.consoleui.prompt.builder.ListPromptBuilder;
import de.codeshelf.consoleui.prompt.builder.PromptBuilder;
import org.apache.jena.rdf.model.Resource;
import org.fusesource.jansi.AnsiConsole;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.component.ConfirmationInput;
import org.springframework.shell.component.MultiItemSelector;
import org.springframework.shell.component.SingleItemSelector;
import org.springframework.shell.component.context.ComponentContext;
import org.springframework.shell.component.support.SelectorItem;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;
import top.nextnet.greekmythcoding.onto.LabeledObject;
import top.nextnet.greekmythcoding.onto.LabeledResource;
import top.nextnet.greekmythcoding.onto.OntoFacade;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public abstract class AbstractShellComponentImpl extends AbstractShellComponent {


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

    protected <T extends LabeledObject> T simpleSingleMatchFromList(String prompt, Collection<T> possibleValues) {
        LabeledResource bookResource;
        List<SelectorItem<T>> books = possibleValues.stream().map(lr -> SelectorItem.of(lr.label(), lr)).collect(Collectors.toList());
        SingleItemSelector<T, SelectorItem<T>> selectorComponent = new SingleItemSelector<>(getTerminal(),
                books, prompt, null);
        selectorComponent.setResourceLoader(getResourceLoader());
        selectorComponent.setTemplateExecutor(getTemplateExecutor());
        SingleItemSelector.SingleItemSelectorContext<T, SelectorItem<T>> selectorContext = selectorComponent
                .run(SingleItemSelector.SingleItemSelectorContext.empty());
        return selectorContext.getResultItem().flatMap(si -> Optional.ofNullable(si.getItem())).get();

    }

    protected Boolean simpleAskYNQuestion(String prompt, boolean defaultToYes) {
        ConfirmationInput confirmationInput = new ConfirmationInput(getTerminal(), prompt, defaultToYes);

        confirmationInput.setResourceLoader(getResourceLoader());
        confirmationInput.setTemplateExecutor(getTemplateExecutor());
        return confirmationInput.run(ComponentContext.empty()).getResultValue();
    }

    protected List<LabeledObject> simpleMultimatchSelector(String prompt, Collection<? extends LabeledObject> possibleResources, Collection<? extends LabeledObject> hints) {

        List<SelectorItem<String>> items = possibleResources.stream().map(lr -> SelectorItem.of(lr.label(), lr.resourceAsStr(), true, hints.contains(lr))).collect(Collectors.toList());

        MultiItemSelector<String, SelectorItem<String>> component = new MultiItemSelector<>(getTerminal(),
                items, prompt, null);
        component.setResourceLoader(getResourceLoader());
        component.setTemplateExecutor(getTemplateExecutor());
        MultiItemSelector.MultiItemSelectorContext<String, SelectorItem<String>> characterSelectorContext = component
                .run(SingleItemSelector.SingleItemSelectorContext.empty());
        List<String> selectedValues = characterSelectorContext.getValues();
        return possibleResources.stream().filter(lr -> selectedValues.contains(lr.resourceAsStr())).collect(Collectors.toList());

    }


    protected List<EditEpisodeStateMachineConfigurer.Events> getLegalEventsForCurrentState(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context) {
        var sm = context.getStateMachine();
        return sm.getTransitions().stream().filter(t -> t.getSource().getId().equals(sm.getState().getId())
                )

                .filter(t -> t.getGuard() == null || !t.getGuard().apply(context).block())
                .map(t -> t.getTrigger().getEvent())
                .collect(Collectors.toList());
    }

    public void askUserForEpisodeLocation(LabeledResource previousEpisodeResource, Resource newEpisode) {

        Collection<LabeledResource> allLocations = ontoFacade.getAllLocations().stream().collect(Collectors.toList());
        Collection<String> allLocationsStrs = allLocations.stream().map(lr -> lr.label()).collect(Collectors.toList());
        ConsolePrompt p = new ConsolePrompt();
        PromptBuilder promptBuilder = p.getPromptBuilder();
        var inputValueBuilder = promptBuilder.createInputPrompt()
                .name("location")
                .message("Où se déroule l'action?")
                .addCompleter(new jline.console.completer.StringsCompleter(allLocationsStrs));

        if (!previousEpisodeResource.equals(LabeledResource.getDefault())) {

            inputValueBuilder.defaultValue(ontoFacade.getLocationForEpisode(previousEpisodeResource).label());
        }

        Map<String, ? extends PromtResultItemIF> res = Collections.EMPTY_MAP;
        try {
            res = p.prompt(inputValueBuilder.addPrompt().build());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        String locationUserInput = ((InputResult) (res.get("location"))).getInput();

        LabeledResource episodeLocation = allLocations.stream().filter(lr -> locationUserInput.equals(lr.label())).findAny().orElseGet(() -> {
            LabeledResource locationType = askUserLocationType(locationUserInput);
            return ontoFacade.createNewLocationClass(locationUserInput, locationType);
        });
        ontoFacade.setLocationForEpisode(episodeLocation.resource(), newEpisode);


    }

    private LabeledResource askUserLocationType(String locationLabel) {
        LabeledResource newLocationType = simpleSingleMatchFromList2("Nouveau lieu détecté. Quel est le type du nouveau lieu?", ontoFacade.getAllLocationTypes());
        return ontoFacade.createNewLocationClass(locationLabel, newLocationType);
    }

    /**
     * Choose a location for the specified episode
     *
     * @param newEpisode
     */
    private List<String> getUserSelectionForLocation(Resource newEpisode, List<Resource> suggestions) {
        //get every possible locations
        Collection<LabeledResource> locations = ontoFacade.getAllLocations();
        List<SelectorItem<String>> items = locations.stream().map(lr -> SelectorItem.of(lr.label(), lr.resourceAsStr(), true, suggestions.contains(lr.resource()))).collect(Collectors.toList());

        MultiItemSelector<String, SelectorItem<String>> component = new MultiItemSelector<>(getTerminal(),
                items, "Choisissez le(s) lieux où se déroule(nt) cet épisode", null);
        component.setResourceLoader(getResourceLoader());
        component.setTemplateExecutor(getTemplateExecutor());
        MultiItemSelector.MultiItemSelectorContext<String, SelectorItem<String>> bookSelectorContext = component
                .run(SingleItemSelector.SingleItemSelectorContext.empty());
        return bookSelectorContext.getValues();

    }

    /**
     * Ask user from some piece of information as String, provide completion for already existing items
     *
     * @param hints values that may match user input
     * @return a labeled resource that contains both label and resource if the resource already exists, and just the label otherwrise (new data)
     */
    protected LabeledResource simpleUserStringInputWithHints(String prompt, Collection<LabeledResource> hints) {

        getTerminal().writer().print(prompt);
        LineReader reader = LineReaderBuilder.builder()
                .terminal(getTerminal())
                .completer(new StringsCompleter(hints.stream().map(lr ->
                        lr.label()).collect(Collectors.toList())))
                .parser(new DefaultParser())
                .build();
        String resourceLabel = reader.readLine().trim();
        Optional<LabeledResource> userSpecifiedInput = hints.stream().filter(lr -> lr.label().equals(resourceLabel)).findFirst();
        if (userSpecifiedInput.isPresent()) {
            return userSpecifiedInput.get();
        } else {
            return new LabeledResource(resourceLabel, null);
        }


    }


    public void createNewLocationClass(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context) {
        String locationLabel = context.getExtendedState().get(EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_LOCATION_LABEL, String.class);
        LabeledResource newLocationClass = askUserLocationType(locationLabel);
        context.getExtendedState().getVariables().put(EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_LOCATION, newLocationClass);
    }
}
