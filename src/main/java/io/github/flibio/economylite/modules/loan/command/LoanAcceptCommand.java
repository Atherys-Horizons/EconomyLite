/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2018 Flibio
 * Copyright (c) Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.github.flibio.economylite.modules.loan.command;

import io.github.flibio.economylite.EconomyLite;
import io.github.flibio.economylite.modules.loan.LoanManager;
import io.github.flibio.economylite.modules.loan.LoanModule;
import io.github.flibio.utils.commands.AsyncCommand;
import io.github.flibio.utils.commands.BaseCommandExecutor;
import io.github.flibio.utils.commands.Command;
import io.github.flibio.utils.commands.ParentCommand;
import io.github.flibio.utils.message.MessageStorage;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.spec.CommandSpec.Builder;
import org.spongepowered.api.entity.living.player.Player;

import java.util.UUID;

@AsyncCommand
@ParentCommand(parentCommand = LoanCommand.class)
@Command(aliases = {"accept"}, permission = "economylite.loan.accept")
public class LoanAcceptCommand extends BaseCommandExecutor<Player> {

    private LoanModule module;
    private LoanManager manager;
    private MessageStorage messages;

    public LoanAcceptCommand(LoanModule module) {
        this.module = module;
        this.manager = module.getLoanManager();
        this.messages = EconomyLite.getMessageStorage();
    }

    @Override
    public Builder getCommandSpecBuilder() {
        return CommandSpec.builder()
                .executor(this);
    }

    @Override
    public void run(Player src, CommandContext args) {
        UUID uuid = src.getUniqueId();
        // Check if the player has a loan being offered
        if (module.tableLoans.containsKey(uuid)) {
            double amnt = module.tableLoans.get(uuid);
            if (manager.addLoanBalance(uuid, amnt)) {
                // Remove from the table
                module.tableLoans.remove(uuid);
                src.sendMessage(messages.getMessage("module.loan.yes"));
            } else {
                src.sendMessage(messages.getMessage("module.loan.fail"));
            }
        } else {
            src.sendMessage(messages.getMessage("module.loan.noloan"));
        }
    }

}
