package org.osbot.maestro.script.nodetasks;

import org.osbot.maestro.framework.NodeTask;
import org.osbot.maestro.framework.Priority;
import org.osbot.maestro.script.slayer.data.SlayerVariables;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.ui.Tab;
import org.osbot.rs07.utility.ConditionalSleep;


public class TaskValidator extends NodeTask {

    public TaskValidator() {
        super(Priority.URGENT);
    }

    @Override
    public boolean runnable() {
        if (SlayerVariables.currentTask == null ? true : SlayerVariables.currentTask.isFinished()) {
            return provider.getInventory().contains("Enchanted Gem");
        }
        return false;
    }

    @Override
    public void execute() {
        if (provider.getTabs().open(Tab.INVENTORY)) {
            if (provider.getInventory().getSelectedItemName() == null) {
                Item gem = provider.getInventory().getItem("Enchanted Gem");
                if (gem != null) {
                    gem.interact("Check");
                    new ConditionalSleep(3000, 1000) {

                        @Override
                        public boolean condition() throws InterruptedException {
                            return SlayerVariables.currentTask != null && !SlayerVariables.currentTask.isFinished();
                        }
                    }.sleep();
                }
            } else {
                provider.getInventory().deselectItem();
            }
        }
    }

}