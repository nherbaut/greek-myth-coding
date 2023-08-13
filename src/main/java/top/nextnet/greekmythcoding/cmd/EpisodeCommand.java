package top.nextnet.greekmythcoding.cmd;

import org.apache.jena.rdf.model.Resource;
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


        List<SelectorItem<String>> books = ontoFacade.getBooks().stream().map(lr -> SelectorItem.of(lr.label(), lr.resourceAsStr())).collect(Collectors.toList());
        SingleItemSelector<String, SelectorItem<String>> bookSelectorComponent = new SingleItemSelector<>(getTerminal(),
                books, "Choose Book for the Episode", null);
        bookSelectorComponent.setResourceLoader(getResourceLoader());
        bookSelectorComponent.setTemplateExecutor(getTemplateExecutor());
        SingleItemSelector.SingleItemSelectorContext<String, SelectorItem<String>> bookSelectorContext = bookSelectorComponent
                .run(SingleItemSelector.SingleItemSelectorContext.empty());
        String bookResource = bookSelectorContext.getResultItem().flatMap(si -> Optional.ofNullable(si.getItem())).get();

        List<Integer> existingEpisodes = ontoFacade.getExistingEpisodesNumberForBookList(bookResource);
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
            return newEpisodeFlow(bookResource, userSelectedEpisodeNumber, previousEpisodeNumber);
        } else {
            return editExistingEpisodeFlow(bookResource, userSelectedEpisodeNumber);
        }

    }

    private String editExistingEpisodeFlow(String bookResource, Integer userSelectedEpisodeNumber) {
        return "ko";
    }

    private String newEpisodeFlow(String bookResource, Integer episodeNumber, Integer previousEpisodeNumber) {
        //ask for episode label
        LabeledResource previousEpisodeResource = ontoFacade.getEpisode(bookResource, previousEpisodeNumber);
        StringInput stringInput = new StringInput(getTerminal(), "Quel est le nom de l'histoire?", "");
        stringInput.setResourceLoader(getResourceLoader());
        stringInput.setTemplateExecutor(getTemplateExecutor());
        String episodeLabel = stringInput.run(ComponentContext.empty()).getResultValue();

        //create the episode
        Resource newEpisode = ontoFacade.addEpisode(bookResource, episodeNumber, episodeLabel);

        //copy location from previous episode
        LabeledResource previousEpisodeLocation = ontoFacade.getLocationForEpisode(previousEpisodeResource.resourceAsStr());
        ConfirmationInput confirmationInput = new ConfirmationInput(getTerminal(), "is " + previousEpisodeLocation.label() + " still the place where the story is happenning?");
        confirmationInput.setResourceLoader(getResourceLoader());
        confirmationInput.setTemplateExecutor(getTemplateExecutor());
        Boolean result = confirmationInput.run(ComponentContext.empty()).getResultValue();
        if (result) {
            ontoFacade.setLocationForEpisode(previousEpisodeLocation.resource(), newEpisode);
        } else {
            List<String> userLocationSelection = getUserSelectionForLocation(newEpisode, Arrays.asList(previousEpisodeLocation.resource()));
            userLocationSelection.stream().forEach(s -> ontoFacade.setLocationForEpisode(s, newEpisode));

        }

        List<LabeledResource> previousCharacters = ontoFacade.getCharactersFromPreviousEpisode(previousEpisodeResource.resourceAsStr());
        List<LabeledResource> userCharacterSelection = getUserSelectionFromCharacterList(previousCharacters);

        for (LabeledResource character : userCharacterSelection) {
            setCharacterconfig(character, newEpisode,previousEpisodeResource);
        }
        ontoFacade.dumpRDFXMLToFile();

        return "ok";
    }

    @Autowired
    private ComponentFlow.Builder componentFlowBuilder;

    private void setCharacterconfig(LabeledResource characterResource, Resource newEpisodeResource, LabeledResource previousEpisodeResource) {
        CharacterAppearance appearance = ontoFacade.getCharacterAppearanceInEpisode(characterResource.resource(), previousEpisodeResource.resource());
        List<LabeledResource> ageRanges = ontoFacade.getAgeRanges();
        List<LabeledResource> roles = ontoFacade.getRoleRange();
        Map<String,String> ageSelectItem = ageRanges.stream().collect(Collectors.toMap(LabeledResource::label,LabeledResource::resourceAsStr));
        Map<String,String> roleSelectItem = roles.stream().collect(Collectors.toMap(LabeledResource::label,LabeledResource::resourceAsStr));

        ComponentFlow flow = componentFlowBuilder.clone().reset()


                .withSingleItemSelector("age")
                .name("Specifier l'age de "+characterResource.label()+" dans cet épisode")
                .selectItems(ageSelectItem)
                .defaultSelect(appearance.ageRange().label())
                .and()
                .withSingleItemSelector("role")
                .name("Specifier le role de "+characterResource.label()+" dans cet épisode")
                .selectItems(roleSelectItem)
                .defaultSelect(appearance.role().label())
                .and()
                .build();
        ComponentContext results =  flow.run().getContext();

                ontoFacade.addCharacterToEpisode(newEpisodeResource,characterResource.resource(),results.get("age").toString(),results.get("role").toString());

    }

    private List<LabeledResource> getUserSelectionFromCharacterList(List<LabeledResource> previousCharacters) {

        List<SelectorItem<String>> items = previousCharacters.stream().map(lr -> SelectorItem.of(lr.label(), lr.resourceAsStr(), true, true)).collect(Collectors.toList());

        MultiItemSelector<String, SelectorItem<String>> component = new MultiItemSelector<>(getTerminal(),
                items, "Choisissez les personnages toujours présents dans cet épisode", null);
        component.setResourceLoader(getResourceLoader());
        component.setTemplateExecutor(getTemplateExecutor());
        MultiItemSelector.MultiItemSelectorContext<String, SelectorItem<String>> characterSelectorContext = component
                .run(SingleItemSelector.SingleItemSelectorContext.empty());
        List<String> selectedValues=characterSelectorContext.getValues();
        return previousCharacters.stream().filter(lr->selectedValues.contains(lr.resourceAsStr())).collect(Collectors.toList());

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

