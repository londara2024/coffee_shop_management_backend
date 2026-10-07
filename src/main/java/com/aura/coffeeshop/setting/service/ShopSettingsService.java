package com.aura.coffeeshop.setting.service;

import com.aura.coffeeshop.common.exception.BusinessRuleException;
import com.aura.coffeeshop.common.exception.NotFoundException;
import com.aura.coffeeshop.common.storage.ImageCleanup;
import com.aura.coffeeshop.common.storage.ImageStorage;
import com.aura.coffeeshop.common.storage.ImageStorage.StoredImage;
import com.aura.coffeeshop.setting.dto.SettingsDtos.BrandColorOption;
import com.aura.coffeeshop.setting.dto.SettingsDtos.CreateStaffRequest;
import com.aura.coffeeshop.setting.dto.SettingsDtos.ShopSettingsResponse;
import com.aura.coffeeshop.setting.dto.SettingsDtos.UpdateShopRequest;
import com.aura.coffeeshop.setting.dto.SettingsDtos.UpdateStaffRequest;
import com.aura.coffeeshop.setting.entity.BrandColor;
import com.aura.coffeeshop.setting.repository.BrandColorRepository;
import com.aura.coffeeshop.store.StaffRole;
import com.aura.coffeeshop.store.dto.StaffResponse;
import com.aura.coffeeshop.store.entity.Staff;
import com.aura.coffeeshop.store.entity.Store;
import com.aura.coffeeshop.store.repository.StaffRepository;
import com.aura.coffeeshop.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * Admin settings: shop info, branding (logo, brand colour), open/closed switch and staff roles.
 * A store must always keep at least one manager.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ShopSettingsService {

    private static final String LOGO_FOLDER = "logos";

    private final StoreService storeService;
    private final StaffRepository staffRepository;
    private final BrandColorRepository brandColorRepository;
    private final ImageStorage imageStorage;

    @Transactional(readOnly = true)
    public ShopSettingsResponse get(String storeId) {
        return toResponse(storeService.resolveStore(storeId));
    }

    public ShopSettingsResponse updateShop(String storeId, UpdateShopRequest request) {
        Store store = storeService.resolveStore(storeId);
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw BusinessRuleException.badRequest("latitude and longitude must be sent together");
        }
        if (request.shopName() != null) {
            store.setName(request.shopName().trim());
        }
        if (request.address() != null) {
            store.setAddress(request.address().trim());
        }
        if (request.hoursLabel() != null) {
            store.setHoursLabel(request.hoursLabel().isBlank() ? null : request.hoursLabel().trim());
        }
        if (request.latitude() != null) {
            store.setLatitude(request.latitude());
            store.setLongitude(request.longitude());
        }
        if (request.brandColor() != null) {
            String hex = request.brandColor().toLowerCase(Locale.ROOT);
            brandColorRepository.findByHex(hex).orElseThrow(() -> BusinessRuleException.badRequest(
                    "brandColor must be one of the preset colours: " + brandColorRepository.findAllByOrderBySortOrderAsc()
                            .stream().map(BrandColor::getHex).toList()));
            store.setBrandColor(hex);
        }
        return toResponse(store);
    }

    /** Upload or replace the shop logo. The previous uploaded logo is deleted once the change commits. */
    public ShopSettingsResponse uploadLogo(String storeId, MultipartFile file) {
        Store store = storeService.resolveStore(storeId);
        StoredImage stored = imageStorage.store(file, LOGO_FOLDER);
        ImageCleanup.onReplace(imageStorage, store.getLogoUrl(), stored.url());
        store.setLogoUrl(stored.url());
        return toResponse(store);
    }

    /** Go back to the default Aura mark. */
    public ShopSettingsResponse removeLogo(String storeId) {
        Store store = storeService.resolveStore(storeId);
        if (store.getLogoUrl() != null) {
            ImageCleanup.afterCommit(imageStorage, store.getLogoUrl());
            store.setLogoUrl(null);
        }
        return toResponse(store);
    }

    /** Open or close the shop. While closed, checkout is refused (see OrderService). */
    public ShopSettingsResponse setOpen(String storeId, boolean open) {
        Store store = storeService.resolveStore(storeId);
        store.setOpen(open);
        return toResponse(store);
    }

    public StaffResponse addStaff(String storeId, CreateStaffRequest request) {
        Staff staff = new Staff();
        staff.setStore(storeService.resolveStore(storeId));
        staff.setName(request.name().trim());
        staff.setRole(request.role());
        return StaffResponse.from(staffRepository.save(staff));
    }

    public StaffResponse updateStaff(String staffId, UpdateStaffRequest request) {
        Staff staff = getStaff(staffId);
        if (request.role() != null && request.role() != staff.getRole()) {
            if (staff.getRole() == StaffRole.MANAGER) {
                requireAnotherManager(staff, "change the role of");
            }
            staff.setRole(request.role());
        }
        if (request.name() != null) {
            staff.setName(request.name().trim());
        }
        return StaffResponse.from(staff);
    }

    /** Past tickets, purchase orders and menu items keep their history; their staff link becomes null. */
    public void removeStaff(String staffId) {
        Staff staff = getStaff(staffId);
        if (staff.getRole() == StaffRole.MANAGER) {
            requireAnotherManager(staff, "remove");
        }
        staffRepository.delete(staff);
    }

    private void requireAnotherManager(Staff manager, String action) {
        if (staffRepository.countByStoreIdAndRole(manager.getStore().getId(), StaffRole.MANAGER) <= 1) {
            throw new BusinessRuleException("Cannot " + action + " " + manager.getName()
                    + ": the store must keep at least one manager");
        }
    }

    private Staff getStaff(String staffId) {
        return staffRepository.findById(staffId).orElseThrow(() -> NotFoundException.of("Staff", staffId));
    }

    private ShopSettingsResponse toResponse(Store store) {
        return ShopSettingsResponse.from(store,
                staffRepository.findByStoreIdOrderByNameAsc(store.getId()).stream().map(StaffResponse::from).toList(),
                brandColorRepository.findAllByOrderBySortOrderAsc().stream().map(BrandColorOption::from).toList());
    }
}
