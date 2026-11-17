package com.safayet.afsos_nama.service;

import com.safayet.afsos_nama.dto.PromiseDTO;
import com.safayet.afsos_nama.model.StudentPromise;
import com.safayet.afsos_nama.model.User;
import com.safayet.afsos_nama.model.enums.PromiseStatus;
import com.safayet.afsos_nama.repository.StudentPromiseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromiseService {

    private final StudentPromiseRepository promiseRepository;

    public boolean savePromise(PromiseDTO dto, User user) {
        StudentPromise promise = dto.getId() == null
                ? new StudentPromise()
                : getPromiseById(dto.getId(), user);

        if (promise == null || dto.getTitle() == null || dto.getTitle().isBlank() || dto.getTargetDate() == null) {
            return false;
        }

        boolean newPromise = promise.getId() == null;
        PromiseStatus existingStatus = promise.getStatus();
        LocalDate existingTargetDate = promise.getTargetDate();
        LocalDate today = LocalDate.now();

        if (newPromise && dto.getTargetDate().isBefore(today)) {
            return false;
        }

        if (!newPromise && existingStatus == PromiseStatus.BROKEN && dto.getTargetDate().isBefore(today)) {
            return false;
        }

        if (!newPromise
                && existingStatus == PromiseStatus.ACTIVE
                && dto.getTargetDate().isBefore(today)
                && !dto.getTargetDate().equals(existingTargetDate)) {
            return false;
        }

        promise.setTitle(dto.getTitle().trim());
        promise.setTargetDate(dto.getTargetDate());

        if (newPromise) {
            promise.setUser(user);
            promise.setStatus(PromiseStatus.ACTIVE);
        } else if (existingStatus == PromiseStatus.BROKEN) {
            promise.setStatus(PromiseStatus.ACTIVE);
        } else {
            promise.setStatus(existingStatus);
        }

        promiseRepository.save(promise);
        return true;
    }

    public List<StudentPromise> getAllPromises(User user) {
        return promiseRepository.findByUserOrderByTargetDateAsc(user);
    }

    public StudentPromise getPromiseById(Integer id, User user) {
        StudentPromise promise = promiseRepository.findById(id).orElse(null);

        if (promise == null || !promise.getUser().getId().equals(user.getId())) {
            return null;
        }

        return promise;
    }

    public boolean markCompleted(Integer id, User user) {
        StudentPromise promise = getPromiseById(id, user);

        if (promise == null || promise.getStatus() != PromiseStatus.ACTIVE) {
            return false;
        }

        promise.setStatus(PromiseStatus.COMPLETED);
        promiseRepository.save(promise);
        return true;
    }

    public boolean markBroken(Integer id, User user) {
        StudentPromise promise = getPromiseById(id, user);

        if (promise == null || promise.getStatus() != PromiseStatus.ACTIVE) {
            return false;
        }

        promise.setStatus(PromiseStatus.BROKEN);
        promise.setBrokenCount(promise.getBrokenCount() + 1);
        promiseRepository.save(promise);
        return true;
    }

    public boolean deletePromise(Integer id, User user) {
        StudentPromise promise = getPromiseById(id, user);

        if (promise == null) {
            return false;
        }

        promiseRepository.delete(promise);
        return true;
    }
}
