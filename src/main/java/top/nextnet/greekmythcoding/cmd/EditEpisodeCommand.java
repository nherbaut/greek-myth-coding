package top.nextnet.greekmythcoding.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.shell.command.annotation.Command;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import top.nextnet.greekmythcoding.onto.LabeledResource;

import java.io.IOException;
import java.util.stream.Collectors;

@Command(command = "edit")
@Component
public class EditEpisodeCommand extends AbstractShellComponentImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditEpisodeCommand.class);


    @Autowired
    StateMachineFactory<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> factory;
    StateMachine<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> stateMachine;



    @Command(command = "episode", group = "Components", description = "crée un nouvel épisode")
    public void episodeSelectionFlow() {
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

        System.out.println("current thread inepisodeSelectionFlow " + Thread.currentThread().getId());

    }

    public void commandStarted(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context) {
        System.out.println("current thread in commandStarted" + Thread.currentThread().getId());
        var nextStates = getLegalEventsForCurrentState(context).stream().map(LabeledEnum::new).collect(Collectors.toSet());
        if (nextStates.size() == 1) {
            stateMachine.sendEvent(EditEpisodeStateMachineConfigurer.Events.valueOf(nextStates.iterator().next().label()));
        } else if (nextStates.size() >= 1) {
            LabeledEnum e = simpleSingleMatchFromList2("what to do next?", nextStates);
            stateMachine.sendEvent(EditEpisodeStateMachineConfigurer.Events.valueOf(e.label()));
        } else {
            stateMachine.sendEvent(EditEpisodeStateMachineConfigurer.Events.FINISH_EPISODE_CREATION);
        }

    }

    public void askUserForEpisodeLocation(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context) {

        LabeledResource input = simpleUserStringInputWithHints("où se déroule l'action?", ontoFacade.getAllLocations());

        if (input.resource() == null) {//new location
            context.getExtendedState().getVariables().put(EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_LOCATION_LABEL, input.label());
            context.getStateMachine().sendEvent(Mono.just(new GenericMessage<>(EditEpisodeStateMachineConfigurer.Events.NEW_LOCATION_CLASS_ENTERED)));
        } else {
            context.getExtendedState().getVariables().put(EditEpisodeStateMachineConfigurer.ExtendedState.EPISODE_LOCATION, input);
            context.getStateMachine().sendEvent(Mono.just(new GenericMessage<>(EditEpisodeStateMachineConfigurer.Events.FINISH_CONFIGURE_LOCATION)));

        }


    }

    public void saveNewLocation(StateContext<EditEpisodeStateMachineConfigurer.States, EditEpisodeStateMachineConfigurer.Events> context) {
    }
}
