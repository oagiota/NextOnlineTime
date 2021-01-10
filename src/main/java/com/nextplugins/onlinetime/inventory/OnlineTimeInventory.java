package com.nextplugins.onlinetime.inventory;

import com.google.inject.Inject;
import com.henryfabio.minecraft.inventoryapi.editor.InventoryEditor;
import com.henryfabio.minecraft.inventoryapi.inventory.impl.paged.PagedInventory;
import com.henryfabio.minecraft.inventoryapi.item.InventoryItem;
import com.henryfabio.minecraft.inventoryapi.item.supplier.InventoryItemSupplier;
import com.henryfabio.minecraft.inventoryapi.viewer.Viewer;
import com.henryfabio.minecraft.inventoryapi.viewer.configuration.border.Border;
import com.henryfabio.minecraft.inventoryapi.viewer.configuration.impl.ViewerConfigurationImpl;
import com.henryfabio.minecraft.inventoryapi.viewer.impl.paged.PagedViewer;
import com.nextplugins.onlinetime.NextOnlineTime;
import com.nextplugins.onlinetime.api.player.TimedPlayer;
import com.nextplugins.onlinetime.api.reward.Reward;
import com.nextplugins.onlinetime.configuration.values.MessageValue;
import com.nextplugins.onlinetime.manager.RewardManager;
import com.nextplugins.onlinetime.manager.TimedPlayerManager;
import com.nextplugins.onlinetime.utils.ItemBuilder;
import com.nextplugins.onlinetime.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yuhtin
 * Github: https://github.com/Yuhtin
 */
public class OnlineTimeInventory extends PagedInventory {

    private final Map<String, Integer> playerRewardFilter = new LinkedHashMap<>();

    @Inject private RewardManager rewardManager;
    @Inject private TimedPlayerManager timedPlayerManager;

    public OnlineTimeInventory() {
        super(
                "online-time.main",
                "Seu tempo no servidor",
                6 * 9
        );

        NextOnlineTime.getInstance().getInjector().injectMembers(this);

    }

    @Override
    protected void configureInventory(Viewer viewer, InventoryEditor editor) {

        ViewerConfigurationImpl.Paged pagedViewer = viewer.getConfiguration();

        pagedViewer.itemPageLimit(21);
        pagedViewer.border(Border.of(1, 1, 2, 1));

        editor.setItem(48, InventoryItem.of(
                new ItemBuilder(viewer.getPlayer().getName())
                        .name("&a" + viewer.getPlayer().getName())
                        .setLore(
                                "&7Confia seu progresso abaixo:",
                                "&7Total de tempo online: &f1 dia e 2 horas"
                        )
                        .wrap()
                )
        );

        editor.setItem(48, changeFilterInventoryItem(viewer));

        editor.setItem(50, InventoryItem.of(
                new ItemBuilder(Material.GOLD_INGOT)
                        .name("&6TOP Online")
                        .setLore("&7Clique para ver os top jogadores", "&7onlines no servidor")
                        .wrap()
                )
        );

    }

    @Override
    protected List<InventoryItemSupplier> createPageItems(PagedViewer viewer) {

        List<InventoryItemSupplier> items = new ArrayList<>();

        Player player = viewer.getPlayer();
        TimedPlayer timedPlayer = timedPlayerManager.getByName(player.getName());

        int rewardFilter = playerRewardFilter.getOrDefault(viewer.getName(), -1);

        for (String name : rewardManager.getRewards().keySet()) {

            Reward reward = rewardManager.getByName(name);
            int statusCode = getStatusCode(timedPlayer, reward);
            if (rewardFilter != -1 && rewardFilter != statusCode) continue;

            String collectStatus = getStatusMessage(statusCode);

            List<String> replacedLore = new ArrayList<>();
            for (String line : MessageValue.get(MessageValue::rewardLore)) {

                if (line.contains("%reward_description%")) replacedLore.addAll(reward.getDescription());
                else {

                    replacedLore.add(line
                            .replace("%time%", TimeUtils.formatTime(reward.getTime()))
                            .replace("%collect_message%", collectStatus)
                    );

                }

            }

            items.add(() -> InventoryItem.of(
                    new ItemBuilder(reward.getIcon())
                            .name(reward.getColoredName())
                            .setLore(replacedLore)
                            .wrap()
                    ).defaultCallback(callback -> {

                        if (statusCode != 1) {

                            player.sendMessage(collectStatus);
                            return;

                        }

                        int avaliableSpaces = 0;
                        for (ItemStack content : player.getInventory().getContents()) {

                            if (content != null && content.getType() != Material.AIR) continue;
                            ++avaliableSpaces;

                        }

                        if (avaliableSpaces < reward.getCommands().size()) {

                            player.sendMessage(MessageValue.get(MessageValue::noSpace)
                                    .replace("%spaces%", String.valueOf(reward.getCommands().size() - avaliableSpaces))
                            );
                            return;

                        }

                        for (String command : reward.getCommands()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                        }

                        player.sendMessage(MessageValue.get(MessageValue::collectedReward));
                        timedPlayer.getCollectedRewards().add(name);
                        callback.updateInventory();

                    })
            );


        }

        return items;

    }

    public int getStatusCode(TimedPlayer timedPlayer, Reward reward) {

        int statusCode = 0;
        if (timedPlayer.getTimeInServer() < reward.getTime()) statusCode = 1;
        if (timedPlayer.getCollectedRewards().contains(reward.getName())) statusCode = 2;

        return statusCode;

    }

    public String getStatusMessage(int statusCode) {

        switch (statusCode) {

            case 0:
                return MessageValue.get(MessageValue::collect);
            case 1:
                return MessageValue.get(MessageValue::noTimeToCollect);
            default:
                return MessageValue.get(MessageValue::alreadyCollected);

        }

    }

    private InventoryItem changeFilterInventoryItem(Viewer viewer) {
        AtomicInteger currentFilter = new AtomicInteger(playerRewardFilter.getOrDefault(viewer.getName(), -1));
        return InventoryItem.of(new ItemBuilder(Material.HOPPER)
                .name("&eFiltro de recompensas")
                .setLore(
                        getColorByFilter(currentFilter.get(), -1) + "* Todas as recompensas",
                        getColorByFilter(currentFilter.get(), 0) + "* Recompensas para coletar",
                        getColorByFilter(currentFilter.get(), 1) + "* Recompensas futuras",
                        getColorByFilter(currentFilter.get(), 2) + "* Recompensas já coletadas"
                )
                .wrap())
                .defaultCallback(event -> {
                    playerRewardFilter.put(viewer.getName(), currentFilter.incrementAndGet() > 2 ? -1 : currentFilter.get());
                    event.updateInventory();
                });
    }

    private String getColorByFilter(int currentFilter, int loopFilter) {
        return currentFilter == loopFilter ? "&a" : "&7";
    }

}
