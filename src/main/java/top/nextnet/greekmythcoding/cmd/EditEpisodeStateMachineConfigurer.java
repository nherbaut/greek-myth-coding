package top.nextnet.greekmythcoding.cmd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import reactor.core.publisher.Mono;
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
                    getCommand(context).askUserForEpisodeLocation(context);
                })
                .state(States.CONFIGURE_CHARACTER)
                .state(States.FINAL);


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
                .guard(locationGard(false))
                .and()
                .withInternal()
                .source(States.CONFIGURE_LOCATION)
                .event(Events.NEW_LOCATION_CLASS_ENTERED)
                .action((context) -> {
                    getCommand(context).createNewLocationClass(context);
                    context.getStateMachine().sendEvent(Mono.just(new GenericMessage(Events.FINISH_CONFIGURE_LOCATION)));
                })
                .and()
                .withExternal()
                .source(States.CONFIGURE_LOCATION)
                .target(States.NEW_EPISODE_CONFIGURATION_MENU)
                .event(Events.FINISH_CONFIGURE_LOCATION)
                .action((context) -> getCommand(context).saveNewLocation(context))
                .and()
                .withExternal()
                .source(States.NEW_EPISODE_CONFIGURATION_MENU)
                .target(States.CONFIGURE_CHARACTER)
                .event(Events.START_CONFIGURE_CHARACTER)
                .and()
                .withExternal()
                .source(States.NEW_EPISODE_CONFIGURATION_MENU)
                .target(States.FINAL)
                .event(Events.FINISH_EPISODE_CREATION)
                .guard(CompositeGuard.combine(locationGard(true), characterGard(1)));


    }

    private Guard<States, Events> characterGard(int minimalCharacterConfigured) {
        return new Guard<States, Events>() {
            @Override
            public boolean evaluate(StateContext<States, Events> context) {
                Collection characterResourceCollection = (Collection) context.getExtendedState().getVariables().get(ExtendedState.CHARACTER_RESOURCE_COLLECTION);
                return characterResourceCollection.size() >= minimalCharacterConfigured;
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
        EPISODE_LOCATION_LABEL,
        EPISODE_LOCATION, CHARACTER_RESOURCE_COLLECTION;
    }

    public enum States {
        INITIAL,
        NEW_EPISODE_CONFIGURATION_MENU,
        CONFIGURE_LOCATION,
        CONFIGURE_CHARACTER,
        FINAL;


    }

    public enum Events {
        START_EPISODE_CREATION,
        START_CONFIGURE_LOCATION,
        FINISH_CONFIGURE_LOCATION,
        START_CONFIGURE_CHARACTER,
        FINISH_CONFIGURE_CHARACTER,
        FINISH_EPISODE_CREATION,
        NEW_LOCATION_CLASS_ENTERED;


    }

    ;


}
