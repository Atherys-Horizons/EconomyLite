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
package io.github.flibio.economylite.api;

import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.VirtualAccount;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interfaces directly with the storage system. Performs no checks, simply does
 * exactly what it is told.
 */
public interface VirtualEconService {

    /**
     * Gets the balance of an account.
     * 
     * @param id The name of the account to get the balance of.
     * @param currency The currency to use.
     * @param cause What is getting the balance.
     * @return The balance of the account.
     */
    public BigDecimal getBalance(String id, Currency currency, Cause cause);

    /**
     * Sets the balance of an account.
     * 
     * @param id The String of the player to set the balance of.
     * @param balance The new balance of the id.
     * @param currency The currency to use.
     * @param cause What caused the balance change.
     * @return If the player's balance was changed successfully.
     */
    public boolean setBalance(String id, BigDecimal balance, Currency currency, Cause cause);

    /**
     * Removes currency from an accounts's balance.
     * 
     * @param id The name of the account to remove currency from.
     * @param amount The amount of currency to remove.
     * @param currency The currency to use.
     * @param cause What caused the balance change.
     * @return If the account's balance was changed successfully.
     */
    default public boolean withdraw(String id, BigDecimal amount, Currency currency, Cause cause) {
        return setBalance(id, getBalance(id, currency, cause).subtract(amount), currency, cause);
    }

    /**
     * Adds currency to an account's balance.
     * 
     * @param id The name of the account to add currency to.
     * @param amount The amount of currency to add.
     * @param currency The currency to use.
     * @param cause What caused the balance change.
     * @return If the account's balance was changed successfully.
     */
    default public boolean deposit(String id, BigDecimal amount, Currency currency, Cause cause) {
        return setBalance(id, getBalance(id, currency, cause).add(amount), currency, cause);
    }

    /**
     * Checks if an account exists in the system.
     * 
     * @param id The name of the account to check for.
     * @param cause What is checking if the account exists.
     * @return If the account exists or not.
     */
    public boolean accountExists(String id, Cause cause);

    /**
     * Checks if an account exists in the system for the specified currency.
     * 
     * @param id The name of the account to check for.
     * @param currency The currency to check against.
     * @param cause What is checking if the account exists.
     * @return If the account exists or not.
     */
    public boolean accountExists(String id, Currency currency, Cause cause);

    /**
     * Clears a currency from the database.
     * 
     * @param currency The currency to clear.
     * @param cause What is checking if the account exists.
     */
    public void clearCurrency(Currency currency, Cause cause);

    /**
     * Gets the top virtual accounts registered in the EconomyLite system.
     * 
     * @param start The starting account to get.
     * @param end The ending account to get.
     * @param cause What is checking if the account exists.
     * @return The top virtual accounts registered in the EconomyLite system.
     */
    public List<VirtualAccount> getTopAccounts(int start, int end, Cause cause);

}
