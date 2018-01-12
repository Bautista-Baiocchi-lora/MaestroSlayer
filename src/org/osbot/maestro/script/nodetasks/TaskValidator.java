package org.osbot.maestro.script.nodetasks;

import org.osbot.maestro.framework.Broadcast;
import org.osbot.maestro.framework.BroadcastReceiver;
import org.osbot.maestro.framework.NodeTask;
import org.osbot.maestro.framework.Priority;
import org.osbot.maestro.script.data.RuntimeVariables;
import org.osbot.maestro.script.slayer.utils.WithdrawRequest;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.ui.Tab;
import org.osbot.rs07.utility.ConditionalSleep;


public class TaskValidator extends NodeTask implements BroadcastReceiver {

    private boolean forceCheck;

    public TaskValidator() {
        super(Priority.URGENT);
        registerBroadcastReceiver(this);
    }

    @Override
    public boolean runnable() {
        if (RuntimeVariables.currentTask == null || forceCheck) {
            if (provider.getInventory().contains("Enchanted gem")) {
                return true;
            } else {
                sendBroadcast(new Broadcast("bank-withdraw-request", new WithdrawRequest("Enchanted gem", 1, false, true, true)));
            }
        }
        return false;
    }

    @Override
    public void execute() {
        if (provider.getTabs().open(Tab.INVENTORY)) {
            if (provider.getInventory().getSelectedItemName() == null) {
                Item gem = provider.getInventory().getItem("Enchanted Gem");
                if (gem != null) {
                    provider.log("Checking task");
                    gem.interact("Check");
                    new ConditionalSleep(3000, 1000) {

                        @Override
                        public boolean condition() throws InterruptedException {
                            return RuntimeVariables.currentTask != null;
                        }
                    }.sleep();
                }
                forceCheck = false;
            } else {
                provider.getInventory().deselectItem();
            }
        }
    }

    @Override
    public void receivedBroadcast(Broadcast broadcast) {
        switch (broadcast.getKey()) {
            case "check-kills-antiban":
                forceCheck = true;
                break;
        }
    }
}
