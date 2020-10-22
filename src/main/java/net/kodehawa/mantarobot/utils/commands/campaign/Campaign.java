/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.utils.commands.campaign;

import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;

public enum Campaign {
    // At least we're sincere with names.
    PREMIUM("general.campaigns.premium.thanks_message", "general.campaigns.premium.generic_sellout"),
    PREMIUM_DAILY("general.campaigns.premium.thanks_message_daily", "general.campaigns.premium.daily_sellout"),
    TWITTER("general.campaigns.twitter", "general.campaigns.twitter");

    final String premiumCampaign;
    final String campaign;

    Campaign(String premiumCampaign, String campaign) {
        this.premiumCampaign = premiumCampaign;
        this.campaign = campaign;
    }

    public String getStringFromCampaign(I18nContext languageContext, boolean premium) {
        return "\n" + languageContext.get(premium ? this.premiumCampaign : this.campaign);
    }
}
