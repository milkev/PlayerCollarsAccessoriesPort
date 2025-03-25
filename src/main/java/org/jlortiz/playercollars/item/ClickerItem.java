package org.jlortiz.playercollars.item;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jlortiz.playercollars.PacketLookAtLerped;
import org.jlortiz.playercollars.PlayerCollarsMod;

import java.util.List;

public class ClickerItem extends Item {
    public ClickerItem() {
        super(new Item.Settings().maxCount(1));
    }

    @Override
    public boolean isEnchantable(ItemStack p_41456_) {
        return true;
    }

    @Override
    public int getEnchantability() {
        return 40;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand) {
        playerEntity.setCurrentHand(hand);
        if (!world.isClient) {
            double distance = playerEntity.getAttributeValue(PlayerCollarsMod.ATTR_CLICKER_DISTANCE);
            if (distance > 0) {
                List<ServerPlayerEntity> plrs = ((ServerWorld) world).getPlayers((p) -> !p.isPartOf(playerEntity) && p.isInRange(playerEntity, distance));
                PacketLookAtLerped packet = new PacketLookAtLerped(playerEntity);
                for (ServerPlayerEntity p : plrs) {
                    try {
                        ItemStack itemStack = AccessoriesCapability.get(p).getFirstEquipped(PlayerCollarsMod.COLLAR_ITEM).stack();
                        if (PlayerCollarsMod.stackOwnedBy(itemStack, p.getUuid())) {
                            ServerPlayNetworking.send(p, packet);
                        }
                    } catch (Exception e) {
                        System.out.println("Player Collars::ClickerItem::use - Failed try: " + e);
                    }
                }
            }
            world.playSoundFromEntity(null, playerEntity, PlayerCollarsMod.CLICKER_ON, SoundCategory.PLAYERS, 1, 1);
        }
        return TypedActionResult.fail(playerEntity.getStackInHand(hand));
    }

    @Override
    public int getMaxUseTime(ItemStack p_41454_, LivingEntity user) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void onStoppedUsing(ItemStack p_41412_, World p_41413_, LivingEntity p_41414_, int p_41415_) {
        if (!p_41413_.isClient) {
            p_41413_.playSoundFromEntity(null, p_41414_, PlayerCollarsMod.CLICKER_OFF, SoundCategory.PLAYERS, 1, 1);
        }
    }

    public int getColor(ItemStack itemStack) {
        DyedColorComponent $$1 = itemStack.get(DataComponentTypes.DYED_COLOR);
        return $$1 != null ? $$1.rgb() : -1;
    }
}
