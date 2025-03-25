package org.jlortiz.playercollars.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.accessories.api.AccessoriesCapability;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.effect.EnchantmentEntityEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import org.jlortiz.playercollars.OwnerComponent;
import org.jlortiz.playercollars.PlayerCollarsMod;

import java.util.List;
import java.util.Optional;

public record RegenerationEnchantmentEffect(EnchantmentLevelBasedValue level) implements EnchantmentEntityEffect {
    public static final MapCodec<RegenerationEnchantmentEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                EnchantmentLevelBasedValue.CODEC.fieldOf("level").forGetter(RegenerationEnchantmentEffect::level)
            ).apply(instance, RegenerationEnchantmentEffect::new));

    @Override
    public void apply(ServerWorld world, int level, EnchantmentEffectContext context, Entity user, Vec3d pos) {
        
        //Optional<List<Pair<SlotReference, ItemStack>>> o = TrinketsApi.getTrinketComponent(context.owner()).map((x) -> x.getEquipped(PlayerCollarsMod.COLLAR_ITEM));
        try {
            ItemStack collarItemstack = AccessoriesCapability.get((LivingEntity) user).getFirstEquipped(PlayerCollarsMod.COLLAR_ITEM).stack();
            OwnerComponent ownerComponent = collarItemstack.get(PlayerCollarsMod.OWNER_COMPONENT_TYPE);
            if (ownerComponent != null) {
                PlayerEntity owner = world.getPlayerByUuid(ownerComponent.uuid());
                if (owner != null && owner.distanceTo(user) < 16) {
                    context.owner().addStatusEffect(new StatusEffectInstance(
                        StatusEffects.REGENERATION,
                       40, 
                        level, 
                        false, 
                        false,  
                        false));
                }
            }
        } catch (Exception e) {
            System.out.println("PlayerCollars::RegenerationEnchantmentEffect::apply - failed: " + e);
        }
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> getCodec() {
        return CODEC;
    }
}
