package com.easetrading.api.broker;

import com.easetrading.api.account.AccountService;
import com.easetrading.api.account.AccountService.AccountView;
import com.easetrading.api.account.AccountService.LinkRequest;
import com.easetrading.api.broker.BrokerModels.BrokerFunds;
import com.easetrading.api.broker.BrokerModels.BrokerStatus;
import com.easetrading.api.common.CurrentUserService;
import org.springframework.web.bind.annotation.*;

/**
 * The logged-in user's broker account.
 *   GET    /api/broker/status     -> linked? connected? what to do next
 *   POST   /api/broker/reconnect  -> log in to Groww again now (after approving the key)
 *   GET    /api/broker/funds      -> available cash and margin
 *   GET    /api/broker/account    -> which key is saved (last 4 chars only)
 *   PUT    /api/broker/account    -> save/replace my Groww key + secret (stored encrypted)
 *   DELETE /api/broker/account    -> remove my Groww keys
 *
 * Keys are accepted here once and never returned.
 */
@RestController
@RequestMapping("/api/broker")
public class BrokerController {

    private final AccountService accounts;
    private final CurrentUserService currentUser;

    public BrokerController(AccountService accounts, CurrentUserService currentUser) {
        this.accounts = accounts;
        this.currentUser = currentUser;
    }

    @GetMapping("/status")
    public BrokerStatus status() {
        return accounts.status(currentUser.currentUserId());
    }

    @PostMapping("/reconnect")
    public BrokerStatus reconnect() {
        return accounts.reconnect(currentUser.currentUserId());
    }

    @GetMapping("/funds")
    public BrokerFunds funds() {
        return accounts.funds(currentUser.currentUserId());
    }

    @GetMapping("/account")
    public AccountView account() {
        return accounts.view(currentUser.currentUserId());
    }

    @PutMapping("/account")
    public BrokerStatus link(@RequestBody LinkRequest req) {
        return accounts.link(currentUser.currentUserId(), req);
    }

    @DeleteMapping("/account")
    public AccountView unlink() {
        accounts.unlink(currentUser.currentUserId());
        return accounts.view(currentUser.currentUserId());
    }
}
