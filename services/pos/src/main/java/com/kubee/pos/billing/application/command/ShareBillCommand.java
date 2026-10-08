package com.kubee.pos.billing.application.command;

import com.kubee.pos.billing.domain.ShareChannel;
import com.kubee.pos.common.cqrs.Command;

/** Records that the bill was printed / sent. Sending on WhatsApp or SMS itself is not built yet. */
public record ShareBillCommand(String billUuid, ShareChannel channel) implements Command<Void> {
}
