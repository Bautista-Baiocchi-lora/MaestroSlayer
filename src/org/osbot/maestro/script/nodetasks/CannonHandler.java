package org.osbot.maestro.script.nodetasks;

import org.osbot.maestro.framework.Broadcast;
import org.osbot.maestro.framework.NodeTimeTask;
import org.osbot.maestro.framework.Priority;
import org.osbot.maestro.framework.Response;
import org.osbot.maestro.script.data.RuntimeVariables;
import org.osbot.maestro.script.slayer.utils.CannonPlacementException;
import org.osbot.rs07.api.filter.Filter;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.RS2Object;
import org.osbot.rs07.api.ui.Message;
import org.osbot.rs07.event.WalkingEvent;
import org.osbot.rs07.listener.MessageListener;
import org.osbot.rs07.utility.Condition;
import org.osbot.rs07.utility.ConditionalSleep;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CannonHandler extends NodeTimeTask implements MessageListener {

    private static final int CANNON_ID = 6;
    private static final int BROKEN_CANNON_ID = 14916;
    private static final int CANNON_PARENT_CONFIG_ID = 1;
    private static final int FIRING_CANNON_CONFIG_CHILD = 1048576;
    private boolean needRepair, cannonSet, needLoad, needPickUp;

    public CannonHandler() {
        super(15, 3, TimeUnit.SECONDS, Priority.MEDIUM);
    }

    @Override
    public Response runnable() throws InterruptedException {
        if (RuntimeVariables.currentTask.getCurrentMonster().canCannon()) {
            if (isCannonSet()) {
                if (super.runnable() == Response.EXECUTE || needReload() || needRepair || needPickUp) {
                    return Response.EXECUTE;
                }
            } else if (!isCannonSet() && !holdingCannon()) {
                //request cannon urgent
                sendBroadcast(new Broadcast("bank-request"));
            } else if (!isCannonSet()) {
                return Response.EXECUTE;
            }
        }
        return Response.CONTINUE;
    }

    @Override
    protected void execute() throws InterruptedException {
        if (!isCannonSet()) {
            if (!RuntimeVariables.currentTask.getCurrentMonster().getCannonPosition().equals(provider.myPosition())) {
                provider.log("Walking to cannon position");
                walkToCannonPosition();
            }
            Item cannonBase = provider.getInventory().getItem("Cannon base");
            if (cannonBase != null) {
                provider.log("Setting up cannon");
                cannonBase.interact("Set-up");
                new ConditionalSleep(10000, 600) {

                    @Override
                    public boolean condition() throws InterruptedException {
                        return isCannonSet();
                    }
                }.sleep();
            }
            needLoad = true;
        }
        if (isCannonSet()) {
            if (needRepair) {
                repairCannon();
            } else if (needReload()) {
                reloadCannon();
                super.execute();
            } else if (needPickUp) {
                pickUpCannon();
            }
        }
    }

    private boolean interactWithCannon(String action, boolean condition) {
        RS2Object cannon = getCannon();
        if (cannon != null && cannon.exists()) {
            if (cannon.hasAction(action)) {
                cannon.interact(action);
                new ConditionalSleep(5000, 500) {

                    @Override
                    public boolean condition() throws InterruptedException {
                        return condition;
                    }
                }.sleep();
                return true;
            }
        }
        return false;
    }

    private boolean needReload() {
        return needLoad || !isCannonFiring();
    }

    private boolean isCannonFiring() {
        return provider.getConfigs().get(CANNON_PARENT_CONFIG_ID) == FIRING_CANNON_CONFIG_CHILD;
    }

    private void repairCannon() {
        provider.log("Repairing cannon");
        if (interactWithCannon("Repair", !needRepair)) {
            provider.log("Cannon repaired");
            needRepair = false;
            return;
        }
        needRepair = true;
    }

    private void reloadCannon() {
        provider.log("Reloading cannon");
        if (provider.getInventory().contains("Cannonball")) {
            if (interactWithCannon("Fire", !needReload())) {
                provider.log("Cannon reloaded");
            }
        } else {
            provider.log("Out of cannonballs");
        }
    }

    private void pickUpCannon() {
        provider.log("Picking up cannon");
        if (interactWithCannon("Pick-Up", !isCannonSet())) {
            provider.log("Cannon picked up");
        }
    }

    private void walkToCannonPosition() {
        WalkingEvent walkingEvent = new WalkingEvent(RuntimeVariables.currentTask.getCurrentMonster().getCannonPosition().unwrap());
        walkingEvent.setMiniMapDistanceThreshold(4);
        walkingEvent.setBreakCondition(new Condition() {
            @Override
            public boolean evaluate() {
                return RuntimeVariables.currentTask.getCurrentMonster().getCannonPosition().equals(provider.myPosition());
            }
        });
        provider.execute(walkingEvent);
    }

    private RS2Object getCannon() {
        return provider.getObjects().closest(new Filter<RS2Object>() {
            @Override
            public boolean match(RS2Object rs2Object) {
                return rs2Object != null && rs2Object.exists() && rs2Object.getId() == (needRepair ? BROKEN_CANNON_ID : CANNON_ID) &&
                        RuntimeVariables.currentTask.getCurrentMonster().getCannonPosition().equals(rs2Object.getPosition());
            }
        });
    }

    private boolean holdingCannon() {
        List<Item> cannonParts = provider.getInventory().filter(new Filter<Item>() {
            @Override
            public boolean match(Item item) {
                return item.getName().contains("Cannon") && !item.getName().equalsIgnoreCase("Cannonball");
            }
        });
        return cannonParts.size() == 4;
    }

    private boolean isCannonSet() {
        return getCannon() != null && !holdingCannon() && cannonSet;
    }

    @Override
    public void onMessage(Message message) throws InterruptedException {
        switch (message.getType()) {
            case GAME:
                if (message.getMessage().toLowerCase().contains("your cannon has broken")) {
                    needRepair = true;
                    cannonSet = true;
                } else if (message.getMessage().toLowerCase().contains("cannon is out of ammo")) {
                    needLoad = true;
                    cannonSet = true;
                } else if (message.getMessage().contains("you repair your cannon")) {
                    needRepair = false;
                    cannonSet = true;
                } else if (message.getMessage().contains("load the cannon with")) {
                    needLoad = false;
                    cannonSet = true;
                } else if (message.getMessage().contains("cannon already firing")) {
                    needLoad = false;
                    cannonSet = true;
                } else if (message.getMessage().contains("you pick up your cannon")) {
                    cannonSet = false;
                } else if (message.getMessage().contains("there isn't enough space to set up here")) {
                    try {
                        throw new CannonPlacementException(RuntimeVariables.currentTask.getCurrentMonster().getCannonPosition().unwrap());
                    } catch (CannonPlacementException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }
}
