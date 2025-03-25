package org.jlortiz.playercollars.client;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.client.SimpleAccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.joml.Quaternionf;

public class CollarRenderer implements SimpleAccessoryRenderer {

    public static void register() {
        AccessoriesRendererRegistry.registerRenderer(PlayerCollarsMod.COLLAR_ITEM, CollarRenderer::new);
    }

    @Override
    public <M extends LivingEntity> void align(ItemStack itemStack, io.wispforest.accessories.api.slot.SlotReference slotReference, EntityModel<M> entityModel, MatrixStack matrixStack) {
        try {
            ModelPart body = ((PlayerEntityModel<?>) entityModel).body;
            AccessoryRenderer.transformToModelPart(matrixStack, body);
            matrixStack.multiply(new Quaternionf().rotateXYZ(0f, (float) Math.PI, 0f));
            matrixStack.translate(0f, 0.45f, -0.25f);
        } catch (ClassCastException ignored) {}
    }
}
