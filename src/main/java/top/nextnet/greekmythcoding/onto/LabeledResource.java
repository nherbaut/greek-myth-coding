package top.nextnet.greekmythcoding.onto;

import org.apache.jena.rdf.model.Resource;

public record LabeledResource(String label, Resource resource) {
    public String resourceAsStr(){
        return resource().toString();
    }

}
