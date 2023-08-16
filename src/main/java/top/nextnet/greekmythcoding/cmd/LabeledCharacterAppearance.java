package top.nextnet.greekmythcoding.cmd;

import top.nextnet.greekmythcoding.onto.CharacterAppearance;
import top.nextnet.greekmythcoding.onto.LabeledObject;

public record LabeledCharacterAppearance(String label,
                                         CharacterAppearance resource) implements LabeledObject<CharacterAppearance> {


}
