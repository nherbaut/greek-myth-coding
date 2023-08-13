package top.nextnet.greekmythcoding.onto;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;

public record LabeledResource(String label, Resource resource) {
    public String resourceAsStr(){
        return resource().toString();
    }
    public static LabeledResource fromStatementSubject(Statement stmt){
        return new LabeledResource(stmt.getSubject().getProperty(RDFS.label).getString(),stmt.getSubject().asResource());
    }

}
