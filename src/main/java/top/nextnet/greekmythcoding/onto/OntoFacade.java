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
                .map(s -> s.getSubject().asResource().getProperty(HAS_EPISODE_NUMBER).getLiteral().getInt())
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
        return
                asStream(ontologyModel.listStatements(null, RDFS.subClassOf, LOCATION))
                        .map(qr -> ontologyModel.getOntClass(qr.getSubject().getURI()))
                        .filter(k -> getLabel(k) != null)
                        .filter(k -> !k.listSubClasses(true).hasNext())
                        .map(LabeledResource::fromRessource)
                        .collect(Collectors.toSet());

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

    public Collection<LabeledResource> getAllNotConcreteTypes(OntClass root) {
        SelectBuilder sb = selectBuilderTemplate.clone();


        ExprFactory factory = new ExprFactory();

        sb.addVar("?x").setDistinct(true)
                .addWhere("?x", "rdfs:subClassOf*", root)

                .addFilter(factory.notexists(selectBuilderTemplate.clone().
                        addWhere("?x", RDF.type, OWL2.NamedIndividual)));

        QueryExecution qe = QueryExecutionFactory.create(sb.build(), ontologyModel);


        return asStream(qe.execSelect())
                .map(qr -> qr.get("x").asResource())
                .filter(r -> r.getProperty(RDFS.label) != null)
                .map(r -> new LabeledResource(r.getProperty(RDFS.label).getString(), r))
                .collect(Collectors.toList());


    }

    public Collection<LabeledResource> getAllLocationTypes() {
        return getAllNotConcreteTypes(LOCATION);
    }

    public Collection<LabeledResource> getAllCharacterTypes() {

        String query = "PREFIX  :     <https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#>\n" +
                "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX  owl:  <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "\n" +
                "SELECT DISTINCT  ?t\n" +
                "WHERE {\n" +
                "?t rdfs:subClassOf* :Character.\n" +
                "FILTER NOT EXISTS  { \n" +
                "?t rdf:type owl:NamedIndividual.}\n" +
                "}";
        return asStream(QueryExecutionFactory.create(query, ontologyModel).execSelect()).map(r -> r.get("t").asResource()).filter(r -> getLabel(r) != null).map(LabeledResource::fromRessource).collect(Collectors.toSet());
    }

    public LabeledResource getLocationIndividualsFromPreviousEpisode(LabeledResource previousEpisode) {
        String query = "PREFIX  :     <https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#>\n" +
                "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX  owl:  <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "\n" +
                "SELECT DISTINCT  ?t\n" +
                "WHERE\n" +
                "  { :" + previousEpisode.resource().getLocalName() + "  :has_location  ?l .\n" +
                "    ?l      rdf:type        ?t .\n" +
                "}";


        return asStream(QueryExecutionFactory.create(query, ontologyModel).execSelect())
                .filter(qs -> !ontologyModel.getOntClass(qs.get("t").asResource().getURI()).listSubClasses(true).hasNext())
                .map(qs -> new LabeledResource(getLabel(qs.get("t")), qs.get("t").asResource()))
                .findAny().orElseThrow();
    }

    public Collection<LabeledCharacterAppearance> getCharactersIndividualsFromPreviousEpisode(LabeledResource previousEpisode) {
        String query = "PREFIX  :     <https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#>\n" +
                "PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX  owl:  <http://www.w3.org/2002/07/owl#>\n" +
                "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "\n" +
                "SELECT DISTINCT  ?t ?a ?r\n" +
                "WHERE\n" +
                "  { :" + previousEpisode.resource().getLocalName() + "  :has_character  ?c .\n" +
                "    ?c      rdf:type        ?t ;\n" +
                "            :has_AgeRange   ?a ;\n" +
                "            :has_role       ?r .\n" +
                "    ?t      rdf:type        owl:NamedIndividual." +

                "  }\n";
        return asStream(QueryExecutionFactory.create(query, ontologyModel).execSelect()).filter(qs -> !ontologyModel.getOntClass(qs.get("t").asResource().getURI()).listSubClasses(true).hasNext()).map(qs -> new LabeledCharacterAppearance(String.format("%s (%s, %s)", getLabel(qs.get("t")), getLabel(qs.get("a")), getLabel(qs.get("r"))), CharacterAppearance.getBuilder().withCharacter(qs.get("t")).withAgeRange(qs.get("a")).withRole(qs.get("r")).build())).collect(Collectors.toSet());

    }

    private static String getLabel(RDFNode res) {
        Statement labelProp = res.asResource().getProperty(RDFS.label);
        if (labelProp != null) {
            return labelProp.getString();
        } else {
            return null;
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


    public Collection<String> getAllTitlesToken() {
        return asStream(ontologyModel.listStatements(null, RDF.type, EPISODE))
                .filter(statement -> statement.getSubject().getProperty(RDFS.label) != null)
                .map(LabeledResource::fromStatementSubject)
                .map(lr -> Arrays.stream(lr.label().split(" ")))
                .flatMap(Function.identity())
                .collect(Collectors.toSet());
    }

    private Collection<LabeledResource> getSuclassesOf(OntClass klass) {
        return asStream(klass.listSubClasses(false))
                .filter(k -> k.getProperty(RDFS.label) != null)
                .map(k -> new LabeledResource(k.getLocalName(), k)).collect(Collectors.toList());
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

        Resource klass = ontologyModel.createResource(Utils.sanitizeURI(String.format("%s_%s", locationType.resource().getURI(), locationLabel)));
        ontologyModel.add(klass, RDF.type, RDFS.Class);
        ontologyModel.add(klass, RDFS.subClassOf, locationType.resource());
        ontologyModel.add(klass, RDFS.subClassOf, CONCRETE_LOCATION);
        ontologyModel.add(klass, RDFS.label, ontologyModel.createTypedLiteral(locationLabel));


        return new LabeledResource(String.format("le lieu de %s (%s)", locationLabel, getLabel(klass)), klass.asResource());
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


    public LabeledResource getPreviousEpisode(LabeledResource book, Integer i) {
        List<LabeledResource> episodes = this.getAllEpisodesForBook(book);
        return episodes.stream()
                .sorted((Comparator<LabeledResource>) (t0, t1) -> -Integer.valueOf(t0.resource().getProperty(HAS_EPISODE_NUMBER).getInt()).compareTo(
                        Integer.valueOf(t1.resource().getProperty(HAS_EPISODE_NUMBER).getInt())))
                .filter(lr -> Integer.valueOf(lr.resource().getProperty(HAS_EPISODE_NUMBER).getInt()) <= i)

                .findFirst().orElse(LabeledResource.getDefault());

    }


    private List<LabeledResource> getAllEpisodesForBook(LabeledResource book) {
        return asStream(ontologyModel.listStatements(null, HAS_BOOK, book.resource()))
                .map(LabeledResource::fromStatementSubject)
                .collect(Collectors.toList());
    }


    public LabeledResource saveEpisode(String episodeName, LabeledResource episodeBook, Integer episodeNumber, LabeledResource episodeLocation, Collection<LabeledCharacterAppearance> characterAppearances) {
        Resource newEpisode = ontologyModel.createResource(Utils.sanitizeURI(EPISODE + "_" + episodeBook.resource().getLocalName() + "_" + String.format("%03d", episodeNumber)));
        ontologyModel.add(newEpisode, RDF.type, EPISODE);
        ontologyModel.add(newEpisode, RDF.type, OWL2.NamedIndividual);
        ontologyModel.add(newEpisode, RDFS.label, ontologyModel.createLiteral(episodeName));
        ontologyModel.add(newEpisode, HAS_BOOK, episodeBook.resource());
        ontologyModel.add(newEpisode, HAS_EPISODE_NUMBER, ontologyModel.createTypedLiteral(episodeNumber));


        Resource newEpisodeLocation = ontologyModel.createResource(Utils.sanitizeURI(newEpisode.getURI() + "_" + episodeLocation.resource().getLocalName()));
        ontologyModel.add(newEpisode, HAS_LOCATION, newEpisodeLocation);
        ontologyModel.add(newEpisodeLocation, RDF.type, episodeLocation.resource());
        ontologyModel.add(newEpisodeLocation, RDFS.label, ontologyModel.createLiteral(String.format("%s dans l'épisode %d du livre %s : %s", episodeLocation.label(), episodeNumber, episodeBook.label(), episodeName)));


        characterAppearances.forEach(a -> {
            Resource newCharAppearance = ontologyModel.createResource(Utils.sanitizeURI(newEpisode.getURI() + "_" + a.resource().character().resource().getLocalName()));

            ontologyModel.add(newCharAppearance, HAS_ROLE, a.resource().role().resource());
            ontologyModel.add(newCharAppearance, RDFS.label, ontologyModel.createLiteral(
                    String.format("Le personnage de %s dans l'épisode %s de %s",
                            a.label(),
                            newEpisode.getProperty(RDFS.label).getLiteral().getString(),
                            episodeBook.label())));
            ontologyModel.add(newCharAppearance, HAS_AGE_RANGE, a.resource().ageRange().resource());
            ontologyModel.add(newCharAppearance, RDF.type, a.resource().character().resource());
            ontologyModel.add(newCharAppearance, RDF.type, OWL2.NamedIndividual);
            ontologyModel.add(newEpisode, HAS_CHARACTER, newCharAppearance);
        });
        return new LabeledResource(episodeName, newEpisode);
    }


    public Collection<LabeledResource> getAllEpisodes() {
        return asStream(ontologyModel.listStatements(null, RDF.type, EPISODE)).map(LabeledResource::fromStatementSubject).collect(Collectors.toSet());
    }

    public LabeledResource createNewCharacterClass(String s, LabeledResource newClassOfCharacter) {
        Resource res = ontologyModel.createResource(Utils.sanitizeURI(NS + s));
        ontologyModel.add(res, RDF.type, OWL2.Class);
        ontologyModel.add(res, RDF.type, OWL2.NamedIndividual);
        ontologyModel.add(res, RDFS.subClassOf, newClassOfCharacter.resource());
        ontologyModel.add(res, RDFS.label, s);
        return new LabeledResource(s, res);

    }
}
