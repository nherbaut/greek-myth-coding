package top.nextnet.greekmythcoding.cmd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import top.nextnet.greekmythcoding.onto.LabeledResource;

import java.util.Collection;

@Configuration
@EnableStateMachineFactory
public class EditEpisodeStateMachineConfigurer
        extends EnumStateMachineConfigurerAdapter<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> {


    @Override
    public void configure(StateMachineConfigurationConfigurer<States, Events> config)
            throws Exception {
        config
                .withConfiguration()
                .autoStartup(true);
    }

    private static EditEpisodeCommand getCommand(StateContext<States, Events> context) {
        return context.getExtendedState().get("command", EditEpisodeCommand.class);
    }

    @Override
    public void configure(StateMachineStateConfigurer<States, Events> states)
            throws Exception {
        states
                .withStates()
                .initial(States.INITIAL)
                .state(States.INITIAL)
                .state(States.NEW_EPISODE_CONFIGURATION_MENU, (context) -> {
                            getCommand(context).commandStarted(context);
                        }
                )
                .state(States.CONFIGURE_LOCATION, (context) -> {
                    getCommand(context).askUserForEpisodeLocation(context, ExtendedState.EPISODE_LOCATION, Events.FINISH_CONFIGURE_LOCATION);
                }, errorAction())
                .state(States.CONFIGURE_CHARACTER, (context) -> {
                    getCommand(context).askUserForCharacters(context, ExtendedState.EPISODE_CHARACTER_APPEARANCE, Events.FINISH_CONFIGURE_CHARACTER);
                })
                .state(States.CONFIGURE_EPISODE_NUMBER, (context) -> {
                    getCommand(context).askUserForEpisodeNumber(context, ExtendedState.EPISODE_NUMBER, Events.FINISH_CONFIGURE_EPISODE_NUMBER);
                })
                .state(States.CONFIGURE_BOOK, (context) -> {
                    getCommand(context).askUserForBook(context, ExtendedState.EPISODE_BOOK, Events.FINISH_CONFIGURE_BOOK);
                })
                .state(States.ASK_SAVE_ONTOLOGY, (context) -> {
                    getCommand(context).askUserSaveConfirmation(context, Events.WORK_DONE);
                })
                .state(States.CONFIGURE_EPISODE_NAME, (context) -> {
                    getCommand(context).askUserForEpisodeName(context, ExtendedState.EPISODE_NAME_STR, Events.FINISH_CONFIGURE_EPISODE_NAME);
                })
                .state(States.FINAL, (context -> {
                    System.out.println("all done");
                    getCommand(context).setRunning(false);
                }));


    }


    @Override
    public void configure(StateMachineTransitionConfigurer<States, Events> transitions)
            throws Exception {
        transitions
                .withExternal()
                .source(States.INITIAL)
                .target(States.NEW_EPISODE_CONFIGURATION_MENU)
                .event(Events.START_EPISODE_CREATION)
                .and()
                .withExternal()
                .source(States.NEW_EPISODE_CONFIGURATION_MENU)
                .target(States.CONFIGURE_LOCATION)
                .event(Events.START_CONFIGURE_LOCATION)
                .guard(CompositeGuard.combine(
                        extendedStateGard(WrappedEnumValidator.build(ExtendedState.EPISODE_LOCATION, false)),
                        extendedStateGard(WrappedEnumValidator.build(ExtendedState.EPISODE_BOOK, true)),
                        extendedStateGard(WrappedEnumValidator.build(ExtendedState.EPISODE_NUMBER, true)),
                        extendedStateGard(WrappedEnumValidator.build(ExtendedState.EPISODE_NAME_STR, true))
                        )
                )

                .and()
                .withExternal()
                .source(States.NEW_EPISODE_CONFIGURATION_MENU)
                .target(States.CONFIGURE_BOOK)
                .event(Events.START_CONFIGURE_BOOK)
                .guard(extendedStateGard(WrappedEnumValidator.build(ExtendedState.EPISODE_BOOK, false)))
                .and()
                .withExternal()
                .source(States.NEW_EPISODE_CONFIGURATION_MENU)
                .target(States.CONFIGURE_EPISODE_NAME)
                .event(Events.START_CONFIGURE_EPISODE_NAME)
                .guard(extendedStateGard(WrappedEnumValidator.build(ExtendedState.EPISODE_NAME_STR, false), WrappedEnumValidator.build(ExtendedState.EPISODE_NAME_STR, false)))
                .and()
                .withExternal()
                .source(States.NEW_EPISODE_CONFIGURATION_MENU)
                .target(States.CONFIGURE_EPISODE_NUMBER)
                .event(Events.START_CONFIGURE_EPISODE_NUMBER)
                .guard(extendedStateGard(WrappedEnumValidator.build(ExtendedState.EPISODE_BOOK, true), WrappedEnumValidator.build(ExtendedState.EPISODE_NUMBER, false)))
                .and()
                .withExternal()
                .source(States.CONFIGURE_EPISODE_NAME)
                .target(States.NEW_EPISODE_CONFIGURATION_MENU)
                .event(Events.FINISH_CONFIGURE_EPISODE_NAME)
                .and()
                .withExternal()
                .source(States.CONFIGURE_LOCATION)
                .target(States.NEW_EPISODE_CONFIGURATION_MENU)
                .event(Events.FINISH_CONFIGURE_LOCATION)
                .action((context) -> getCommand(context).saveInOntology(context, ExtendedState.EPISODE_LOCATION))
                .and()
                .withExternal()
                .source(States.CONFIGURE_BOOK)
                .target(States.NEW_EPISODE_CONFIGURATION_MENU)
                .event(Events.FINISH_CONFIGURE_BOOK)
                .action((context) -> getCommand(context).saveInOntology(context, ExtendedState.EPISODE_BOOK))
                .and()
                .withExternal()
                .source(States.CONFIGURE_EPISODE_NUMBER)
                .target(States.NEW_EPISODE_CONFIGURATION_MENU)
                .event(Events.FINISH_CONFIGURE_EPISODE_NUMBER)
                .action((context) -> getCommand(context).saveInOntology(context, ExtendedState.EPISODE_NUMBER))
                .and()
                .withExternal()
                .source(States.NEW_EPISODE_CONFIGURATION_MENU)
                .target(States.CONFIGURE_CHARACTER)
                .event(Events.START_CONFIGURE_CHARACTER)
                .guard(extendedStateGard(WrappedEnumValidator.build(ExtendedState.EPISODE_BOOK, true),
                        WrappedEnumValidator.build(ExtendedState.EPISODE_NUMBER, true)))
                .and()
                .withExternal()
                .source(States.CONFIGURE_CHARACTER)
                .target(States.NEW_EPISODE_CONFIGURATION_MENU)
                .event(Events.FINISH_CONFIGURE_CHARACTER)
                .and()
                .withExternal()
                .source(States.NEW_EPISODE_CONFIGURATION_MENU)
                .target(States.ASK_SAVE_ONTOLOGY)
                .event(Events.FINISH_EPISODE_CREATION)
                .guard(CompositeGuard.combine(
                        characterGard(1),
                        extendedStateGard(WrappedEnumValidator.build(ExtendedState.EPISODE_LOCATION, true)),
                        extendedStateGard(WrappedEnumValidator.build(ExtendedState.EPISODE_BOOK, true)),
                        extendedStateGard(WrappedEnumValidator.build(ExtendedState.EPISODE_NUMBER, true)))
                )
                .and()
                .withExternal()
                .source(States.ASK_SAVE_ONTOLOGY)
                .target(States.FINAL)
                .event(Events.WORK_DONE)
        ;


    }

    @Bean
    public Action<States, Events> errorAction() {
        return new Action<States, Events>() {

            @Override
            public void execute(StateContext<States, Events> context) {
                // RuntimeException("MyError") added to context
                Exception exception = context.getException();
                if (exception != null)
                    exception.getMessage();
            }
        };
    }

    static class WrappedEnumValidator<T extends Enum> {
        private T e;
        private boolean needTrue;

        private WrappedEnumValidator(T e, boolean needTrue) {
            this.e = e;
            this.needTrue = needTrue;
        }

        public static <T extends Enum> WrappedEnumValidator<T> build(T ee, boolean n) {
            return new WrappedEnumValidator<T>(ee, n);
        }
    }

    private <S extends Enum, E extends Enum, ES extends Enum> Guard<S, E> extendedStateGard(WrappedEnumValidator<ES>... extendedStates) {
        return new Guard<>() {
            @Override
            public boolean evaluate(StateContext<S, E> context) {
                for (WrappedEnumValidator<ES> extendedState : extendedStates) {
                    if ((context.getExtendedState().getVariables().get(extendedState.e) == null) == extendedState.needTrue) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    private Guard<States, Events> characterGard(int minimalCharacterConfigured) {
        return new Guard<States, Events>() {
            @Override
            public boolean evaluate(StateContext<States, Events> context) {
                Collection characterResourceCollection = (Collection) context.getExtendedState().getVariables().get(ExtendedState.EPISODE_CHARACTER_APPEARANCE);
                return characterResourceCollection != null && characterResourceCollection.size() >= minimalCharacterConfigured;
            }
        };
    }


    private Guard<States, Events> locationGard(boolean hasLocationSelected) {
        return new Guard<States, Events>() {
            @Override
            public boolean evaluate(StateContext<States, Events> context) {
                LabeledResource location = context.getExtendedState().get(ExtendedState.EPISODE_LOCATION, LabeledResource.class);
                return hasLocationSelected == (location != null);
            }
        };
    }

    @Bean
    public StateMachineListener<States, Events> listener(@Autowired EditEpisodeCommand editEpisodeCommand) {

        return new StateMachineListenerAdapter<States, Events>();
    }

    public enum ExtendedState {
        EPISODE_LOCATION_LABEL(String.class),
        EPISODE_LOCATION(LabeledResource.class),
        EPISODE_CHARACTER_APPEARANCE(Collection.class),
        EPISODE_NUMBER(Integer.class),
        EPISODE_BOOK(LabeledResource.class),
        PREVIOUS_EPISODE(LabeledResource.class),
        EPISODE_NAME_STR(String.class);

        public Class<?> getType() {
            return type;
        }

        private final Class<?> type;

        private ExtendedState(Class<?> type) {
            this.type = type;
        }
    }

    public enum States {
        INITIAL,
        NEW_EPISODE_CONFIGURATION_MENU,
        CONFIGURE_LOCATION, CONFIGURE_EPISODE_NAME, CONFIGURE_CHARACTER,
        CONFIGURE_BOOK,
        CONFIGURE_EPISODE_NUMBER,
        ASK_SAVE_ONTOLOGY,
        FINAL;


    }

    public enum Events {
        START_EPISODE_CREATION,
        START_CONFIGURE_LOCATION,
        FINISH_CONFIGURE_LOCATION,
        START_CONFIGURE_CHARACTER,
        FINISH_CONFIGURE_CHARACTER,
        FINISH_CONFIGURE_EPISODE_NAME,
        START_CONFIGURE_BOOK,
        FINISH_CONFIGURE_BOOK,
        START_CONFIGURE_EPISODE_NUMBER,
        FINISH_CONFIGURE_EPISODE_NUMBER,
        FINISH_EPISODE_CREATION,

        WORK_DONE,
        START_CONFIGURE_EPISODE_NAME;


    }

    ;


}
