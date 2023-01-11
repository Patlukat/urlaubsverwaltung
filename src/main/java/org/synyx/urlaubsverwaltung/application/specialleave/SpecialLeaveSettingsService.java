package org.synyx.urlaubsverwaltung.application.specialleave;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
public class SpecialLeaveSettingsService {

    private final SpecialLeaveSettingsRepository specialLeaveSettingsRepository;

    SpecialLeaveSettingsService(SpecialLeaveSettingsRepository specialLeaveSettingsRepository) {
        this.specialLeaveSettingsRepository = specialLeaveSettingsRepository;
    }

    public void saveAll(List<SpecialLeaveSettingsItem> specialLeaveSettingsItems) {

        final Map<Long, SpecialLeaveSettingsItem> itemsById = specialLeaveSettingsItems.stream()
            .collect(toMap(SpecialLeaveSettingsItem::getId, identity()));

        final List<SpecialLeaveSettingsEntity> entities = specialLeaveSettingsRepository.findAllById(itemsById.keySet()).stream()
            .map(entity -> mergeUpdates(itemsById, entity))
            .collect(toList());

        specialLeaveSettingsRepository.saveAll(entities);
    }

    public List<SpecialLeaveSettingsItem> getSpecialLeaveSettings() {
        return specialLeaveSettingsRepository.findAll().stream()
            .map(this::toSpecialLeaveSettingsItem)
            .collect(toList());
    }

    private SpecialLeaveSettingsEntity mergeUpdates(Map<Long, SpecialLeaveSettingsItem> itemsById, SpecialLeaveSettingsEntity entity) {
        final SpecialLeaveSettingsItem specialLeaveSettingsItem = itemsById.get(entity.getId());
        entity.setActive(specialLeaveSettingsItem.isActive());
        entity.setDays(specialLeaveSettingsItem.getDays());
        return entity;
    }

    private SpecialLeaveSettingsItem toSpecialLeaveSettingsItem(SpecialLeaveSettingsEntity specialLeaveSettingsEntity) {
        return new SpecialLeaveSettingsItem(specialLeaveSettingsEntity.getId(),
            specialLeaveSettingsEntity.isActive(),
            specialLeaveSettingsEntity.getMessageKey(),
            specialLeaveSettingsEntity.getDays());
    }
}
