package org.jlortiz.playercollars.leash.mixin;

import io.wispforest.accessories.api.AccessoriesCapability;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.leash.LeashImpl;
import org.jlortiz.playercollars.leash.LeashProxyEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity implements LeashImpl {
    @Unique
    private final ServerPlayerEntity leashplayers$self = (ServerPlayerEntity) (Object) this;

    @Unique
    private LeashProxyEntity leashplayers$proxy;
    @Unique
    private Entity leashplayers$holder;

    @Unique
    private int leashplayers$lastage;

    @Unique
    private double leashplayer$loyalty;


    @Unique
    private void leashplayers$update() {
        if (
                leashplayers$holder != null && (
                        !leashplayers$holder.isAlive()
                                || !leashplayers$self.isAlive()
                                || leashplayers$self.isDisconnected()
                                || leashplayers$self.hasVehicle()
                )
        ) {
            leashplayers$detach();
            leashplayers$drop();
        }

        if (leashplayers$proxy != null) {
            if (leashplayers$proxy.proxyIsRemoved()) {
                leashplayers$proxy = null;
            }
            else {
                Entity holderActual = leashplayers$holder;
                Entity holderTarget = leashplayers$proxy.getLeashHolder();

                if (holderTarget == null && holderActual != null) {
                    leashplayers$detach();
                    leashplayers$drop();
                }
                else if (holderTarget != holderActual) {
                    leashplayers$attach(holderTarget);
                }
            }
        }

        leashplayers$apply();
    }

    @Unique
    private void leashplayers$apply() {
        ServerPlayerEntity player = leashplayers$self;
        Entity holder = leashplayers$holder;
        if (holder == null) return;
        if (holder.getWorld() != player.getWorld()) return;

        float distance = player.distanceTo(holder);
        if (distance < leashplayer$loyalty) {
            return;
        }
        if (distance > 6 + leashplayer$loyalty) {
            leashplayers$detach();
            leashplayers$drop();
            return;
        }

        double dx = (holder.getX() - player.getX()) / (double) distance;
        double dy = (holder.getY() - player.getY()) / (double) distance;
        double dz = (holder.getZ() - player.getZ()) / (double) distance;
        final double factor = 0.4d + 0.1d * leashplayer$loyalty;

        player.addVelocity(
                Math.copySign(dx * dx * factor, dx),
                Math.copySign(dy * dy * factor, dy),
                Math.copySign(dz * dz * factor, dz)
        );

        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        player.velocityDirty = false;
    }

    @Unique
    private void leashplayers$attach(Entity entity) {
        leashplayers$holder = entity;

        if (leashplayers$proxy == null) {
            leashplayers$proxy = new LeashProxyEntity(leashplayers$self);
            leashplayers$proxy.setPos(leashplayers$self.getX(), leashplayers$self.getY(), leashplayers$self.getZ());
            leashplayers$self.getWorld().spawnEntity(leashplayers$proxy);
        }
        leashplayers$proxy.attachLeash(leashplayers$holder, true);

        if (leashplayers$self.hasVehicle()) {
            leashplayers$self.stopRiding();
        }

        leashplayers$lastage = leashplayers$self.age;
    }

    @Unique
    private void leashplayers$detach() {
        leashplayers$holder = null;

        if (leashplayers$proxy != null) {
            if (leashplayers$proxy.isAlive() || !leashplayers$proxy.proxyIsRemoved()) {
                leashplayers$proxy.proxyRemove();
            }
            leashplayers$proxy = null;
        }
    }

    @Unique
    private void leashplayers$drop() {
        leashplayers$self.dropItem(new ItemStack(Items.LEAD), false, true);
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void leashplayers$tick(CallbackInfo info) {
        leashplayers$update();
    }

    @Override
    public ActionResult leashplayers$interact(PlayerEntity leasher, Hand hand) {
        ItemStack stack = leasher.getStackInHand(hand);
        if (stack.getItem() == Items.LEAD && leashplayers$holder == null) {
            AtomicBoolean found = new AtomicBoolean(false);
            try{
                ItemStack collarItem = AccessoriesCapability.get((PlayerEntity) (Object) this).getFirstEquipped(PlayerCollarsMod.COLLAR_ITEM).stack();
                if(PlayerCollarsMod.stackOwnedBy(collarItem, leasher.getUuid())) {
                    found.set(true);
                    leashplayer$loyalty = ((PlayerEntity) (Object) this).getAttributeValue(PlayerCollarsMod.ATTR_LEASH_DISTANCE);
                }
            } catch (Exception e) {
                System.out.println("PlayerCollars::MixinServerPlayerEntity::leashPlayers$interact - failed: " + e);
            }
            if (!found.get()) return ActionResult.PASS;
            if (!leasher.isCreative()) {
                stack.decrement(1);
            }
            leashplayers$attach(leasher);
            return ActionResult.SUCCESS;
        }

        if (leashplayers$holder == leasher && leashplayers$lastage + 20 < leashplayers$self.age) {
            if (!leasher.isCreative()) {
                leashplayers$drop();
            }
            leashplayers$detach();
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Inject(at=@At("TAIL"), method="damage")
    private void checkCollarThorns(DamageSource damageSource, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (damageSource.getAttacker() != null) {
            LivingEntity self = ((LivingEntity) (Object) this);
            try{
                ItemStack collarItemstack = AccessoriesCapability.get(self).getFirstEquipped(PlayerCollarsMod.COLLAR_ITEM).stack();
                EnchantmentHelper.onTargetDamaged((ServerWorld) self.getWorld(), damageSource.getAttacker(), damageSource, collarItemstack);
            } catch (Exception e) {
                System.out.println("PlayerCollars::MixingServerPlayerEntity::checkCollarThorns - failed: " + e);
            }
        }
    }
}