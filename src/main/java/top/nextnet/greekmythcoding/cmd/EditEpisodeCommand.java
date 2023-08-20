package top.nextnet.greekmythcoding.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import reactor.core.publisher.Mono;
import top.nextnet.greekmythcoding.onto.CharacterAppearance;
import top.nextnet.greekmythcoding.onto.LabeledObject;
import top.nextnet.greekmythcoding.onto.LabeledResource;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;


@Component
@CommandLine.Command(name = "episode")
public class EditEpisodeCommand extends AbstractShellComponentImpl implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditEpisodeCommand.class);


    @Autowired
    StateMachineFactory<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> factory;
    StateMachine<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> stateMachine;

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    private boolean running = true;

    @Override
    public Integer call() throws Exception {
        stateMachine = factory.getStateMachine();
        stateMachine.getExtendedState().getVariables().put("command", this);

        stateMachine.addStateListener(new StateMachineListenerAdapter<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events>() {
            @Override
            public void stateChanged(State<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> from, State<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> to) {
                LOGGER.info(from.getId().name() + " -> " + to.getId().name());
            }

            @Override
            public void transition(Transition<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> transition) {
                LOGGER.info(transition.getName());
            }
        });
        stateMachine.startReactively().subscribe();
        stateMachine.sendEvent(EditEpisodeStateMachineConfigurer.Events.START_EPISODE_CREATION);

        while (this.running) {

            Thread.sleep(1000);
        }

        return 0;

    }

    public void commandStarted(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context) {
        System.out.println("current thread in commandStarted" + Thread.currentThread().getId());
        Collection<LabeledObject<EditEpisodeStateMachineConfigurer.Events>> nextLegalEvents = getLegalEventsForCurrentState(context).stream().map(new Function<EditEpisodeStateMachineConfigurer.Events, LabeledObject<EditEpisodeStateMachineConfigurer.Events>>() {
            @Override
            public LabeledObject<EditEpisodeStateMachineConfigurer.Events> apply(EditEpisodeStateMachineConfigurer.Events events) {
                return new LabeledObject<EditEpisodeStateMachineConfigurer.Events>() {
                    @Override
                    public String label() {
                        return events.getLabel();
                    }

                    @Override
                    public EditEpisodeStateMachineConfigurer.Events resource() {
                        return events;
                    }
                };
            }
        }).collect(Collectors.toSet());
        if (nextLegalEvents.size() == 1) {
            stateMachine.sendEvent(nextLegalEvents.iterator().next().resource());
        } else if (nextLegalEvents.size() >= 1) {
            var e = simpleSingleMatchFromList2("Que souhaitez-vous faire?", nextLegalEvents);
            stateMachine.sendEvent(e.resource());
        } else {
            stateMachine.sendEvent(EditEpisodeStateMachineConfigurer.Events.FINISH_EPISODE_CREATION);
        }

    }


    public void askUserForCharacters(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context, EditEpisodeStateMachineConfigurer.ExtendedState extendedState, EditEpisodeStateMachineConfigurer.Events events) {
        Collection<LabeledObject> allCharacters = new ArrayList<>();
        Collection<LabeledCharacterAppearance> appearances = getAppearancesFromContext(context);

        if (appearances.size() == 0) { //noone here, preload with previous characters
            LabeledResource previousEpisode = getPreviousEpisodeFromContext(context);

            allCharacters.addAll(ontoFacade.getAllCharacters());

            appearances = ontoFacade.getCharactersIndividualsFromPreviousEpisode(previousEpisode);
        }
        appearances.retainAll(simpleMultimatchSelector("Quels sont les personnages toujours présents dans l'épisode?", appearances, appearances));
        Collection<LabeledResource> allRetainedCharacters = appearances.stream().map(lca -> lca.resource().character()).collect(Collectors.toSet());
        allCharacters = allCharacters.stream().filter(lr -> !allRetainedCharacters.contains(lr.resource())).collect(Collectors.toSet());
        ;
        final Collection<LabeledResource> allCharacterClasses = ontoFacade.getAllCharacterTypes();
        final Collection<LabeledResource> allAgeRanges = ontoFacade.getAgeRanges();
        final Collection<LabeledResource> allRoles = ontoFacade.getRoleRange();
        while (simpleAskYNQuestion("Y-a-t-il d'autres personnages dans l'épisode?", false)) {
            LabeledCharacterAppearance newCharacterAppearanceInEpisode = (LabeledCharacterAppearance) simpleInputStringWithHints("Quel est le nom du personnage?", allCharacters, "", new Function<String, LabeledObject>() {

                @Override
                public LabeledCharacterAppearance apply(String s) {
                    LabeledResource newClassOfCharacter = simpleSingleMatchFromList2("Quel est le type du personnage?", allCharacterClasses);
                    LabeledResource characterClass = ontoFacade.createNewCharacterClass(s,newClassOfCharacter);
                    LabeledResource newCharacterAgeInEpisode = simpleSingleMatchFromList2("Quelle est la classe d'age du personnage dans cet épisode?", allAgeRanges);
                    LabeledResource newCharacterRoleInEpisode = simpleSingleMatchFromList2("Quelle est le rôle du personnage dans cet épisode?", allRoles);

                    String characterLabel = String.format("%s (%s, %s) dans %s", s, newCharacterAgeInEpisode.label(), newCharacterRoleInEpisode.label(), context.getExtendedState().get(EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_NAME_STR, EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_NAME_STR.getType()));
                    return new LabeledCharacterAppearance(characterLabel, CharacterAppearance.getBuilder().withCharacter(characterClass.resource()).withAgeRange(newCharacterAgeInEpisode.resource()).withRole(newCharacterRoleInEpisode.resource()).build());


                }
            });
            appearances.add(newCharacterAppearanceInEpisode);
        }

        saveAndSendEvent(context, appearances, extendedState, events);

    }

    private Collection<LabeledCharacterAppearance> getAppearancesFromContext(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context) {

        Collection<LabeledCharacterAppearance> appearanceFromContext = context.getExtendedState().get(EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_CHARACTER_APPEARANCE, Collection.class);
        if (appearanceFromContext == null) {
            return Collections.emptySet();
        } else {
            return appearanceFromContext;
        }
    }

    private LabeledResource getPreviousEpisodeFromContext(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context) {

        LabeledResource previousEpisodeInContext = context.getExtendedState().get(EditEpisodeStateMachineConfigurer.ExtendedState.PREVIOUS_EPISODE, LabeledResource.class);
        if (previousEpisodeInContext != null) {
            return previousEpisodeInContext;
        } else {
            LabeledResource book = context.getExtendedState().get(EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_BOOK, LabeledResource.class);
            Integer episodeNumber = context.getExtendedState().get(EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_NUMBER, Integer.class);
            return ontoFacade.getPreviousEpisode(book, episodeNumber);
        }
    }

    protected static Integer getNextEpisodeSuggestion(List<Integer> existingEpisodes) {
        for (int i = 1; i < 100; i++) {
            if (!existingEpisodes.contains(i)) {
                return i;
            }
        }
        return -1;
    }


    public void askUserForEpisodeNumber(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context, EditEpisodeStateMachineConfigurer.ExtendedState extendedState, EditEpisodeStateMachineConfigurer.Events events) {
        List<Integer> episodesForBook = ontoFacade.getExistingEpisodesNumberForBookList(context.getExtendedState().get(EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_BOOK, LabeledResource.class));
        Integer defaultEpisode = getNextEpisodeSuggestion(episodesForBook);
        String defaultValue = "" + defaultEpisode;
        if (defaultEpisode == -1) {
            defaultValue = "";
        }
        Integer episodeNumber = this.<Integer>simpleInputString("Quel est le numéro de l'épisode?", "" + defaultEpisode, Integer::valueOf);
        saveAndSendEvent(context, episodeNumber, extendedState, events);
    }

    public void askUserForBook(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context, EditEpisodeStateMachineConfigurer.ExtendedState extendedState, EditEpisodeStateMachineConfigurer.Events events) {
        LabeledResource book = this.simpleSingleMatchFromList2("De quel livre est tiré l'épisode?", ontoFacade.getBooks());
        saveAndSendEvent(context, book, extendedState, events);
    }

    private void saveAndSendEvent(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context, EditEpisodeStateMachineConfigurer.Events events) {
        context.getStateMachine().sendEvent(Mono.just(MessageBuilder.withPayload(events).build())).doOnError(Throwable::printStackTrace)
                .subscribe();
    }

    private void saveAndSendEvent(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context, Object resource, EditEpisodeStateMachineConfigurer.ExtendedState extendedState, EditEpisodeStateMachineConfigurer.Events events) {
        context.getExtendedState().getVariables().put(extendedState, resource);
        saveAndSendEvent(context, events);

    }

    public void saveInOntology(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context, EditEpisodeStateMachineConfigurer.ExtendedState extendedState) {
        switch (extendedState) {
            case EPISODE_LOCATION -> {
            }
            case EPISODE_CHARACTER_APPEARANCE -> {
            }
            case EPISODE_NUMBER -> {
            }
            case EPISODE_BOOK -> {
            }
        }
    }

    public void askUserForEpisodeLocation(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context, EditEpisodeStateMachineConfigurer.ExtendedState extendedState, EditEpisodeStateMachineConfigurer.Events events) {
        LabeledResource previousEpisode = getPreviousEpisodeFromContext(context);
        LabeledResource previousLocation;
        if (previousEpisode.equals(LabeledResource.getDefault())) {
            previousLocation = LabeledResource.getDefault();
        } else {
            previousLocation = ontoFacade.getLocationIndividualsFromPreviousEpisode(previousEpisode);
        }

        context.getExtendedState().getVariables().put(EditEpisodeStateMachineConfigurer.ExtendedState.PREVIOUS_EPISODE, previousEpisode);

        Collection<LabeledResource> allLocations = ontoFacade.getAllLocations();
        LabeledResource newLocation = simpleInputStringWithHints("Dans quel lieu se déroule l'action?", allLocations, previousLocation.label(), new Function<String, LabeledResource>() {
            @Override
            public LabeledResource apply(String s) {
                LabeledResource locationType = simpleSingleMatchFromList2("Quel est le type de ce lieu?", ontoFacade.getAllLocationTypes());
                return ontoFacade.createNewLocationClass(s, locationType);
            }
        });

        saveAndSendEvent(context, newLocation, extendedState, events);

    }

    public void askUserSaveConfirmation(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context, EditEpisodeStateMachineConfigurer.Events events) {
        if (simpleAskYNQuestion("Savegarder les modifications?", true)) {
            String episodeName = (String) context.getExtendedState().get(EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_NAME_STR, EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_NAME_STR.getType());
            LabeledResource episodeBook = context.getExtendedState().get(EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_BOOK, LabeledResource.class);
            Integer episodeNumber = context.getExtendedState().get(EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_NUMBER, Integer.class);
            LabeledResource episodeLocation = context.getExtendedState().get(EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_LOCATION, LabeledResource.class);
            Collection<LabeledCharacterAppearance> characterAppearances = context.getExtendedState().get(EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_CHARACTER_APPEARANCE, Collection.class);
            ontoFacade.saveEpisode(episodeName, episodeBook, episodeNumber, episodeLocation, characterAppearances);
            try {
                ontoFacade.save();
            } catch (Throwable t) {
                t.printStackTrace();
                throw new RuntimeException(t);
            }
        }

        saveAndSendEvent(context, events);
    }

    public void askUserForEpisodeName(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context, EditEpisodeStateMachineConfigurer.ExtendedState extendedState, EditEpisodeStateMachineConfigurer.Events events) {

        Set<String> hints = new HashSet<>();
        hints.addAll(this.ontoFacade.getAllLocations().stream().map(LabeledResource::label).collect(Collectors.toSet()));
        hints.addAll(this.ontoFacade.getAllCharacters().stream().map(LabeledResource::label).collect(Collectors.toSet()));
        hints.addAll(this.ontoFacade.getAllEpisodes().stream().map(lr -> Arrays.stream(lr.label().split(" "))).flatMap(Function.identity()).collect(Collectors.toSet()));
        String episodeName = simpleInputStringWithHints("Quel est le nom de l'épisode?", hints, "", Function.<String>identity());
        saveAndSendEvent(context, episodeName, extendedState, events);
    }
}

