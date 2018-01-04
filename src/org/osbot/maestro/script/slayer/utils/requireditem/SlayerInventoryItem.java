package org.osbot.maestro.script.slayer.utils.requireditem;

import org.osbot.maestro.script.slayer.data.SlayerVariables;
import org.osbot.maestro.script.slayer.task.Monster;
import org.osbot.rs07.api.filter.Filter;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.script.MethodProvider;

public class SlayerInventoryItem extends SlayerItem {


    public static final SlayerInventoryItem BAG_OF_SALT = new SlayerInventoryItem("Bag of salt", 250, true);
    public static final SlayerInventoryItem ANTIDOTE = new SlayerInventoryItem("Antidote", 1, true, new ItemRequired() {
        @Override
        public boolean required(MethodProvider provider) {
            return SlayerVariables.currentTask.getMonster() == Monster.ROCKSLUGS && provider.myPlayer().getCombatLevel() < 46;
        }
    });
    public static final SlayerInventoryItem ANTIPOISON = new SlayerInventoryItem("Antipoison", 1, true, new ItemRequired() {
        @Override
        public boolean required(MethodProvider provider) {
            return SlayerVariables.currentTask.getMonster() == Monster.ROCKSLUGS && provider.myPlayer().getCombatLevel() < 46;
        }
    });


    private final int amount;
    private final boolean stackable;

    public SlayerInventoryItem(String name, int amount, boolean stackable, ItemRequired condition) {
        super(name, condition);
        this.amount = amount;
        this.stackable = stackable;
    }

    public SlayerInventoryItem(String name, int amount, boolean stackable) {
        super(name);
        this.amount = amount;
        this.stackable = stackable;
    }

    public boolean isStackable() {
        return stackable;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public boolean hasItem(MethodProvider provider) {
        return provider.getInventory().contains(new Filter<Item>() {
            @Override
            public boolean match(Item item) {
                return item.getName().contains(name);
            }
        });
    }

    @Override
    public int getCount(MethodProvider provider) {
        return provider.getInventory().getItem(name).getAmount();
    }

}
