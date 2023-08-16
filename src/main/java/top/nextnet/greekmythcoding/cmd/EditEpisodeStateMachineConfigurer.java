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

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory
public class EditEpisodeStateMachine
        extends EnumStateMachineConfigurerAdapter<EditEpisodeCommand.States, EditEpisodeCommand.Events> {


    public EditEpisodeCommand getEditEpisodeCommand() {
        return editEpisodeCommand;
    }

    public void setEditEpisodeCommand(EditEpisodeCommand editEpisodeCommand) {
        this.editEpisodeCommand = editEpisodeCommand;
    }

    EditEpisodeCommand editEpisodeCommand;
    @Override
    public void configure(StateMachineConfigurationConfigurer<EditEpisodeCommand.States, EditEpisodeCommand.Events> config)
            throws Exception {
        config
                .withConfiguration()
                .autoStartup(true);
    }

    @Override
    public void configure(StateMachineStateConfigurer<EditEpisodeCommand.States, EditEpisodeCommand.Events> states)
            throws Exception {
        states
                .withStates()
                .initial(EditEpisodeCommand.States.INITIAL)
                .states(EnumSet.allOf(EditEpisodeCommand.States.class));


    }


    @Override
    public void configure(StateMachineTransitionConfigurer<EditEpisodeCommand.States, EditEpisodeCommand.Events> transitions)
            throws Exception {
        transitions
                .withExternal()
                .source(EditEpisodeCommand.States.INITIAL)
                .target(EditEpisodeCommand.States.NEW_EPISODE_CONFIGURATION_MENU)
                .event(EditEpisodeCommand.Events.START_EPISODE_CREATION)
                .action((context)->{editEpisodeCommand.commandStarted(context);})
                .and()
                .withExternal()
                .source(EditEpisodeCommand.States.NEW_EPISODE_CONFIGURATION_MENU)
                .target(EditEpisodeCommand.States.CONFIGURE_LOCATION)
                .event(EditEpisodeCommand.Events.START_CONFIGURE_LOCATION)
                .action(new Action<EditEpisodeCommand.States, EditEpisodeCommand.Events>() {
                    @Override
                    public void execute(StateContext<EditEpisodeCommand.States, EditEpisodeCommand.Events> context) {
                        editEpisodeCommand.askUserForEpisodeLocation( context);
                    }
                })
                .guard(locationGard(false))
                .and()
                .withExternal()
                .source(EditEpisodeCommand.States.CONFIGURE_LOCATION)
                .target(EditEpisodeCommand.States.NEW_EPISODE_CONFIGURATION_MENU)
                .event(EditEpisodeCommand.Events.FINISH_CONFIGURE_LOCATION)
                .and()
                .withExternal()
                .source(EditEpisodeCommand.States.NEW_EPISODE_CONFIGURATION_MENU)
                .target(EditEpisodeCommand.States.CONFIGURE_CHARACTER)
                .event(EditEpisodeCommand.Events.START_CONFIGURE_CHARACTER)
                .and()
                .withExternal()
                .source(EditEpisodeCommand.States.NEW_EPISODE_CONFIGURATION_MENU)
                .target(EditEpisodeCommand.States.FINAL)
                .event(EditEpisodeCommand.Events.FINISH_EPISODE_CREATION)
                .guard(CompositeGuard.combine(locationGard(true), characterGard(1)));


    }

    private Guard<EditEpisodeCommand.States, EditEpisodeCommand.Events> characterGard(int minCharacter) {
        return new Guard<EditEpisodeCommand.States, EditEpisodeCommand.Events>() {
            @Override
            public boolean evaluate(StateContext<EditEpisodeCommand.States, EditEpisodeCommand.Events> context) {
                return context.getExtendedState().get(EditEpisodeCommand.ExtendedState.CHARACTER_COUNT, Integer.class) >= minCharacter;
            }
        };
    }

    private Guard<EditEpisodeCommand.States, EditEpisodeCommand.Events> locationGard(boolean hasLocationSelected) {
        return new Guard<EditEpisodeCommand.States, EditEpisodeCommand.Events>() {
            @Override
            public boolean evaluate(StateContext<EditEpisodeCommand.States, EditEpisodeCommand.Events> context) {
                String location = context.getExtendedState().get(EditEpisodeCommand.ExtendedState.LOCATION, String.class);
                return hasLocationSelected==(location != null && !location.isBlank());
            }
        };
    }

    @Bean
    public StateMachineListener<EditEpisodeCommand.States, EditEpisodeCommand.Events> listener(@Autowired EditEpisodeCommand editEpisodeCommand) {

        return new StateMachineListenerAdapter<EditEpisodeCommand.States, EditEpisodeCommand.Events>();
    }

    ;


}
