package top.nextnet.greekmythcoding.onto;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.PrintUtil;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Hello world!
 */
@Component
public class OntoFacade {

    public static final String GET_ALL_BOOKS = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "prefix :     <https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#>\n" +
            "SELECT ?s ?label WHERE{\n" +
            "?s rdf:type owl:NamedIndividual.\n" +
            "?s rdf:type :Book.\n" +
            "?s rdfs:label ?label.\n" +
            "\n" +
            "}";

    public static final String WHO_IS_ATHEN_KING = " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
            "prefix :     <https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#>\n" +
            "SELECT DISTINCT  ?king\n" +
            "WHERE    {?episode_location rdf:type :Athene.\n" +
            "?episode_location :has_king ?aking.\n" +
            "?episode :has_location ?episode_location.\n" +
            "?episode :episode_number ?_episode_number.\n" +
            "?aking rdf:type ?king.\n" +
            "?episode :has_book ?book.\n" +
            "?king rdfs:subClassOf :NamedHuman.\n" +
            "bind( str(?_episode_number) as ?episode_number ).\n" +
            "FILTER(?episode_number=\"1\")\n" +
            "FILTER(?book=:FT)\n" +
            "} ";
    private static final OntModel ontologyModel;

    static {
        ontologyModel = ModelFactory.createOntologyModel();
        try (InputStream is = OntoFacade.class.getClassLoader().getResourceAsStream("owl/greek-mythology-stories.owl")) {
            ontologyModel.read(is, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String NS = "https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#";

    private static Resource getResource(String name) {
        return ontologyModel.createResource(NS + name);
    }

    private static Property getProperty(String name) {
        return ontologyModel.createObjectProperty(NS + name);
    }

    private static void printStatements(Model m, Resource s, Property p, Resource o) {
        for (StmtIterator i = m.listStatements(s, p, o); i.hasNext(); ) {
            Statement stmt = i.nextStatement();
            System.out.println(" - " + PrintUtil.print(stmt));
        }
    }


    public List<LabeledResource> getBooks() {


        Ontology ontology = ontologyModel.getOntology("https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0");
        Query query = QueryFactory.create(GET_ALL_BOOKS);
        List<LabeledResource> res = getLabeledResources(query, "label", "s");
        return res;
    }

    private static List<LabeledResource> getLabeledResources(Query query, String labelID, String resourceID) {
        List<LabeledResource> res = new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontologyModel)) {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext(); ) {
                QuerySolution soln = results.nextSolution();
                RDFNode label = soln.get(labelID);       // Get a result variable by name.
                RDFNode resource = soln.get(resourceID);
                res.add(new LabeledResource(label.asLiteral().toString(), resource.asResource()));
            }
        }
        return res;
    }


    public List<Integer> getExistingEpisodesForBookList(String bookResource) {

        String GET_EPISODE_NUMBERS_FROM_BOOKS = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "prefix :     <https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#>\n" +
                "SELECT distinct ?s  ?episode_number WHERE{\n" +
                "?s rdf:type owl:NamedIndividual.\n" +
                "?s rdf:type :Episode.\n" +
                "?s :has_book ?book_resource.\n" +
                "?s :episode_number ?_episode_number.\n" +
                "bind( str(?_episode_number) as ?episode_number)\n" +
                "}";

        QuerySolutionMap initialBinding = new QuerySolutionMap();
        ;
        initialBinding.add("book_resource", ResourceFactory.createResource(bookResource));
        var queryExecution = QueryExecutionFactory.create(GET_EPISODE_NUMBERS_FROM_BOOKS, ontologyModel, initialBinding);
        List<Integer> res = new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionFactory.create(queryExecution.getQuery(), ontologyModel)) {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext(); ) {
                QuerySolution soln = results.nextSolution();
                RDFNode episodeNumber = soln.get("episode_number");       // Get a result variable by name.

                res.add(Integer.parseInt(episodeNumber.asLiteral().getString()));
            }
        }
        return res;
    }

    public LabeledResource getLocationForEpisode(String episodeResource) {

        SelectBuilder sb = new SelectBuilder();

        ExprFactory exprF = sb.getExprFactory();


        sb.addPrefix("owl", "http://www.w3.org/2002/07/owl#")
                .addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                .addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
                .addPrefix("", NS)
                .addVar("?LocationType")
                .addVar("?_label")
                .addWhere(ResourceFactory.createResource(episodeResource), ":has_location", "?l")

                .addWhere("?l", "rdf:type", "?LocationType")
                .addWhere("?LocationType", "rdfs:label", "?_label")
                .setDistinct(true);


        QuerySolutionMap initialBinding = new QuerySolutionMap();
        ;

        var queryExecution = QueryExecutionFactory.create(sb.build(), ontologyModel);
        try (QueryExecution qexec = QueryExecutionFactory.create(queryExecution.getQuery(), ontologyModel)) {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext(); ) {
                QuerySolution soln = results.nextSolution();
                RDFNode locationType = soln.get("?LocationType");       // Get a result variable by name.
                return new LabeledResource(soln.get("_label").asLiteral().getString(), locationType.asResource());

            }
        }
        return null;
    }

    public LabeledResource getEpisodeFromBookAndEpisodeNumber(String bookResource, Integer previousEpisodeNumber) {

        SelectBuilder sb = new SelectBuilder();

        ExprFactory exprF = sb.getExprFactory();


        sb.addPrefix("owl", "http://www.w3.org/2002/07/owl#")
                .addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                .addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
                .addPrefix("", NS)
                .addVar("?location")
                .addVar("?label")
                .addWhere("?location", "rdfs:subClassOf*", LOCATION)
                .addWhere("?location", "rdfs:label", "?label")
                .addWhere("?individual", "rdf:type", "owl:NamedIndividual")
                .addWhere("?individual", "rdf:type", "?location")
                .setDistinct(true);


        String QUERY_STR = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "prefix :     <https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#>\n" +
                "SELECT distinct ?episode ?episode_label WHERE{\n" +
                "?episode rdf:type owl:NamedIndividual.\n" +
                "?episode :episode_number " + previousEpisodeNumber + " .\n" +
                "?episode :has_book :" + ResourceFactory.createResource(bookResource).getLocalName() + ".\n" +
                "?episode rdfs:label ?episode_label.\n" +
                "\n" +
                "}";

        QuerySolutionMap initialBinding = new QuerySolutionMap();
        ;

        var queryExecution = QueryExecutionFactory.create(QUERY_STR, ontologyModel, initialBinding);
        try (QueryExecution qexec = QueryExecutionFactory.create(queryExecution.getQuery(), ontologyModel)) {
            ResultSet results = qexec.execSelect();
            for (; results.hasNext(); ) {
                QuerySolution soln = results.nextSolution();
                RDFNode rdfNode = soln.get("episode");       // Get a result variable by name.
                return new LabeledResource(soln.get("episode_label").asLiteral().getString(), rdfNode.asResource());

            }
        }
        return null;
    }

    final Resource EPISODE = ontologyModel.createResource(NS + "Episode");
    final Property HAS_BOOK = ontologyModel.createProperty(NS + "has_book");
    final Property HAS_LOCATION = ontologyModel.createProperty(NS + "has_location");
    final Property LOCATION = ontologyModel.createProperty(NS + "Location");

    public Resource addEpisode(String bookresource, Integer episodeNumber, String episodeLabel) {
        Resource newEpisode = ResourceFactory.createResource(bookresource + "_" + String.format("%02d", episodeNumber));
        Individual i = ontologyModel.createIndividual(NS + newEpisode.getLocalName(), newEpisode);
        i.addRDFType(EPISODE);
        i.addProperty(RDFS.label, episodeLabel);
        i.addProperty(HAS_BOOK, ResourceFactory.createResource(bookresource));
        return newEpisode;
    }

    public void setLocationForEpisode(Resource location, Resource episode) {
        ontologyModel.add(episode, HAS_LOCATION, location);
    }

    public List<LabeledResource> getAllLocations() {


        SelectBuilder sb = new SelectBuilder();

        ExprFactory exprF = sb.getExprFactory();


        sb.addPrefix("owl", "http://www.w3.org/2002/07/owl#")
                .addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                .addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
                .addPrefix("", NS)
                .addVar("?location")
                .addVar("?label")
                .addWhere("?location", "rdfs:subClassOf*", LOCATION)
                .addWhere("?location", "rdfs:label", "?label")
                .addWhere("?individual", "rdf:type", "owl:NamedIndividual")
                .addWhere("?individual", "rdf:type", "?location")
                .setDistinct(true);


        Query q = sb.build();
        return this.getLabeledResources(q, "label", "location");
    }


}
