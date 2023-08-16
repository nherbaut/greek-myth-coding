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
import top.nextnet.greekmythcoding.Utils;
import top.nextnet.greekmythcoding.cmd.LabeledCharacterAppearance;


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
    public static final String WHO_IS_ATHEN_KING = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
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

    final OntClass CONCRETE_CHARACTER = ontologyModel.getOntClass(NS + "ConcreteCharacter");
    final OntClass CONCRETE_CONCEPT = ontologyModel.getOntClass(NS + "ConcreteConcept");
    final OntClass CONCRETE_LOCATION = ontologyModel.getOntClass(NS + "ConcreteLocation");


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

    public List<Integer> getExistingEpisodesNumberForBookList(LabeledResource book) {

        return asStream(ontologyModel.listStatements(new SimpleSelector(null, HAS_BOOK, book.resource())))
                .map(s -> s.getSubject().asResource().getProperty(HAS_EPISODE_NUMBER).getInt())
                .sorted()
                .collect(Collectors.toList());
    }

    public LabeledResource getEpisode(LabeledObject<Resource> book, Integer episodeNumber) {

        return asStream(ontologyModel.listStatements(new SimpleSelector(null, HAS_BOOK, book.resource())))
                .filter(statement -> episodeNumber == statement.getSubject().getProperty(HAS_EPISODE_NUMBER).getInt())
                .map(s -> new LabeledResource(s.getSubject().getProperty(RDFS.label).getString(), s.getSubject())).findFirst().orElse(LabeledResource.getDefault());


    }

    public LabeledResource getLocationForEpisode(LabeledResource episodeResource) {

        SelectBuilder sb = selectBuilderTemplate.clone();


        sb.addPrefix("owl", "http://www.w3.org/2002/07/owl#")
                .addPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                .addPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
                .addPrefix("", NS)
                .addVar("?location")

                .addWhere(episodeResource.resource(), ":has_location", "?l")
                .addWhere("?l", RDF.type, "?LocationType")
                .addWhere("?LocationType", RDF.type, "?location")
                .addWhere("?location", "rdfs:subClassOf*", CONCRETE_CONCEPT)
                .setDistinct(true);


        QuerySolutionMap initialBinding = new QuerySolutionMap();

        var queryExecution = QueryExecutionFactory.create(sb.build(), ontologyModel);
        try (QueryExecution qexec = QueryExecutionFactory.create(queryExecution.getQuery(), ontologyModel)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                RDFNode locationType = soln.get("?location");       // Get a result variable by name.
                return new LabeledResource(locationType.asResource().getProperty(RDFS.label).getString(), locationType.asResource());

            }
        }
        return null;
    }

    public Resource addEpisode(LabeledResource book, Integer episodeNumber, String episodeLabel) {
        Resource newEpisode = ontologyModel.createResource(book.resource().getURI() + "_" + String.format("%03d", episodeNumber));

        newEpisode.addProperty(RDF.type, EPISODE);
        newEpisode.addProperty(RDFS.label, episodeLabel);
        newEpisode.addProperty(RDF.type, OWL2.NamedIndividual);
        newEpisode.addProperty(HAS_BOOK, book.resource());
        newEpisode.addLiteral(HAS_EPISODE_NUMBER, ResourceFactory.createTypedLiteral(episodeNumber));
        return newEpisode;
    }

    public void setLocationForEpisode(String location, Resource episode) {
        setLocationForEpisode(ontologyModel.createResource(location), episode);
    }

    public void setLocationForEpisode(Resource locationClass, Resource episode) {
        Resource newLocation = ontologyModel.createResource(NS + episode.getLocalName() + "_" + locationClass.getLocalName());
        newLocation.addProperty(RDF.type, locationClass);
        newLocation.addProperty(RDFS.label, String.format("%s, où se déroule l'épisode %s", locationClass.getLocalName(), getLabel(episode)));
        ontologyModel.add(episode, HAS_LOCATION, newLocation);
    }

    public Collection<LabeledResource> getAllLocations() {
        return getPunnedResources(LOCATION);

    }

    public Collection<LabeledResource> getAllCharacter() {
        return getPunnedResources(CHARACTER);
    }

    public Collection<LabeledResource> getPunnedResources(OntClass klass) {
        SelectBuilder sb = selectBuilderTemplate.clone();
        sb.addVar("?x")
                .addWhere("?x", RDFS.subClassOf, klass)
                .addWhere("?x", RDF.type, OWL2.NamedIndividual)
                .addWhere("?x", RDF.type, OWL2.Class)
                .addWhere("?x", RDFS.subClassOf, CONCRETE_CONCEPT);

        return asStream(QueryExecutionFactory.create(sb.build(), ontologyModel).execSelect())
                .map(s -> s.get("x"))
                .filter(s -> s.asResource().getProperty(RDFS.label) != null)
                .map(s -> new LabeledResource(s.asResource().getProperty(RDFS.label).getString(), s.asResource()))
                .collect(Collectors.toList());

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

    public Collection<LabeledResource> getAllLocationTypes() {
        SelectBuilder sb = selectBuilderTemplate.clone();


        ExprFactory factory = new ExprFactory();

        sb.addVar("?x").setDistinct(true)
                .addWhere("?x", "rdfs:subClassOf*", LOCATION)
                .addWhere("?x", RDF.type, OWL2.NamedIndividual)
                .addWhere("?x", RDF.type, OWL2.Class)
                .addFilter(factory.notexists(selectBuilderTemplate.
                        addWhere("?x", "rdfs:subClassOf*", CONCRETE_CONCEPT)
                        .addWhere("?y", "rdfs:subClassOf", "?x")
                        .addWhere("?y", RDF.type, OWL2.Class)));
        QueryExecution qe = QueryExecutionFactory.create(sb.build(), ontologyModel);


        return asStream(qe.execSelect())
                .map(qr -> qr.get("x").asResource())
                .filter(r -> r.getProperty(RDFS.label) != null)
                .map(r -> new LabeledResource(r.getProperty(RDFS.label).getString(), r))
                .collect(Collectors.toList());


    }

    public Collection<LabeledCharacterAppearance> getCharactersIndividualsFromPreviousEpisode(String previousEpisode) {
        SelectBuilder sb = selectBuilderTemplate.clone();
        sb.addVar("?t").
                addVar("?c").
                addVar("?age_range").
                addVar("?role")
                .setDistinct(true)
                .addWhere(ontologyModel.getResource(previousEpisode), HAS_CHARACTER, "?c")
                .addWhere("?c", RDF.type, "?t")
                .addWhere("?c", HAS_AGE_RANGE, "?age_range")
                .addWhere("?c", HAS_ROLE, "?role")
                .addWhere("?t", RDF.type, "<https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#ConcreteCharacter>")
                .build();
        return asStream(QueryExecutionFactory.create(sb.build(), ontologyModel)
                .execSelect())
                .map(qs -> new LabeledCharacterAppearance(String.format("%s (%s, %s)", getLabel(qs.get("t")), getLabel(qs.get("age_range")), getLabel(qs.get("role"))), new CharacterAppearance.Builder().withCharacter(qs.get("t")).withAgeRange(qs.get("age_range")).withRole(qs.get("role")).build()))
                .collect(Collectors.toSet());

    }

    private static String getLabel(RDFNode res) {
        Statement labelProp = res.asResource().getProperty(RDFS.label);
        if (labelProp != null) {
            return labelProp.getString();
        } else {
            return "UNKNOWN";
        }
    }


    private static <T> Stream<T> asStream(Iterator<T> itr) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(itr, Spliterator.IMMUTABLE), false).collect(Collectors.toList()).stream();
    }

    public CharacterAppearance getCharacterAppearanceInEpisode(Resource character, Resource previousEpisode) {
        SelectBuilder sb = selectBuilderTemplate.clone();
        sb.addVar("?age_range")
                .addVar("?role")
                .addVar("?age_range")
                .addWhere("?character_episode", "rdf:type", character)
                .addWhere(previousEpisode, ":has_character", "?character_episode")
                .addWhere("?character_episode", HAS_AGE_RANGE, "?age_range")
                .addWhere("?character_episode", HAS_ROLE, "?role");
        var queryExecution = QueryExecutionFactory.create(sb.build(), ontologyModel);
        return asStream(queryExecution.execSelect())
                .map(qs ->
                        new CharacterAppearance(LabeledResource.fromRessource(character),
                                LabeledResource.fromRessource(qs.get("age_range").asResource()),
                                LabeledResource.fromRessource(qs.get("role").asResource())))
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


    public Resource addCharacterToEpisode(String newEpisodeResource, String character, String ageResourceStr, String roleResourceStr) {
        return addCharacterToEpisode(ontologyModel.getResource(newEpisodeResource),
                ontologyModel.getResource(character),
                ontologyModel.getResource(ageResourceStr),
                ontologyModel.getResource(roleResourceStr));
    }

    public Resource addCharacterToEpisode(Resource newEpisodeResource, Resource character, Resource ageResourceStr, Resource roleResourceStr) {

        Resource newCharacter = ontologyModel.createResource(newEpisodeResource.getURI() + "_" + character.getLocalName());

        newCharacter.addProperty(HAS_AGE_RANGE, ageResourceStr);
        newCharacter.addProperty(HAS_ROLE, roleResourceStr);
        String appearangeLabel = "apparition de " + character.getProperty(RDFS.label).getString() + " (" + getLabel(ageResourceStr) + ") en tant que " + getLabel(roleResourceStr) + " dans l'épisode " + getLabel(newEpisodeResource);
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
        OntClass newChar = ontologyModel.createClass(NS + characterLabel.replace(" ", "_"));
        newChar.addProperty(RDFS.label, characterLabel);
        return new LabeledResource(characterLabel, newChar);
    }

    public void setParentForCharacter(LabeledResource character, String parentResourceIRI) {

        ontologyModel.createStatement(character.resource(), HAS_PARENT, ontologyModel.getOntClass(parentResourceIRI));
    }

    public Collection<String> getAllTitlesToken() {
        return asStream(ontologyModel.listStatements(null, RDF.type, EPISODE))
                .filter(statement -> statement.getSubject().getProperty(RDFS.label) != null)
                .map(LabeledResource::fromStatementSubject)
                .map(lr -> Arrays.stream(lr.label().split(" ")))
                .flatMap(Function.identity())
                .collect(Collectors.toSet());
    }

    private Collection<LabeledResource> getSuclassesOf(OntClass klass) {
        return asStream(klass.listSubClasses(true))
                .filter(k -> k.getProperty(RDFS.label) != null)
                .map(k -> new LabeledResource(k.getLocalName(), k)).collect(Collectors.toList());
    }

    public Collection<LabeledResource> getCharacterTypes() {
        return getSuclassesOf(CHARACTER);
    }

    public Collection<LabeledResource> getSubClass(Resource resource) {
        return getSuclassesOf(ontologyModel.getOntClass(resource.getURI()));
    }

    public void setType(Resource subject, Resource object) {
        ontologyModel.getOntClass(subject.getURI()).setSuperClass(ontologyModel.getOntClass(object.getURI()));
    }


    public Collection<LabeledResource> getSelectableCharacterTypes() {

        SelectBuilder sb = selectBuilderTemplate.clone();
        sb.addVar("?s")
                .addWhere("?s", "rdf:type", ":SelectableCharacter");

        var queryExecution = QueryExecutionFactory.create(sb.build(), ontologyModel);
        try (QueryExecution qexec = QueryExecutionFactory.create(queryExecution.getQuery(), ontologyModel)) {
            return asStream(qexec.execSelect())
                    .map(smt -> ontologyModel.getOntClass(smt.get("?s").asResource().getURI()))
                    .filter(k -> k != null && k.getLabel(null) != null)
                    .map(LabeledResource::fromClass)
                    .collect(Collectors.toList());
        }


    }

    public void setConcreteCharacter(Resource resource) {
        ontologyModel.add(resource, RDFS.subClassOf, CONCRETE_CHARACTER);
    }

    public LabeledResource createNewLocationClass(String locationLabel, LabeledResource locationType) {

        OntClass klass = ontologyModel.createClass(String.format("%s_%s", locationType.resource().getURI(), Utils.sanitizeURI(locationLabel)));
        ontologyModel.add(klass, RDF.type, locationType.resource());
        ontologyModel.add(klass, RDF.type, CONCRETE_LOCATION);

        return new LabeledResource(String.format("le lieu de %s (%s)", locationLabel, getLabel(locationType.resource())), klass.asResource());
    }

    public <T extends LabeledObject> LabeledResource getCharacterClassFromIndividual(T character) {
        SelectBuilder sb = selectBuilderTemplate.clone();
        sb.addVar("?t")
                .setDistinct(true)
                .addWhere(character.resource(), RDF.type, "?t")
                .addWhere("?t", RDF.type, "<https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#ConcreteCharacter>")
                .build();
        return asStream(QueryExecutionFactory.create(sb.build(), ontologyModel)
                .execSelect()).map(r -> LabeledResource.fromRessource(r.get("t").asResource())).findAny().orElse(LabeledResource.getDefault());

    }
}
