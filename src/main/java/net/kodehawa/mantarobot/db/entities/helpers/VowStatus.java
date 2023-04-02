package net.kodehawa.mantarobot.db.entities.helpers;

import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;

public enum VowStatus {
    // Modify instead
    ALREADY_DONE("commands.marry.vow.vow_added"),
    NOT_ENOUGH_MONEY("commands.marry.vow.not_enough_money"),
    MODIFY_NOT_ENOUGH_MONEY("commands.marry.vow_edit.not_enough_money"),
    NO_MARRIAGE("commands.marry.vow.no_marriage"),
    MISSING_ITEM("commands.marry.vow.no_item"),
    TOO_LONG("commands.marry.vow.too_long"),
    SUCCESS("commands.marry.vow.success");

    final String status;
    VowStatus(String status) {
        this.status = status;
    }

    public String getStatus(I18nContext i18nContext) {
        return i18nContext.get(status);
    }
}
