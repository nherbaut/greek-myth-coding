package top.nextnet.greekmythcoding.onto;

import org.apache.jena.rdf.model.RDFNode;

public record CharacterAppearance(LabeledResource character, LabeledResource ageRange, LabeledResource role) {
    public static CharacterAppearance getDefault() {
        return new CharacterAppearance(LabeledResource.getDefault(),
                LabeledResource.getDefault(), LabeledResource.getDefault());
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    public static class Builder {
        private LabeledResource role;
        private LabeledResource character;
        private LabeledResource ageRange;

        private Builder() {
        }

        public Builder withCharacter(RDFNode res) {

            this.character = LabeledResource.fromRessource(res.asResource());
            return this;
        }

        public Builder withAgeRange(RDFNode res) {
            this.ageRange = LabeledResource.fromRessource(res.asResource());
            return this;
        }

        public Builder withRole(RDFNode res) {
            this.role = LabeledResource.fromRessource(res.asResource());
            return this;
        }


        public CharacterAppearance build() {
            return new CharacterAppearance(this.character, this.ageRange, this.role);
        }
    }
}
