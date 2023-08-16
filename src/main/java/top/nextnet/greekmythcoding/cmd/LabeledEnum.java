package top.nextnet.greekmythcoding.cmd;

import top.nextnet.greekmythcoding.onto.LabeledObject;

public record LabeledEnum(Enum resource) implements LabeledObject<Enum> {

    @Override
    public String label() {
        return resource.name();
    }
}
