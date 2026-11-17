package com.safayet.afsos_nama.model;

import com.safayet.afsos_nama.model.enums.PromiseStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "student_promises")
public class StudentPromise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromiseStatus status = PromiseStatus.ACTIVE;

    @Column(nullable = false)
    private int brokenCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 50)
    private String semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_afsos_id")
    private Afsos sourceAfsos;

    @Transient
    public boolean isOverdue() {
        return status == PromiseStatus.ACTIVE
                && targetDate != null
                && targetDate.isBefore(LocalDate.now());
    }
}
