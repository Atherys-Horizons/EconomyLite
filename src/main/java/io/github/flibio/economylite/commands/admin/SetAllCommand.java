/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT). See the LICENSE file at the root of this project for more information.
 */
package io.github.flibio.economylite.commands.admin;

import io.github.flibio.economylite.EconomyLite;
import io.github.flibio.utils.commands.AsyncCommand;
import io.github.flibio.utils.commands.BaseCommandExecutor;
import io.github.flibio.utils.commands.Command;
import io.github.flibio.utils.commands.ParentCommand;
import io.github.flibio.utils.message.MessageStorage;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.spec.CommandSpec.Builder;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.text.Text;

import java.math.BigDecimal;

@AsyncCommand
@ParentCommand(parentCommand = EconCommand.class)
@Command(aliases = {"setall"}, permission = "economylite.admin.econ.setall")
public class SetAllCommand extends BaseCommandExecutor<CommandSource> {

    private MessageStorage messageStorage = EconomyLite.getMessageStorage();

    @Override
    public Builder getCommandSpecBuilder() {
        return CommandSpec.builder()
                .executor(this)
                .arguments(GenericArguments.doubleNum(Text.of("balance")));
    }

    @Override
    public void run(CommandSource src, CommandContext args) {
        if (args.getOne("balance").isPresent()) {
            String targetName = "all players";
            BigDecimal newBal = BigDecimal.valueOf(args.<Double>getOne("balance").get());
            if (EconomyLite.getPlayerService().setBalanceAll(newBal, EconomyLite.getCurrencyService().getCurrentCurrency(),
                    Cause.of(EventContext.empty(),(EconomyLite.getInstance())))) {
                src.sendMessage(messageStorage.getMessage("command.econ.setsuccess", "name", targetName));
            } else {
                src.sendMessage(messageStorage.getMessage("command.econ.setfail", "name", targetName));
            }
        } else {
            src.sendMessage(messageStorage.getMessage("command.error"));
        }
    }

}
