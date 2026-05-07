package com.xzavier0722.mc.plugin.slimeglue.manager;

import com.xzavier0722.mc.plugin.slimeglue.SlimeGlue;
import com.xzavier0722.mc.plugin.slimeglue.api.ACompatibilityModule;
import com.xzavier0722.mc.plugin.slimeglue.api.listener.IListener;
import com.xzavier0722.mc.plugin.slimeglue.api.listener.SubscriptionType;
import com.xzavier0722.mc.plugin.slimeglue.api.protection.IProtectionHandler;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CompatibilityModuleManager {

    private final Map<String, ACompatibilityModule> disabledModules;
    private final Map<String, ACompatibilityModule> enabledModules;
    private final Set<IProtectionHandler> enabledProtectionHandlerSet = new HashSet<>();
    private final Set<IListener> enabledListenerSet = new HashSet<>();

    public CompatibilityModuleManager() {
        disabledModules = new HashMap<>();
        enabledModules = new HashMap<>();
    }

    public void register(ACompatibilityModule module) {
        disabledModules.put(module.getCompatibilityPluginName(), module);
    }

    public void load() {
        disabledModules.values().removeIf(this::load);
    }

    public void load(String pluginName) {
        ACompatibilityModule module = disabledModules.get(pluginName);
        if (module == null) {
            return;
        }

        load(module);
    }

    public void unload(String pluginName) {
        ACompatibilityModule module = enabledModules.remove(pluginName);
        if (module == null) {
            return;
        }
        enabledProtectionHandlerSet.removeAll(module.getProtectionHandlers());
        enabledListenerSet.removeAll(module.getListeners());

        module.disable();
        disabledModules.put(module.getCompatibilityPluginName(), module);
    }

    public Set<IListener> getListeners(String sfId) {
        Set<IListener> re = new HashSet<>();
        enabledListenerSet.forEach(l -> {
            if (l.getSubscriptionType() == SubscriptionType.SUBSCRIBE_TYPE_ALL || l.getSubscribedId().contains(sfId)) {
                re.add(l);
            }
        });
        return re;
    }

    public Set<IProtectionHandler> getProtectionHandlers() {
        return enabledProtectionHandlerSet;
    }

    private boolean load(ACompatibilityModule module) {
        String name = module.getCompatibilityPluginName();
        SlimeGlue.logger().i("Loading compatibility module for " + name);
        Plugin plugin = SlimeGlue.instance().getServer().getPluginManager().getPlugin(name);

        if (plugin == null || !plugin.isEnabled()) {
            SlimeGlue.logger().w("Plugin " + name + " is not installed or enabled, module ignored.");
            return false;
        }

        try {
            module.enable(plugin);
            enabledModules.put(name, module);
            enabledProtectionHandlerSet.addAll(module.getProtectionHandlers());
            enabledListenerSet.addAll(module.getListeners());
            return true;
        } catch (Throwable e) {
            SlimeGlue.logger().e("Exception thrown while loading the compatibility module for " + name);
            e.printStackTrace();
        }
        return false;
    }

}
