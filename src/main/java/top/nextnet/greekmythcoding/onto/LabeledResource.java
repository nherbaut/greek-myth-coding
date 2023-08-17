package top.nextnet.greekmythcoding.onto;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;

import static top.nextnet.greekmythcoding.Utils.sanitizeURI;

public record LabeledResource(String label,
                              Resource resource) implements LabeledObject<Resource>, Comparable<LabeledResource> {

    private static String defaultNS = "";

    public static void setDefaultNS(String NS) {
        defaultNS = NS;
    }


    public static LabeledResource fromString(String label) {
        return new LabeledResource(label, ResourceFactory.createResource(sanitizeURI(defaultNS + label)));
    }


    public static LabeledResource fromRessource(Resource resource) {
        return new LabeledResource(resource.getProperty(RDFS.label).getString(), resource);
    }

    public static LabeledResource fromStatementSubject(Statement stmt) {
        try {
            return new LabeledResource(stmt.getSubject().getProperty(RDFS.label).getString(), stmt.getSubject().asResource());
        }
        catch (Throwable e){
            System.out.println("issue with statement " + stmt.toString());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static LabeledResource fromStatementObject(Statement stmt) {
        return new LabeledResource(stmt.getObject().asResource().getProperty(RDFS.label).getString(), stmt.getObject().asResource());
    }

    public static LabeledResource fromClass(OntClass ontClass) {
        return new LabeledResource(ontClass.getLabel(null), ontClass);
    }


    @Override
    public int compareTo(LabeledResource tLabeledResource) {
        return this.label.compareTo(tLabeledResource.label);
    }

    public static LabeledResource getDefault() {
        return new LabeledResource("", null);
    }

    @Override
    public String toString() {
        return label();
    }
}
