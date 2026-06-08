package de.omegazirkel.risingworld.globalintercom.ui;

import de.omegazirkel.risingworld.GlobalIntercom;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import de.omegazirkel.risingworld.tools.ui.PluginShortcutVisibility;
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
                flexWrapper.addChild(shortcutSetting(uiPlayer));
                OZUIElement element = defaultSettingsContainer();
                element.style.width.set(95, Unit.Percent);
                UILabel label = defaultSettingsLabel(t().get("TC_SETTINGS_EMPTY", uiPlayer));
                element.addChild(label);
                flexWrapper.addChild(element);
            }

            protected OZUIElement shortcutSetting(Player uiPlayer) {
                OZUIElement element = defaultSettingsContainer();
                element.addChild(defaultSettingsLabel(t().get("TC_LABEL_GLOBAL_INTERCOM_SHORTCUT", uiPlayer)));
                boolean visible = shortcutVisible(uiPlayer);
                element.addChild(switchButtons(uiPlayer, visible, event -> {
                    if (GlobalIntercom.playerSettings != null) {
                        GlobalIntercom.playerSettings.setBoolean(uiPlayer.getDbID(), shortcutKey(), !visible);
                    }
                    redrawContent();
                }));
                return element;
            }
        };
    }

    public static boolean shortcutVisible(Player player) {
        return GlobalIntercom.playerSettings == null
                || GlobalIntercom.playerSettings.getBoolean(player.getDbID(), shortcutKey()).orElse(true);
    }

    private static String shortcutKey() {
        return PluginShortcutVisibility.playerSettingKey(GlobalIntercom.name);
    }
}
