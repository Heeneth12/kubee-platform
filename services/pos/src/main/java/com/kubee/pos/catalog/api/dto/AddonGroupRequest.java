package com.kubee.pos.catalog.api.dto;

import com.kubee.pos.catalog.application.command.CreateAddonGroupCommand;
import com.kubee.pos.catalog.application.command.UpdateAddonGroupCommand;
import com.kubee.pos.catalog.domain.AddonSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AddonGroupRequest(
        @NotBlank @Size(max = 100) String name,
        Integer minSelect,
        Integer maxSelect,
        Boolean active,
        @Valid List<AddonRequest> addons
) {

    public CreateAddonGroupCommand toCreateCommand() {
        return new CreateAddonGroupCommand(name, minSelectOrZero(), maxSelect, specs());
    }

    public UpdateAddonGroupCommand toUpdateCommand(String addonGroupUuid) {
        return new UpdateAddonGroupCommand(addonGroupUuid, name, minSelectOrZero(), maxSelect,
                active == null || active, specs());
    }

    private int minSelectOrZero() {
        return minSelect == null ? 0 : minSelect;
    }

    private List<AddonSpec> specs() {
        return addons == null ? List.of() : addons.stream().map(AddonRequest::toSpec).toList();
    }
}
