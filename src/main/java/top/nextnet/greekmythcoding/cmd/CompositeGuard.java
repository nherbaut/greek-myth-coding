package top.nextnet.greekmythcoding.cmd;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class CompositeGuard<S, E> implements Guard<S, E> {
    private Collection<Guard> guards = new ArrayList<>();

    private CompositeGuard(Guard<S, E>... g) {
        guards.addAll(Arrays.asList(g));
    }

    public static <S, E> Guard<S, E> combine(Guard<S, E>... g) {
        return new CompositeGuard<>(g);
    }


    @Override
    public boolean evaluate(StateContext<S, E> context) {
        return !guards.stream().map(g -> g.evaluate(context)).filter(e -> e == false).findAny().isPresent();
    }
}
