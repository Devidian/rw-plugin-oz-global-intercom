package de.omegazirkel.risingworld.globalintercom.ui;

import de.omegazirkel.risingworld.GlobalIntercom;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Unit;

public class GlobalIntercomPlayerPluginSettings extends PlayerPluginSettings {
    public GlobalIntercomPlayerPluginSettings(String pluginVersion) {
        this.pluginLabel = GlobalIntercom.name;
        this.pluginVersion = pluginVersion;
    }

    private I18n t() {
        return I18n.getInstance(GlobalIntercom.name);
    }

    @Override
    public BasePlayerPluginSettingsPanel createPlayerPluginSettingsUIElement(Player uiPlayer) {
        return new BasePlayerPluginSettingsPanel(uiPlayer, pluginLabel) {
            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();
                OZUIElement element = defaultSettingsContainer();
                element.style.width.set(95, Unit.Percent);
                UILabel label = defaultSettingsLabel(t().get("TC_SETTINGS_EMPTY", uiPlayer));
                element.addChild(label);
                flexWrapper.addChild(element);
            }
        };
    }
}
