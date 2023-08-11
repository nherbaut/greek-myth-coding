package top.nextnet.greekmythcoding.cmd;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.component.SingleItemSelector;
import org.springframework.shell.component.support.SelectorItem;
import org.springframework.shell.standard.AbstractShellComponent;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Command(command = "new")
public class EpisodeCommand extends AbstractShellComponent {

    @Command(command = "episode", group = "Components")
    public String singleSelector() {
        SelectorItem<String> i1 = SelectorItem.of("key1", "value1");
        SelectorItem<String> i2 = SelectorItem.of("key2", "value2");
        List<SelectorItem<String>> items = Arrays.asList(i1, i2);
        SingleItemSelector<String, SelectorItem<String>> component = new SingleItemSelector<>(getTerminal(),
                items, "testSimple", null);
        component.setResourceLoader(getResourceLoader());
        component.setTemplateExecutor(getTemplateExecutor());
        SingleItemSelector.SingleItemSelectorContext<String, SelectorItem<String>> context = component
                .run(SingleItemSelector.SingleItemSelectorContext.empty());
        String result = context.getResultItem().flatMap(si -> Optional.ofNullable(si.getItem())).get();
        return "Got value " + result;
    }
}

