package top.nextnet.greekmythcoding.onto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

class OntoFacadeTest {

    @Test
    void getAllLocations() {
        OntoFacade facade = new OntoFacade();
        facade.getAllLocations();
    }

    @Test
    void getCharactersFromPreviousEpisode() {
        List<LabeledResource> res =  new OntoFacade().getCharactersFromPreviousEpisode("https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#FT_02");
        System.out.println(res);

    }

    @Test
    void getCharacterAgeRangeInEpisode() {
        OntoFacade facade = new OntoFacade();
         facade.getCharacterAppearanceInEpisode(OntoFacade.ontologyModel.getResource("https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#Thésée"),
                OntoFacade.ontologyModel.getResource("https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#FT_02"));


    }
    @Test
    void getAgeRange() {
        OntoFacade facade = new OntoFacade();
        facade.getRoleRange().stream().forEach(System.out::println);


    }

    @Test
    void testLionFT02(){
        OntoFacade facade = new OntoFacade();
        CharacterAppearance charApp = facade.getCharacterAppearanceInEpisode(
                OntoFacade.ontologyModel.getResource("https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#Lion_de_Némée"),
                OntoFacade.ontologyModel.getResource("https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#FT_02"));
        System.out.println(charApp);
    }

    @Test
    void testEpiNumber(){
        OntoFacade facade = new OntoFacade();
        System.out.println(facade.getExistingEpisodesNumberForBookList("https://nextnet.top/ontologies/2023/07/greek-mythology-stories/1.0.0#FT").stream().map(i -> i.toString()).collect(Collectors.joining(" ")));
    }
}