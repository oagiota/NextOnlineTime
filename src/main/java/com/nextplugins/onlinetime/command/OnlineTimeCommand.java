package com.nextplugins.onlinetime.command;

import com.google.inject.Inject;
import com.nextplugins.onlinetime.api.conversion.Conversor;
import com.nextplugins.onlinetime.api.player.TimedPlayer;
import com.nextplugins.onlinetime.configuration.values.MessageValue;
import com.nextplugins.onlinetime.inventory.OnlineTimeInventory;
import com.nextplugins.onlinetime.manager.ConversorManager;
import com.nextplugins.onlinetime.manager.TimedPlayerManager;
import com.nextplugins.onlinetime.utils.ColorUtils;
import com.nextplugins.onlinetime.utils.TimeUtils;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Optional;
import me.saiintbrisson.minecraft.command.command.Context;
import me.saiintbrisson.minecraft.command.target.CommandTarget;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Yuhtin
 * Github: https://github.com/Yuhtin
 */
public class OnlineTimeCommand {

    @Inject private TimedPlayerManager timedPlayerManager;
    @Inject private ConversorManager conversorManager;

    @Command(
            name = "tempo",
            aliases = {"tempoonline", "time"},
            target = CommandTarget.PLAYER
    )
    public void timeCommand(Context<Player> context) {
        context.getSender().sendMessage(
                MessageValue.get(MessageValue::helpMessage)
                        .stream()
                        .map(line -> line.replace("%label%", context.getLabel()))
                        .collect(Collectors.toList())
                        .toArray(new String[]{})
        );
    }

    @Command(
            name = "tempo.ver",
            target = CommandTarget.PLAYER
    )
    public void viewTimeCommand(Context<Player> context,
                                @Optional Player target) {

        String name = target == null ? context.getSender().getName() : target.getName();

        TimedPlayer timedPlayer = this.timedPlayerManager.getPlayers().getOrDefault(name, null);
        if (timedPlayer == null) {

            context.sendMessage(MessageValue.get(MessageValue::offlinePlayer));
            return;

        }

        context.sendMessage(MessageValue.get(MessageValue::timeOfTarget)
                .replace("%target%", name)
                .replace("%time%", TimeUtils.formatTime(timedPlayer.getTimeInServer()))
        );

    }

    @Command(
            name = "tempo.menu",
            target = CommandTarget.PLAYER
    )
    public void openInventoryCommand(Context<Player> context) {

        OnlineTimeInventory onlineTimeInventory = new OnlineTimeInventory().init();
        onlineTimeInventory.openInventory(context.getSender());

    }

    @Command(
            name = "tempo.enviar",
            target = CommandTarget.PLAYER,
            permission = "nextonlinetime.sendtime"
    )
    public void sendTimeCommand(Context<Player> context,
                                Player target,
                                String time) {

        if (context.getSender() == target) {

            context.sendMessage(MessageValue.get(MessageValue::cantSendForYou));
            return;

        }

        long timeInMillis = TimeUtils.getTime(time);
        if (timeInMillis < 1) {

            context.sendMessage(MessageValue.get(MessageValue::invalidTime));
            return;

        }

        TimedPlayer timedPlayer = this.timedPlayerManager.getByName(context.getSender().getName());
        if (timedPlayer.getTimeInServer() < timeInMillis) {

            context.sendMessage(MessageValue.get(MessageValue::noTime));
            return;

        }

        TimedPlayer timedTarget = this.timedPlayerManager.getByName(target.getName());

        timedTarget.addTime(timeInMillis);
        timedPlayer.removeTime(timeInMillis);

        context.sendMessage(MessageValue.get(MessageValue::sendedTime)
                .replace("%time%", TimeUtils.formatTime(timeInMillis))
                .replace("%target%", target.getName())
        );

        target.sendMessage(MessageValue.get(MessageValue::receivedTime)
                .replace("%time%", TimeUtils.formatTime(timeInMillis))
                .replace("%sender%", context.getSender().getName())
        );

    }

    @Command(
            name = "conversor",
            permission = "nextonlinetime.admin",
            target = CommandTarget.ALL
    )
    public void onConversorCommand(Context<CommandSender> context,
                                   String conversor) {

        Conversor pluginConversor = checkConversorAvaility(context, conversor);
        if (pluginConversor == null) return;

        context.sendMessage(ColorUtils.colored(
                "&aIniciando conversão de dados do plugin " + pluginConversor.getConversorName() + "."
        ));

        long initial = System.currentTimeMillis();
        this.conversorManager.setConverting(true);

        Set<TimedPlayer> timedPlayers = pluginConversor.lookupPlayers();
        if (timedPlayers == null) {

            context.sendMessage(ColorUtils.colored(
                    "&cOcorreu um erro, veja se configurou corretamente o conversor."
            ));
            return;

        }

        this.conversorManager.startConversion(
                context.getSender(),
                timedPlayers,
                pluginConversor.getConversorName(),
                initial
        );

    }

    private Conversor checkConversorAvaility(Context<CommandSender> context, String conversor) {

        if (this.conversorManager.isConverting()) {

            context.sendMessage(ColorUtils.colored(
                    "&cVocê já está convertendo uma tabela, aguarde a finalização da mesma."
            ));
            return null;

        }

        int maxPlayers = context.getSender() instanceof Player ? 1 : 0;
        if (Bukkit.getOnlinePlayers().size() > maxPlayers) {

            context.sendMessage(ColorUtils.colored(
                    "&cEsta função só pode ser usada com apenas você online."
            ));
            return null;

        }

        Conversor pluginConversor = this.conversorManager.getByName(conversor);
        if (pluginConversor == null) {

            context.sendMessage(ColorUtils.colored(
                    "&cEste conversor é inválido, conversores válidos: " + this.conversorManager.avaliableConversors()
            ));

        }

        return pluginConversor;

    }

}
