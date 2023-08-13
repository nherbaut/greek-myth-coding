package top.nextnet.greekmythcoding.onto;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;

public record LabeledResource(String label, Resource resource) {
    public String resourceAsStr() {
        return resource().toString();
    }

    public static LabeledResource fromRessource(Resource resource) {
        return new LabeledResource(resource.getProperty(RDFS.label).getString(), resource);
    }

    public static LabeledResource fromStatementSubject(Statement stmt) {
        return new LabeledResource(stmt.getSubject().getProperty(RDFS.label).getString(), stmt.getSubject().asResource());
    }

    public static LabeledResource fromStatementObject(Statement stmt) {
        return new LabeledResource(stmt.getObject().asResource().getProperty(RDFS.label).getString(), stmt.getObject().asResource());
    }

    public static LabeledResource fromClass(OntClass ontClass) {
        return new LabeledResource(ontClass.getLabel(null), ontClass);
    }
}
