package top.nextnet.greekmythcoding.onto;

import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.PrintUtil;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
    protected static final OntModel ontologyModel;
    private static final SelectBuilder selectBuilderTemplate;
    private static final String NS = "https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#";

    static {
        ontologyModel = ModelFactory.createOntologyModel();
        try (InputStream is = OntoFacade.class.getClassLoader().getResourceAsStream("owl/greek-mythology-stories.owl")) {
            ontologyModel.read(is, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        selectBuilderTemplate = new SelectBuilder();
        selectBuilderTemplate.addPrefix("owl", "http://www.w3.org/2002/07/owl#")
                .addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                .addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
                .addPrefix("", NS);
    }

    final OntClass EPISODE = ontologyModel.getOntClass(NS + "Episode");
    final Property HAS_BOOK = ontologyModel.getOntProperty(NS + "has_book");
    final Property HAS_EPISODE_NUMBER = ontologyModel.getOntProperty(NS + "episode_number");
    final Property HAS_PARENT = ontologyModel.getOntProperty(NS + "has_parent");

    final Property HAS_LOCATION = ontologyModel.getOntProperty(NS + "has_location");
    final OntClass LOCATION = ontologyModel.getOntClass(NS + "Location");
    final OntClass CHARACTER = ontologyModel.getOntClass(NS + "Character");
    final Property HAS_CHARACTER = ontologyModel.getOntProperty(NS + "has_character");
    final Property HAS_AGE_RANGE = ontologyModel.getOntProperty(NS + "has_AgeRange");
    final Property HAS_ROLE = ontologyModel.getOntProperty(NS + "has_role");
    final OntClass AGE_RANGE = ontologyModel.getOntClass(NS + "AgeRange");
    final Property AGE_RANGE_RANK = ontologyModel.getProperty(NS + "has_age_range_rank");
    final OntClass ROLE = ontologyModel.getOntClass(NS + "Role");

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
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                RDFNode label = soln.get(labelID);       // Get a result variable by name.
                RDFNode resource = soln.get(resourceID);
                res.add(new LabeledResource(label.asLiteral().toString(), resource.asResource()));
            }
        }
        return res;
    }

    public List<Integer> getExistingEpisodesNumberForBookList(String bookResource) {

        return asStream(ontologyModel.listStatements(new SimpleSelector(null, HAS_BOOK, ontologyModel.getResource(bookResource))))
                .map(s -> s.getSubject().asResource().getProperty(HAS_EPISODE_NUMBER).getInt())
                .sorted()
                .collect(Collectors.toList());
    }

    public LabeledResource getEpisode(String book, Integer episodeNumber) {

        return asStream(ontologyModel.listStatements(new SimpleSelector(null, HAS_BOOK, ontologyModel.getResource(book))))
                .filter(statement -> episodeNumber == statement.getSubject().getProperty(HAS_EPISODE_NUMBER).getInt())
                .map(s -> new LabeledResource(s.getSubject().getProperty(RDFS.label).getString(), s.getSubject())).findFirst().orElse(null);


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
                .addWhere(ontologyModel.getResource(episodeResource), ":has_location", "?l")

                .addWhere("?l", "rdf:type", "?LocationType")
                .addWhere("?LocationType", "rdfs:label", "?_label")
                .setDistinct(true);


        QuerySolutionMap initialBinding = new QuerySolutionMap();

        var queryExecution = QueryExecutionFactory.create(sb.build(), ontologyModel);
        try (QueryExecution qexec = QueryExecutionFactory.create(queryExecution.getQuery(), ontologyModel)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                RDFNode locationType = soln.get("?LocationType");       // Get a result variable by name.
                return new LabeledResource(soln.get("_label").asLiteral().getString(), locationType.asResource());

            }
        }
        return null;
    }

    public Resource addEpisode(String bookresource, Integer episodeNumber, String episodeLabel) {
        Resource newEpisode = ontologyModel.createResource(bookresource + "_" + String.format("%02d", episodeNumber));

        newEpisode.addProperty(RDF.type, EPISODE);
        newEpisode.addProperty(RDFS.label, episodeLabel);
        newEpisode.addProperty(RDF.type, OWL2.NamedIndividual);
        newEpisode.addProperty(HAS_BOOK, ResourceFactory.createResource(bookresource));
        newEpisode.addLiteral(HAS_EPISODE_NUMBER, ResourceFactory.createTypedLiteral(episodeNumber));
        return newEpisode;
    }

    public void setLocationForEpisode(String location, Resource episode) {
        setLocationForEpisode(ontologyModel.createResource(location), episode);
    }

    public void setLocationForEpisode(Resource locationClass, Resource episode) {
        Resource newLocation = ontologyModel.createResource(NS + episode.getLocalName() + "_" + locationClass.getLocalName());
        newLocation.addProperty(RDF.type, locationClass);
        ontologyModel.add(episode, HAS_LOCATION, newLocation);
    }

    public List<LabeledResource> getAllLocations() {
        return null;
    }

    public void dumpRDFXMLtoConsole() {
        ontologyModel.write(new PrintStream(System.out));
    }

    public void dumpRDFXMLToFile() {
        try {
            ontologyModel.write(new FileWriter("output.owl"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<LabeledResource> getCharactersFromPreviousEpisode(String previousEpisode) {
        Resource previousEpisodeResource = ontologyModel.getOntResource(previousEpisode);

        return asStream(previousEpisodeResource.listProperties(HAS_CHARACTER))
                .map(stm -> stm.getObject().asResource())
                .map(r -> asStream(r.listProperties(RDF.type)))
                .flatMap(Function.identity())
                .filter(s -> !s.getObject().equals(OWL2.NamedIndividual))//individuals have 2 types (NI and the Class) we take only the ontology-defined class
                .filter(s -> s.getObject().asResource().getProperty(RDFS.label) != null)
                .map(s -> new LabeledResource(s.getObject().asResource().getProperty(RDFS.label).getLiteral().getString(), s.getObject().asResource()))
                .collect(Collectors.toList());


    }

    private static <T> Stream<T> asStream(Iterator<T> itr) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(itr, Spliterator.IMMUTABLE), false).collect(Collectors.toList()).stream();
    }

    public CharacterAppearance getCharacterAppearanceInEpisode(Resource character, Resource previousEpisode) {
        SelectBuilder sb = selectBuilderTemplate.clone();
        sb.addVar("?age_range")
                .addVar("?role")
                .addWhere("?character_episode", "rdf:type", character)
                .addWhere(previousEpisode, ":has_character", "?character_episode")
                .addWhere("?character_episode", HAS_AGE_RANGE, "?age_range")
                .addWhere("?character_episode", HAS_ROLE, "?role");
        var queryExecution = QueryExecutionFactory.create(sb.build(), ontologyModel);
        return asStream(queryExecution.execSelect())
                .map(qs -> new CharacterAppearance(new LabeledResource(qs.get("age_range").asResource().getProperty(RDFS.label).getString(), qs.get("age_range").asResource()), new LabeledResource(qs.get("role").asResource().getProperty(RDFS.label).getString(), qs.get("role").asResource())))
                .findFirst().orElseThrow(() -> new RuntimeException("No Such Character " + character + "in Episode " + previousEpisode.toString()));
    }

    public List<LabeledResource> getAgeRanges() {
        SelectBuilder sb = selectBuilderTemplate.clone();
        sb.addVar("?age_range_item")
                .addWhere("?age_range_item", RDF.type, AGE_RANGE)
                .addWhere("?age_range_item", RDF.type, OWL2.NamedIndividual);
        var queryExecution = QueryExecutionFactory.create(sb.build(), ontologyModel);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(queryExecution.execSelect(), Spliterator.IMMUTABLE), false)
                .sorted(new Comparator<QuerySolution>() {
                    @Override
                    public int compare(QuerySolution t0, QuerySolution t1) {
                        Integer t0Rank = t0.get("age_range_item").asResource().getProperty(AGE_RANGE_RANK).getInt();
                        Integer t1Rank = t1.get("age_range_item").asResource().getProperty(AGE_RANGE_RANK).getInt();
                        return t0Rank.compareTo(t1Rank);
                    }
                })
                .map(qs -> new LabeledResource(qs.get("age_range_item").asResource().getProperty(RDFS.label).getLiteral().getString(), qs.get("age_range_item").asResource()))
                .collect(Collectors.toList());
    }

    public List<LabeledResource> getRoleRange() {
        SelectBuilder sb = selectBuilderTemplate.clone();
        sb.addVar("?role_item")
                .addWhere("?role_item", RDF.type, ROLE)
                .addWhere("?role_item", RDF.type, OWL2.NamedIndividual);
        var queryExecution = QueryExecutionFactory.create(sb.build(), ontologyModel);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(queryExecution.execSelect(), Spliterator.IMMUTABLE), false)
                .sorted(new Comparator<QuerySolution>() {
                    @Override
                    public int compare(QuerySolution t0, QuerySolution t1) {
                        String roleDescr0 = t0.get("role_item").asResource().getProperty(RDFS.label).getLiteral().toString();
                        String roleDescr1 = t1.get("role_item").asResource().getProperty(RDFS.label).getLiteral().toString();
                        return roleDescr0.compareTo(roleDescr1);
                    }
                })
                .map(qs -> new LabeledResource(qs.get("role_item").asResource().getProperty(RDFS.label).getLiteral().getString(), qs.get("role_item").asResource()))
                .collect(Collectors.toList());
    }


    public Resource addCharacterToEpisode(Resource newEpisodeResource, Resource character, String ageResourceStr, String roleResourceStr) {

        Resource newCharacter = ontologyModel.createResource(newEpisodeResource.getURI() + "_" + character.getLocalName());

        newCharacter.addProperty(HAS_AGE_RANGE, ontologyModel.getResource(ageResourceStr));
        newCharacter.addProperty(HAS_ROLE, ontologyModel.getResource(roleResourceStr));
        String appearangeLabel = "apparition de " + character.getProperty(RDFS.label).getString() + " (" + ontologyModel.getResource(ageResourceStr).getProperty(RDFS.label).getString() + ") en tant que " + ontologyModel.getResource(roleResourceStr).getProperty(RDFS.label).getString() + " dans l'épisode " + newEpisodeResource.getProperty(RDFS.label).getString();
        newCharacter.addProperty(RDFS.label, appearangeLabel);
        newCharacter.addProperty(RDF.type, character);
        newEpisodeResource.addProperty(HAS_CHARACTER, newCharacter);
        return newCharacter;
    }

    public void save() {
        Path originalFile = Path.of("/home/nherbaut/Desktop/greek-myth-coding/src/main/resources/owl/greek-mythology-stories.owl");
        Path backupPath = Path.of(FileNameUtils.getBaseName(originalFile) + "_" + System.currentTimeMillis() + "." + FileNameUtils.getExtension(originalFile));
        try {
            Files.copy(originalFile, backupPath, StandardCopyOption.COPY_ATTRIBUTES);
            try (Writer writer = new FileWriter(originalFile.toFile())) {
                ontologyModel.write(writer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<LabeledResource> getAllCharacters() {
        return asStream(ontologyModel.listStatements(null, RDFS.subClassOf, CHARACTER)).filter(stmt -> stmt.getSubject().getProperty(RDFS.label) != null).map(LabeledResource::fromStatementSubject).collect(Collectors.toList());
    }

    public LabeledResource createNewCharacter(String characterLabel) {
        Resource newChar = ontologyModel.createResource(NS+characterLabel.replace(" ", "_"));
        newChar.addProperty(RDF.type, CHARACTER);
        newChar.addProperty(RDFS.label, characterLabel);
        return new LabeledResource(characterLabel, newChar);
    }

    public void setParentForCharacter(LabeledResource character, String parentResourceIRI) {

        ontologyModel.createStatement(character.resource(), HAS_PARENT, ontologyModel.getOntClass(parentResourceIRI));
    }
}
