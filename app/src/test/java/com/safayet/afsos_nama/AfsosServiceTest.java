package com.safayet.afsos_nama;

import com.safayet.afsos_nama.dto.AfsosDTO;
import com.safayet.afsos_nama.model.Afsos;
import com.safayet.afsos_nama.model.User;
import com.safayet.afsos_nama.model.enums.AfsosCategory;
import com.safayet.afsos_nama.model.enums.AfsosLevel;
import com.safayet.afsos_nama.model.enums.AfsosStatus;
import com.safayet.afsos_nama.model.enums.ReactionType;
import com.safayet.afsos_nama.repository.AfsosRepository;
import com.safayet.afsos_nama.repository.UserRepository;
import com.safayet.afsos_nama.service.AfsosService;
import com.safayet.afsos_nama.service.DashboardService;
import com.safayet.afsos_nama.service.PromiseService;
import com.safayet.afsos_nama.service.ReactionService;
import com.safayet.afsos_nama.dto.PromiseDTO;
import com.safayet.afsos_nama.model.enums.Role;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Transactional
public class AfsosServiceTest {

    @Autowired
    private AfsosService afsosService;

    @Autowired
    private AfsosRepository afsosRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReactionService reactionService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private PromiseService promiseService;

    private User user;
    private AfsosDTO dto;


    @BeforeEach
    void setUp() {

        user = new User();

        user.setName("Test User");
        user.setEmail("student@example.test");
        user.setPassword("123456");
        user.setCurrentSemester("6th Semester");

        user = userRepository.save(user);


        dto = new AfsosDTO();

        dto.setTitle("Missed Assignment");
        dto.setDescription("Submitted assignment late");
        dto.setCategory(AfsosCategory.ASSIGNMENT);
        dto.setLevel(AfsosLevel.HIGH);
        dto.setRegretDate(LocalDate.now());
    }

    private Afsos saveTestAfsos() {

        afsosService.saveAfsos(dto, user);

        return afsosRepository
                .findByUserOrderByRegretDateDesc(user)
                .get(0);
    }


    @Test
    void saveAfsosTest() {

        Afsos savedAfsos = saveTestAfsos();

        assertNotNull(savedAfsos.getId());

        assertEquals(
                user.getId(),
                savedAfsos.getUser().getId()
        );

        assertEquals(
                AfsosStatus.NEW,
                savedAfsos.getStatus()
        );

        assertEquals(
                "6th Semester",
                savedAfsos.getSemester()
        );

        assertFalse(savedAfsos.isApprovedForFeed());
    }


    @Test
    void getAfsosByIdTest() {

        Afsos savedAfsos = saveTestAfsos();

        Afsos result = afsosService.getAfsosById(
                savedAfsos.getId(),
                user
        );

        assertNotNull(result);

        assertEquals(
                savedAfsos.getId(),
                result.getId()
        );
    }


    @Test
    void otherUserCannotGetAfsosTest() {

        Afsos savedAfsos = saveTestAfsos();


        User otherUser = new User();

        otherUser.setName("Other User");
        otherUser.setEmail("other@example.test");
        otherUser.setPassword("123456");

        otherUser = userRepository.save(otherUser);


        Afsos result = afsosService.getAfsosById(
                savedAfsos.getId(),
                otherUser
        );

        assertNull(result);
    }


    @Test
    void deleteAfsosTest() {

        Afsos savedAfsos = saveTestAfsos();

        Integer id = savedAfsos.getId();


        afsosService.deleteAfsos(
                id,
                user
        );


        assertFalse(
                afsosRepository.existsById(id)
        );
    }


    @Test
    void sharingRequiresModerationAndOnlyOneReactionPerUser() {
        dto.setSharedAnonymously(true);
        Afsos post = saveTestAfsos();
        User moderator = userRepository.save(account("moderator@example.test", Role.ADMIN));
        User other = userRepository.save(account("reactor@example.test", Role.STUDENT));

        reactionService.setReaction(post.getId(), other.getId(), ReactionType.SAME_BHAI);
        assertTrue(reactionService.summaries(List.of(post), other.getId()).isEmpty());
        assertTrue(afsosService.moderateConfession(post.getId(), moderator, true));
        assertTrue(post.isApprovedForFeed());

        reactionService.setReaction(post.getId(), user.getId(), ReactionType.SAME_BHAI);
        reactionService.setReaction(post.getId(), user.getId(), ReactionType.SAME_BHAI);
        reactionService.setReaction(post.getId(), other.getId(), ReactionType.SAME_BHAI);
        assertEquals(2, reactionService.summaries(List.of(post), user.getId())
                .get(post.getId()).get("SAME_BHAI").total());

        reactionService.setReaction(post.getId(), other.getId(), ReactionType.RIP_CGPA);
        var counts = reactionService.summaries(List.of(post), other.getId()).get(post.getId());
        assertEquals(1, counts.get("SAME_BHAI").total());
        assertFalse(counts.get("SAME_BHAI").selected());
        assertTrue(counts.get("RIP_CGPA").selected());

        reactionService.setReaction(post.getId(), other.getId(), null);
        assertFalse(reactionService.summaries(List.of(post), other.getId())
                .get(post.getId()).containsKey("RIP_CGPA"));
    }

    @Test
    void reportsScopeReflectionsAndLinkedPromises() {
        assertNull(dashboardService.semesterView(user, null).data().progressScore());

        Afsos current = saveTestAfsos();
        dto.setSemester("5th Semester");
        dto.setStatus(AfsosStatus.FIXED);
        afsosService.saveAfsos(dto, user);

        PromiseDTO action = new PromiseDTO();
        action.setSourceAfsosId(current.getId());
        action.setTitle("Submit before the deadline");
        action.setTargetDate(LocalDate.now().plusDays(1));
        User other = userRepository.save(account("other-promise@example.test", Role.STUDENT));
        assertFalse(promiseService.savePromise(action, other));
        assertTrue(promiseService.savePromise(action, user));

        var promise = promiseService.getAllPromises(user).getFirst();
        assertEquals(current.getId(), promise.getSourceAfsos().getId());
        assertEquals("6th Semester", promise.getSemester());
        assertFalse(promiseService.markBroken(promise.getId(), other));
        assertTrue(promiseService.markCompleted(promise.getId(), user));

        var selected = dashboardService.semesterView(user, null);
        assertEquals(1, selected.data().totalAfsos());
        assertEquals(40, selected.data().progressScore());
        assertEquals(2, dashboardService.semesterView(user, "").data().totalAfsos());
        var previous = dashboardService.semesterView(user, "5th Semester");
        assertEquals(80, previous.data().progressScore());
        assertNotNull(previous.data().weeklyResolved());
    }

    private User account(String email, Role role) {
        User account = new User();
        account.setName(email);
        account.setEmail(email);
        account.setPassword("integration-test-only");
        account.setRole(role);
        return account;
    }
}
