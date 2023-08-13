package top.nextnet.greekmythcoding.cmd;

import org.apache.jena.rdf.model.Resource;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.CompletionMatcherImpl;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.component.ConfirmationInput;
import org.springframework.shell.component.MultiItemSelector;
import org.springframework.shell.component.SingleItemSelector;
import org.springframework.shell.component.StringInput;
import org.springframework.shell.component.context.ComponentContext;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.component.support.SelectorItem;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.stereotype.Component;
import top.nextnet.greekmythcoding.cmd.exception.NoPreviousEpisodeException;
import top.nextnet.greekmythcoding.onto.CharacterAppearance;
import top.nextnet.greekmythcoding.onto.LabeledResource;
import top.nextnet.greekmythcoding.onto.OntoFacade;

import java.util.*;
import java.util.stream.Collectors;

@Command(command = "new")
@Component
public class EpisodeCommand extends AbstractShellComponent {

    @Autowired
    OntoFacade ontoFacade;

    @Command(command = "episode", group = "Components", description = "crée un nouvel épisode")
    public String episodeSelectionFlow() {


        LabeledResource book = simpleSingleMatchFromList("Choissez un livre dans la liste", ontoFacade.getBooks());

        List<Integer> existingEpisodes = ontoFacade.getExistingEpisodesNumberForBookList(book);
        Integer defaultEpisode = getNextEpisodeSuggestion(existingEpisodes);

        StringInput episodeSelectorComponent = new StringInput(getTerminal(), "Enter Episode number", defaultEpisode.toString());
        episodeSelectorComponent.setResourceLoader(getResourceLoader());
        episodeSelectorComponent.setTemplateExecutor(getTemplateExecutor());


        Integer userSelectedEpisodeNumber = Integer.parseInt(episodeSelectorComponent.run(StringInput.StringInputContext.empty()).getResultValue());

        Integer previousEpisodeNumber;
        try {
            previousEpisodeNumber = getPreviousEpisodeNumber(existingEpisodes, userSelectedEpisodeNumber);
        } catch (NoPreviousEpisodeException e) {
            previousEpisodeNumber = null;
        }

        if (!existingEpisodes.contains(userSelectedEpisodeNumber)) {
            return newEpisodeFlow(book, userSelectedEpisodeNumber, previousEpisodeNumber);
        } else {
            return editExistingEpisodeFlow(book, userSelectedEpisodeNumber);
        }

    }

    private LabeledResource simpleSingleMatchFromList(String prompt, Collection<LabeledResource> possibleValues) {
        LabeledResource bookResource;
        List<SelectorItem<LabeledResource>> books = possibleValues.stream().map(lr -> SelectorItem.of(lr.label(), lr)).collect(Collectors.toList());
        SingleItemSelector<LabeledResource, SelectorItem<LabeledResource>> selectorComponent = new SingleItemSelector<>(getTerminal(),
                books, prompt, null);
        selectorComponent.setResourceLoader(getResourceLoader());
        selectorComponent.setTemplateExecutor(getTemplateExecutor());
        SingleItemSelector.SingleItemSelectorContext<LabeledResource, SelectorItem<LabeledResource>> selectorContext = selectorComponent
                .run(SingleItemSelector.SingleItemSelectorContext.empty());
        return selectorContext.getResultItem().flatMap(si -> Optional.ofNullable(si.getItem())).get();

    }

    private String editExistingEpisodeFlow(LabeledResource bookResource, Integer userSelectedEpisodeNumber) {
        return "ko";
    }

    private String newEpisodeFlow(LabeledResource book, Integer episodeNumber, Integer previousEpisodeNumber) {
        //ask for episode label
        LabeledResource previousEpisodeResource = ontoFacade.getEpisode(book, previousEpisodeNumber);

        Set<String> titleCompletion = ontoFacade.getAllCharacters().stream().map(lr ->
                lr.label()).collect(Collectors.toSet());
        titleCompletion.addAll(ontoFacade.getAllTitlesToken());
        LineReader reader = LineReaderBuilder.builder()
                .terminal(getTerminal())
                .completer(new StringsCompleter(titleCompletion))
                .parser(new DefaultParser())
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
                .variable(LineReader.INDENTATION, 2)
                .option(LineReader.Option.INSERT_BRACKET, true)
                .build();
        getTerminal().writer().println("Quel est le titre de l'épisode?");
        String episodeLabel = reader.readLine();

        //create the episode
        Resource newEpisode = ontoFacade.addEpisode(book, episodeNumber, episodeLabel);

        //copy location from previous episode
        LabeledResource previousEpisodeLocation = ontoFacade.getLocationForEpisode(previousEpisodeResource.resourceAsStr());

        Boolean result = askSimpleYNQuestion("L'histoire se passe-t-elle toujours à  " + previousEpisodeLocation.label(), true);
        if (result) {
            ontoFacade.setLocationForEpisode(previousEpisodeLocation.resource(), newEpisode);
        } else {
            List<String> userLocationSelection = getUserSelectionForLocation(newEpisode, Arrays.asList(previousEpisodeLocation.resource()));
            userLocationSelection.stream().forEach(s -> ontoFacade.setLocationForEpisode(s, newEpisode));

        }

        Collection<LabeledResource> previousCharacters = ontoFacade.getCharactersFromPreviousEpisode(previousEpisodeResource.resourceAsStr());
        Collection<LabeledResource> userCharacterSelection = simpleMultimatchSelector("Indiquez les personnages toujours présents dans cet épisode", previousCharacters, previousCharacters);


        for (LabeledResource character : userCharacterSelection) {
            setCharacterconfig(character, newEpisode, previousEpisodeResource);
        }

        while (newCharacterFlow(newEpisode)) {

        }

        return "ok";
    }

    private Boolean askSimpleYNQuestion(String prompt, boolean defaultToYes) {
        ConfirmationInput confirmationInput = new ConfirmationInput(getTerminal(), prompt, defaultToYes);

        confirmationInput.setResourceLoader(getResourceLoader());
        confirmationInput.setTemplateExecutor(getTemplateExecutor());
        return confirmationInput.run(ComponentContext.empty()).getResultValue();
    }

    private boolean newCharacterFlow(Resource newEpisode) {
        if (askSimpleYNQuestion("Y a-t-il d'autres personnages?", false)) {
            CompletionMatcherImpl completionMatcher = new CompletionMatcherImpl();

            Collection<LabeledResource> characterTypes = ontoFacade.getSelectableCharacterTypes();




            List<LabeledResource> allChars = ontoFacade.getAllCharacters();
            getTerminal().writer().println("Entrez le nom du personnage");
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(getTerminal())
                    .completer(new StringsCompleter(allChars.stream().map(lr ->
                            lr.label()).collect(Collectors.toList())))
                    .parser(new DefaultParser())
                    .build();
            String characterLabel = reader.readLine().trim();
            Optional<LabeledResource> userSpecifiedCharacterInList = allChars.stream().filter(lr -> lr.label().equals(characterLabel)).findFirst();
            if (userSpecifiedCharacterInList.isPresent()) {
                setCharacterconfig(userSpecifiedCharacterInList.get(), newEpisode);
            } else {
                LabeledResource characterClass = simpleSingleMatchFromList("Choissez un type de personnage", characterTypes);
                LabeledResource newCharacter = ontoFacade.createNewCharacter(characterLabel);
                setCharacterconfig(newCharacter, newEpisode);
                if (askSimpleYNQuestion("Connait-on déjà les parents de ce personnage?", false)) {
                    List<LabeledResource> parents = simpleMultimatchSelector("Selectionner les parents de " + characterLabel, ontoFacade.getAllCharacters(), Collections.emptyList());
                    parents.forEach(p -> ontoFacade.setParentForCharacter(newCharacter, p.resourceAsStr()));
                }
                ontoFacade.setType(newCharacter.resource(), characterClass.resource());
                ontoFacade.setConcreteCharacter(newCharacter.resource());


            }


            return true;
        }
        return false;
    }


    @Autowired
    private ComponentFlow.Builder componentFlowBuilder;

    private void setCharacterconfig(LabeledResource characterResource, Resource episodeResource) {
        setCharacterconfig(characterResource, episodeResource, null);
    }

    private void setCharacterconfig(LabeledResource characterResource, Resource episodeResource, LabeledResource hintEpisodeResource) {
        CharacterAppearance appearance;
        if (hintEpisodeResource != null) {
            appearance = ontoFacade.getCharacterAppearanceInEpisode(characterResource.resource(), hintEpisodeResource.resource());
        } else {
            appearance = CharacterAppearance.getDefault();
        }
        List<LabeledResource> ageRanges = ontoFacade.getAgeRanges();
        List<LabeledResource> roles = ontoFacade.getRoleRange();
        Map<String, String> ageSelectItem = ageRanges.stream().collect(Collectors.toMap(LabeledResource::label, LabeledResource::resourceAsStr));
        Map<String, String> roleSelectItem = roles.stream().collect(Collectors.toMap(LabeledResource::label, LabeledResource::resourceAsStr));

        ComponentFlow flow = componentFlowBuilder.clone().reset()


                .withSingleItemSelector("age")
                .name("Specifier l'age de " + characterResource.label() + " dans cet épisode")
                .selectItems(ageSelectItem)
                .defaultSelect(appearance.ageRange().label())
                .and()
                .withSingleItemSelector("role")
                .name("Specifier le role de " + characterResource.label() + " dans cet épisode")
                .selectItems(roleSelectItem)
                .defaultSelect(appearance.role().label())
                .and()
                .build();
        ComponentContext results = flow.run().getContext();

        ontoFacade.addCharacterToEpisode(episodeResource, characterResource.resource(), results.get("age").toString(), results.get("role").toString());

    }

    private List<LabeledResource> simpleMultimatchSelector(String prompt, Collection<LabeledResource> possibleResources, Collection<LabeledResource> hints) {

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

    /**
     * Choose a location for the specified episode
     *
     * @param newEpisode
     */
    private List<String> getUserSelectionForLocation(Resource newEpisode, List<Resource> suggestions) {
        //get every possible locations
        List<LabeledResource> locations = ontoFacade.getAllLocations();
        List<SelectorItem<String>> items = locations.stream().map(lr -> SelectorItem.of(lr.label(), lr.resourceAsStr(), true, suggestions.contains(lr.resource()))).collect(Collectors.toList());

        MultiItemSelector<String, SelectorItem<String>> component = new MultiItemSelector<>(getTerminal(),
                items, "Choisissez le(s) lieux où se déroule(nt) cet épisode", null);
        component.setResourceLoader(getResourceLoader());
        component.setTemplateExecutor(getTemplateExecutor());
        MultiItemSelector.MultiItemSelectorContext<String, SelectorItem<String>> bookSelectorContext = component
                .run(SingleItemSelector.SingleItemSelectorContext.empty());
        return bookSelectorContext.getValues();

    }

    private static Integer getPreviousEpisodeNumber(List<Integer> existingEpisodes, Integer selectedEpisodeNumber) throws NoPreviousEpisodeException {
        return existingEpisodes.stream().sorted().filter(i -> i < selectedEpisodeNumber).reduce((first, second) -> second).orElseThrow(() -> new NoPreviousEpisodeException(selectedEpisodeNumber));
    }

    protected static Integer getNextEpisodeSuggestion(List<Integer> existingEpisodes) {
        List<Integer> existingEpisodesSorted = new ArrayList<>();
        existingEpisodesSorted.addAll(existingEpisodes);
        existingEpisodesSorted.sort(Comparator.naturalOrder());
        Integer defaultEpisode = null;
        if (!existingEpisodes.contains(1)) {
            defaultEpisode = 1;
        } else {
            Integer currEp = 1;
            for (Integer ep : existingEpisodesSorted) {
                if (ep - currEp >= 2) {
                    defaultEpisode = currEp + 1;
                    break;
                } else {
                    currEp = ep;
                }
            }
        }
        return defaultEpisode;
    }
}

