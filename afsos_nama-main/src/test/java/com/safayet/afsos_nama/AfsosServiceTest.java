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

    private User user;
    private AfsosDTO dto;


    @BeforeEach
    void setUp() {

        user = new User();

        user.setName("Test User");
        user.setEmail("testuser@gmail.com");
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
        otherUser.setEmail("otheruser@gmail.com");
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
    void approveConfessionTest() {

        dto.setSharedAnonymously(true);

        Afsos savedAfsos = saveTestAfsos();


        assertFalse(
                savedAfsos.isApprovedForFeed()
        );


        afsosService.approveConfession(
                savedAfsos.getId()
        );


        Afsos approvedAfsos =
                afsosRepository
                        .findById(savedAfsos.getId())
                        .orElseThrow();


        assertTrue(
                approvedAfsos.isApprovedForFeed()
        );
    }


    @Test
    void reactTest() {

        dto.setSharedAnonymously(true);

        Afsos savedAfsos = saveTestAfsos();


        afsosService.approveConfession(
                savedAfsos.getId()
        );


        afsosService.react(
                savedAfsos.getId(),
                ReactionType.SAME_BHAI
        );


        Afsos updatedAfsos =
                afsosRepository
                        .findById(savedAfsos.getId())
                        .orElseThrow();


        assertEquals(
                1,
                updatedAfsos.getSameBhaiCount()
        );
    }
}