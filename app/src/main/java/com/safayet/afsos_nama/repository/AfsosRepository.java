package com.safayet.afsos_nama.repository;

import com.safayet.afsos_nama.model.Afsos;
import com.safayet.afsos_nama.model.User;
import com.safayet.afsos_nama.model.enums.AfsosCategory;
import com.safayet.afsos_nama.model.enums.AfsosLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AfsosRepository extends JpaRepository<Afsos, Integer> {

    List<Afsos> findByUserOrderByRegretDateDesc(User user);

    List<Afsos> findByUserAndSemesterOrderByRegretDateDesc(User user, String semester);

    Optional<Afsos> findByIdAndUser(Integer id, User user);

    @Query("select distinct a.semester from Afsos a where a.user = :user and a.semester is not null")
    List<String> findSemesters(@Param("user") User user);

    @Query("""
            select a from Afsos a where a.user = :user
              and (:category is null or a.category = :category)
              and (:level is null or a.level = :level)
              and (:keyword = '' or locate(:keyword, lower(a.title)) > 0
                   or locate(:keyword, lower(a.description)) > 0
                   or locate(:keyword, lower(coalesce(a.relatedCourse, ''))) > 0)
            order by a.regretDate desc, a.id desc
            """)
    Page<Afsos> search(@Param("user") User user, @Param("keyword") String keyword,
                       @Param("category") AfsosCategory category, @Param("level") AfsosLevel level,
                       Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Afsos a where a.id = :id")
    Optional<Afsos> findForUpdate(@Param("id") Integer id);

    List<Afsos> findBySharedAnonymouslyTrueAndApprovedForFeedTrueOrderByRegretDateDesc();

    @EntityGraph(attributePaths = "user")
    List<Afsos> findBySharedAnonymouslyTrueAndApprovedForFeedFalseOrderByRegretDateDesc();
}
