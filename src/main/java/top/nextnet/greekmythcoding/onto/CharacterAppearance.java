package top.nextnet.greekmythcoding.onto;

public record CharacterAppearance(LabeledResource ageRange, LabeledResource role) {
    public static CharacterAppearance getDefault(){
        return new CharacterAppearance(new LabeledResource("", null), new LabeledResource("", null));
    }
}
