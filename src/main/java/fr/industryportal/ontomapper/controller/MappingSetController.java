package fr.industryportal.ontomapper.controller;

import fr.industryportal.ontomapper.cache.CacheSet;
import fr.industryportal.ontomapper.model.entities.Contribution;
import fr.industryportal.ontomapper.model.entities.Contributor;
import fr.industryportal.ontomapper.model.entities.MappingSet;
import fr.industryportal.ontomapper.model.entities.enums.ContributorType;
import fr.industryportal.ontomapper.model.repos.ContributionRepository;
import fr.industryportal.ontomapper.model.repos.ContributorRepository;
import fr.industryportal.ontomapper.model.repos.MappingRepository;
import fr.industryportal.ontomapper.model.repos.MappingSetRepository;
import fr.industryportal.ontomapper.model.requests.SetRequest;
import fr.industryportal.ontomapper.model.requests.User;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author Abdelwadoud Rasmi
 * Controller to manage mapping sets
 */
@RestController
@RequestMapping("/set")
public class MappingSetController {

    @Autowired
    private MappingSetRepository mappingSetRepository;

    @Autowired
    private MappingRepository mappingRepository;

    @Autowired
    private ContributorRepository contributorRepository;

    @Autowired
    private ContributionRepository contributionRepository;


    @GetMapping("")
    public List<MappingSet> getAllSets() {
        return CacheSet.getInstance(mappingSetRepository).getSets();
    }

    @GetMapping("/match")
    public List<MappingSet> getSetsByStringMatch(@RequestParam String string) {
        string = string.toLowerCase().trim();
        if (string.isEmpty())
            return new ArrayList<>();
        //
        String finalMatch = string;

        return CacheSet
                .getInstance(mappingSetRepository)
                .getSets()
                .stream()
                .filter(ms -> contains(ms.getMapping_set_id(), finalMatch)
                        || contains(ms.getVersion(), finalMatch)
                        || contains(ms.getDescription(), finalMatch)
                        || contains(ms.getLicense(), finalMatch)
                        || contains(ms.getSubject_type(), finalMatch)
                        || contains(ms.getSubject_source(), finalMatch)
                        || contains(ms.getSubject_source_version(), finalMatch)
                        || contains(ms.getObject_type(), finalMatch)
                        || contains(ms.getObject_source(), finalMatch)
                        || contains(ms.getObject_source_version(), finalMatch)
                        || contains(ms.getMapping_provider(), finalMatch)
                        || contains(ms.getMapping_tool(), finalMatch)
                        || contains(ms.getSubject_match_field(), finalMatch)
                        || contains(ms.getObject_match_field(), finalMatch)
                        || contains(ms.getObject_preprocessing(), finalMatch)
                        || contains(ms.getSubject_preprocessing(), finalMatch)
                        || contains(ms.getSee_also(), finalMatch)
                        || contains(ms.getOther(), finalMatch)
                        || contains(ms.getComment(), finalMatch))
                .collect(Collectors.toList());
    }

    /**
     * Add or delete a list of mappingSets
     */
    @PostMapping("")
    public JSONObject addOrEditMappingSets(HttpServletRequest request,
                                     @RequestBody List<SetRequest> sets) {

        User user = ((User) request.getAttribute("user"));
        AtomicReference<Long> generatedId = new AtomicReference<>() ;
        //
        sets.forEach(setRequest -> {
            MappingSet s = setRequest.toDBModel(mappingSetRepository, user.getApikey());
            // inserting or updating the mapping set
            s = mappingSetRepository.findByStringId(s.getMapping_set_id());

            if (s == null || !s.isDeleted()) {
                if (s != null)
                    setRequest.setId(s.getId());

                s = mappingSetRepository.save(setRequest.toDBModel(mappingSetRepository, user.getApikey()));

            }
            //saving creators (as contributors) if they don't exist
            MappingSet finalS = s;
            generatedId.set(s.getId());

            if (setRequest.getCreators() != null)
                setRequest.getCreators().forEach(c -> {
                    Contributor creator;
                    try {
                        creator = contributorRepository.save(c.toDBModel(null, user.getApikey()));
                    } catch (Exception e) {
                        creator = contributorRepository.findByContributorId(c.getId());
                    }
                    //
                    if (finalS != null) {
                        List<MappingSet> mappingSets = Stream.of(finalS).collect(Collectors.toList());
                        contributionRepository.save(new Contribution(null, creator, ContributorType.CREATOR, mappingSets, null, false));
                    }
                });
        });
        //
        CacheSet
                .getInstance(mappingSetRepository)
                .rearm();

        JSONObject response = new JSONObject();
        response.put("set_id", generatedId.get());

        return response;

    }


    /**
     * Delete a mapping set along with its mappings
     */
    @DeleteMapping("")
    public void deleteSet(HttpServletRequest request,
                          @RequestParam Long set) {

        User user = ((User) request.getAttribute("user"));
        //
        Optional<MappingSet> mappingSet = mappingSetRepository.findById(set);
        if (mappingSet.isPresent() && (user.getRoles().contains("ADMINISTRATOR") ||
                user.getApikey().equals(mappingSet.get().getCreated_by()))) {
            //
            mappingRepository.deleteBySetId(set);
            mappingSetRepository.deleteById(set);
        }

        //
        CacheSet
                .getInstance(mappingSetRepository)
                .rearm();


    }

    /**
     * Checks if a string contains an other string
     * taking into account enums that return a string
     */
    private boolean contains(Object param1, String param2) {
        String param = (param1 == null) ? "" : param1.toString();
        return (param1 != null && param2 != null)
                && (param.toLowerCase().contains(param2.toLowerCase()) || param2.toLowerCase().contains(param.toLowerCase()));
    }

}
