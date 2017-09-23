/*
 * This file is part of EconomyLite, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2017 Flibio
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
package io.github.flibio.economylite.commands;

import io.github.flibio.economylite.TextUtils;

import io.github.flibio.economylite.EconomyLite;
import io.github.flibio.utils.commands.AsyncCommand;
import io.github.flibio.utils.commands.BaseCommandExecutor;
import io.github.flibio.utils.commands.Command;
import io.github.flibio.utils.message.MessageStorage;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.spec.CommandSpec.Builder;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.text.Text;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

@AsyncCommand
@Command(aliases = {"pay"}, permission = "economylite.pay")
public class PayCommand extends BaseCommandExecutor<Player> {

    private MessageStorage messageStorage = EconomyLite.getMessageStorage();
    private EconomyService ecoService = EconomyLite.getEconomyService();

    @Override
    public Builder getCommandSpecBuilder() {
        return CommandSpec.builder()
                .executor(this)
                .arguments(GenericArguments.user(Text.of("player")), GenericArguments.doubleNum(Text.of("amount")));
    }

    @Override
    public void run(Player src, CommandContext args) {
        if (args.getOne("player").isPresent() && args.getOne("amount").isPresent()) {
            BigDecimal amount = BigDecimal.valueOf(args.<Double>getOne("amount").get());
            if (amount.doubleValue() <= 0) {
                src.sendMessage(messageStorage.getMessage("command.pay.invalid"));
            } else {
                User target = args.<User>getOne("player").get();
                if (!EconomyLite.isEnabled("confirm-offline-payments") || target.isOnline()) {
                    // Complete the payment
                    pay(target, amount, src);
                } else {
                    src.sendMessage(messageStorage.getMessage("command.pay.confirm", "player", target.getName()));
                    // Check if they want to still pay
                    src.sendMessage(TextUtils.yesOrNo(c -> {
                        pay(target, amount, src);
                    }, c -> {
                        src.sendMessage(messageStorage.getMessage("command.pay.confirmno", "player", target.getName()));
                    }));
                }

            }
        } else {
            src.sendMessage(messageStorage.getMessage("command.error"));
        }
    }

    private void pay(User target, BigDecimal amount, Player src) {
        String targetName = target.getName();
        if (!target.getUniqueId().equals(src.getUniqueId())) {
            Optional<UniqueAccount> uOpt = ecoService.getOrCreateAccount(src.getUniqueId());
            Optional<UniqueAccount> tOpt = ecoService.getOrCreateAccount(target.getUniqueId());
            if (uOpt.isPresent() && tOpt.isPresent()) {
                if (uOpt.get()
                        .transfer(tOpt.get(), ecoService.getDefaultCurrency(), amount, Cause.of(EventContext.empty(),(EconomyLite.getInstance())))
                        .getResult().equals(ResultType.SUCCESS)) {
                    Text label = ecoService.getDefaultCurrency().getPluralDisplayName();
                    if (amount.equals(BigDecimal.ONE)) {
                        label = ecoService.getDefaultCurrency().getDisplayName();
                    }
                    src.sendMessage(messageStorage.getMessage("command.pay.success", "target", Text.of(targetName), "amountandlabel",
                            Text.of(String.format(Locale.ENGLISH, "%,.2f", amount) + " ").toBuilder().append(label).build()));
                    if (target instanceof Player) {
                        ((Player) target).sendMessage(messageStorage.getMessage("command.pay.target", "amountandlabel",
                                Text.of(String.format(Locale.ENGLISH, "%,.2f", amount) + " ").toBuilder().append(label).build(), "sender",
                                uOpt.get().getDisplayName()));
                    }
                } else {
                    src.sendMessage(messageStorage.getMessage("command.pay.failed", "target", targetName));
                }
            } else {
                src.sendMessage(messageStorage.getMessage("command.error"));
            }
        } else {
            src.sendMessage(messageStorage.getMessage("command.pay.notyou"));
        }
    }

}
