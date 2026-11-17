package com.safayet.afsos_nama.repository;

import com.safayet.afsos_nama.model.StudentPromise;
import com.safayet.afsos_nama.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentPromiseRepository extends JpaRepository<StudentPromise, Integer> {

    @EntityGraph(attributePaths = "sourceAfsos")
    List<StudentPromise> findByUserOrderByTargetDateAsc(User user);

    List<StudentPromise> findByUserAndSemesterOrderByTargetDateAsc(User user, String semester);

    Optional<StudentPromise> findByIdAndUser(Integer id, User user);

    @Query("select distinct p.semester from StudentPromise p where p.user = :user and p.semester is not null")
    List<String> findSemesters(@Param("user") User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from StudentPromise p where p.id = :id and p.user = :user")
    Optional<StudentPromise> findForUpdate(@Param("id") Integer id, @Param("user") User user);
}
