package com.aura.coffeeshop.store.service;

import com.aura.coffeeshop.common.exception.NotFoundException;
import com.aura.coffeeshop.store.dto.StaffResponse;
import com.aura.coffeeshop.store.dto.StoreResponse;
import com.aura.coffeeshop.store.entity.Staff;
import com.aura.coffeeshop.store.entity.Store;
import com.aura.coffeeshop.store.repository.StaffRepository;
import com.aura.coffeeshop.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final StaffRepository staffRepository;

    public List<StoreResponse> listStores() {
        return storeRepository.findAllByOrderByNameAsc().stream().map(StoreResponse::from).toList();
    }

    public List<StaffResponse> listStaff(String storeId) {
        getStore(storeId);
        return staffRepository.findByStoreIdOrderByNameAsc(storeId).stream().map(StaffResponse::from).toList();
    }

    public Store getStore(String id) {
        return storeRepository.findById(id).orElseThrow(() -> NotFoundException.of("Store", id));
    }

    /** The given store, or the main (first-created) store when {@code id} is null. */
    public Store resolveStore(String id) {
        if (id != null) {
            return getStore(id);
        }
        return storeRepository.findFirstByOrderByCreatedAtAscIdAsc()
                .orElseThrow(() -> new NotFoundException("No store is configured"));
    }

    public Staff getStaff(String id) {
        return staffRepository.findById(id).orElseThrow(() -> NotFoundException.of("Staff", id));
    }
}
