package top.nextnet.greekmythcoding.cmd;

import de.codeshelf.consoleui.prompt.ConsolePrompt;
import de.codeshelf.consoleui.prompt.InputResult;
import de.codeshelf.consoleui.prompt.ListResult;
import de.codeshelf.consoleui.prompt.PromtResultItemIF;
import de.codeshelf.consoleui.prompt.builder.ListPromptBuilder;
import de.codeshelf.consoleui.prompt.builder.PromptBuilder;
import org.apache.jena.rdf.model.Resource;
import org.springframework.stereotype.Component;
import top.nextnet.greekmythcoding.cmd.exception.NoPreviousEpisodeException;
import top.nextnet.greekmythcoding.onto.CharacterAppearance;
import top.nextnet.greekmythcoding.onto.LabeledObject;
import top.nextnet.greekmythcoding.onto.LabeledResource;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class NewEpisodeCommand extends AbstractShellComponentImpl {

/*

    public String episodeSelectionFlow() {


        LabeledResource book = simpleSingleMatchFromList2("Choissez un livre dans la liste", ontoFacade.getBooks());

        List<Integer> existingEpisodes = ontoFacade.getExistingEpisodesNumberForBookList(book);
        Integer defaultEpisode = getNextEpisodeSuggestion(existingEpisodes);

        Integer userSelectedEpisodeNumber = Integer.parseInt(simpleUserInputWithDefault2("Entrez le numéro de l'épisode", defaultEpisode.toString()));

        Integer previousEpisodeNumber;
        try {
            previousEpisodeNumber = getPreviousEpisodeNumber(existingEpisodes, userSelectedEpisodeNumber);
        } catch (NoPreviousEpisodeException e) {
            previousEpisodeNumber = null;
        }

        if (!existingEpisodes.contains(userSelectedEpisodeNumber)) {
            return newEpisodeFlow(book, userSelectedEpisodeNumber, previousEpisodeNumber);
        } else {

            return editExistingEpisodeFlow(this.ontoFacade.getEpisode(book, userSelectedEpisodeNumber));
        }

    }

    private String simpleUserInputWithDefault2(String message, String defaultValue) {
        return simpleUserInputWithDefault2(message, defaultValue, Collections.emptyList());
    }

    private String simpleUserInputWithDefault2(String message, String defaultValue, Collection<String> hints) {
        try {
            ConsolePrompt p = new ConsolePrompt();
            PromptBuilder promptBuilder = p.getPromptBuilder();

            promptBuilder.createInputPrompt()
                    .name("inputPrompt")
                    .message(message)
                    .defaultValue(defaultValue)
                    .addCompleter(new jline.console.completer.StringsCompleter(hints))
                    .addPrompt();
            HashMap<String, ? extends PromtResultItemIF> res = p.prompt(promptBuilder.build());
            InputResult ir = (InputResult) res.get("inputPrompt");
            return ir.getInput();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    enum ModifyEpisodeFlow {

        CHANGE_CHARACTERS("Modifier les personnages"),
        CHANGE_LOCATION("Modifier le lieu"),
        NONE("Ne rien faire");


        public String getLabel() {
            return label;
        }

        final String label;

        private ModifyEpisodeFlow(String str) {
            this.label = str;
        }
    }

    private String editExistingEpisodeFlow(LabeledResource episode) {

        return null;
    }

    enum ChangeCharacterMenu {
        NEW,
        DELETE,
        EDIT;
    }

    private void editExistingEpisodeFlowChangeCharacter(Resource episode) {

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

        askUserForEpisodeLocation(previousEpisodeResource, newEpisode);

        Collection<LabeledCharacterAppearance> previousCharacters = ontoFacade.getCharactersIndividualsFromPreviousEpisode(previousEpisodeResource.resourceAsStr());

        Collection<LabeledObject> userCharacterSelection = simpleMultimatchSelector("Indiquez les personnages toujours présents dans cet épisode", previousCharacters, previousCharacters);


        for (LabeledObject character : userCharacterSelection) {
            LabeledCharacterAppearance characterAppearance = (LabeledCharacterAppearance) character;
            ontoFacade.addCharacterToEpisode(newEpisode,characterAppearance.resource().character().resource(),characterAppearance.resource().ageRange().resource(),characterAppearance.resource().role().resource());
        }

        while (newCharacterFlow(newEpisode)) {

        }

        return "ok";
    }

    private boolean newCharacterFlow(Resource newEpisode) {
        if (simpleAskYNQuestion("Y a-t-il d'autres personnages?", false)) {
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
                if (simpleAskYNQuestion("Connait-on déjà les parents de ce personnage?", false)) {
                    List<LabeledObject> parents = simpleMultimatchSelector("Selectionner les parents de " + characterLabel, ontoFacade.getAllCharacters(), Collections.emptyList());
                    parents.forEach(p -> ontoFacade.setParentForCharacter(newCharacter, p.resourceAsStr()));
                }
                ontoFacade.setType(newCharacter.resource(), characterClass.resource());
                ontoFacade.setConcreteCharacter(newCharacter.resource());


            }


            return true;
        }
        return false;
    }


    private void setCharacterconfig(LabeledResource characterResource, Resource episodeResource) {
        setCharacterconfig(characterResource, LabeledResource.fromRessource(episodeResource), null);
    }

    private <T extends LabeledObject> void setCharacterconfig(LabeledResource characterResource, T episodeResource, LabeledObject<T> hintEpisodeResource) {
        CharacterAppearance appearance;
        if (hintEpisodeResource != null) {
            appearance = ontoFacade.getCharacterAppearanceInEpisode(characterResource.resource(), (Resource) (hintEpisodeResource.resource()));
        } else {
            appearance = CharacterAppearance.getDefault();
        }
        List<LabeledResource> ageRanges = ontoFacade.getAgeRanges();
        List<LabeledResource> roles = ontoFacade.getRoleRange();

        ConsolePrompt prompt = new ConsolePrompt();
        PromptBuilder promptBuilder = prompt.getPromptBuilder();
        try {
            {
                ListPromptBuilder lpb = promptBuilder.createListPrompt().name("age")
                        .message("Specifier l'age de " + characterResource.label() + " dans cet épisode");
                if (appearance.ageRange().resource() != null) {
                    lpb.newItem(appearance.ageRange().resource().toString()).text(appearance.ageRange().label()).add();
                }
                ageRanges.stream()
                        .filter(ar -> !ar.equals(appearance.ageRange()))
                        .forEach(r -> lpb.newItem(r.resourceAsStr()).text(r.label()).add());
                promptBuilder = lpb.addPrompt();
            }
            {
                ListPromptBuilder lpb = promptBuilder.createListPrompt().name("role")
                        .message("Specifier le rôle de " + characterResource.label() + " dans cet épisode");
                if (appearance.role().resource() != null) {
                    lpb.newItem(appearance.role().resource().toString()).text(appearance.role().label()).add();
                }
                roles.stream()

                        .filter(ar -> !ar.equals(appearance.role()))
                        .forEach(r -> lpb.newItem(r.resourceAsStr()).text(r.label()).add());
                promptBuilder = lpb.addPrompt();
            }
            HashMap<String, ? extends PromtResultItemIF> result = prompt.prompt(promptBuilder.build());
            ontoFacade.addCharacterToEpisode(episodeResource.resourceAsStr(), characterResource.resourceAsStr(), ((ListResult) result.get("age")).getSelectedId(), ((ListResult) result.get("role")).getSelectedId());
        } catch (IOException ieo) {
            throw new RuntimeException(ieo);
        }


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
    }*/
}

