package org.valkyrienskies.clockwork.content.curiosities.tools.pastrymaker;

import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.Create;
import com.simibubi.create.content.curiosities.weapons.*;
import com.simibubi.create.foundation.item.CustomArmPoseItem;
import dev.architectury.injectables.annotations.PlatformOnly;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.simibubi.create.content.curiosities.armor.BackTankUtil;
import com.simibubi.create.content.curiosities.zapper.ShootableGadgetItemMethods;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.item.CustomArmPoseItem;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VecHelper;
import org.valkyrienskies.clockwork.ClockWorkHandlers;
import org.valkyrienskies.clockwork.ClockWorkMod;

import java.util.Optional;

import static dev.architectury.injectables.annotations.PlatformOnly.FORGE;

public class PastrymakerItem extends PotatoCannonItem implements CustomArmPoseItem {

    public static final int MAX_DAMAGE = 300;
    private int maxUses() {
        return AllConfigs.SERVER.curiosities.maxPotatoCannonShots.get() * 2;
    }
    @Override
    public boolean isCannon(ItemStack stack) {
        return stack.getItem() instanceof PastrymakerItem;
    }

    public PastrymakerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return findAmmoInInventory(world, player, stack).map(itemStack -> {

                    if (ShootableGadgetItemMethods.shouldSwap(player, stack, hand, this::isCannon))
                        return InteractionResultHolder.fail(stack);

                    if (world.isClientSide) {
                        ClockWorkHandlers.PASTRYMAKER_RENDER_HANDLER.dontAnimateItem(hand);
                        return InteractionResultHolder.success(stack);
                    }

                    Vec3 barrelPos = ShootableGadgetItemMethods.getGunBarrelVec(player, hand == InteractionHand.MAIN_HAND,
                            new Vec3(.75f, -0.15f, 1.5f));
                    Vec3 correction =
                            ShootableGadgetItemMethods.getGunBarrelVec(player, hand == InteractionHand.MAIN_HAND, new Vec3(-.05f, 0, 0))
                                    .subtract(player.position()
                                            .add(0, player.getEyeHeight(), 0));

                    PotatoCannonProjectileType projectileType = PotatoProjectileTypeManager.getTypeForStack(itemStack)
                            .orElse(BuiltinPotatoProjectileTypes.FALLBACK);
                    Vec3 lookVec = player.getLookAngle();
                    Vec3 motion = lookVec.add(correction)
                            .normalize()
                            .scale(2)
                            .scale(projectileType.getVelocityMultiplier());

                    float soundPitch = projectileType.getSoundPitch() + (Create.RANDOM.nextFloat() - .5f) / 4f;

                    boolean spray = projectileType.getSplit() > 1;
                    Vec3 sprayBase = VecHelper.rotate(new Vec3(0, 0.1, 0), 360 * Create.RANDOM.nextFloat(), Axis.Z);
                    float sprayChange = 360f / projectileType.getSplit();

                    for (int i = 0; i < projectileType.getSplit(); i++) {
                        PotatoProjectileEntity projectile = AllEntityTypes.POTATO_PROJECTILE.create(world);
                        projectile.setItem(itemStack);
                        projectile.setEnchantmentEffectsFromCannon(stack);

                        Vec3 splitMotion = motion;
                        if (spray) {
                            float imperfection = 40 * (Create.RANDOM.nextFloat() - 0.5f);
                            Vec3 sprayOffset = VecHelper.rotate(sprayBase, i * sprayChange + imperfection, Axis.Z);
                            splitMotion = splitMotion.add(VecHelper.lookAt(sprayOffset, motion));
                        }

                        projectile.setPos(barrelPos.x, barrelPos.y, barrelPos.z);
                        projectile.setDeltaMovement(splitMotion);
                        projectile.setOwner(player);
                        world.addFreshEntity(projectile);
                    }

                    if (!player.isCreative()) {
                        itemStack.shrink(1);
                        if (itemStack.isEmpty())
                            player.getInventory().removeItem(itemStack);
                    }

                    if (!BackTankUtil.canAbsorbDamage(player, maxUses()))
                        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));

                    Integer cooldown =
                            findAmmoInInventory(world, player, stack).flatMap(PotatoProjectileTypeManager::getTypeForStack)
                                    .map(PotatoCannonProjectileType::getReloadTicks)
                                    .orElse(10);

                    cooldown /= 5;

                    ShootableGadgetItemMethods.applyCooldown(player, stack, hand, this::isCannon, cooldown);
                    ShootableGadgetItemMethods.sendPackets(player,
                            b -> new PastrymakerPacket(barrelPos, lookVec.normalize(), itemStack, hand, soundPitch, b));
                    return InteractionResultHolder.success(stack);
                })
                .orElse(InteractionResultHolder.pass(stack));
    }

    private Optional<ItemStack> findAmmoInInventory(Level world, Player player, ItemStack held) {
        ItemStack findAmmo = player.getProjectile(held);
        return PotatoProjectileTypeManager.getTypeForStack(findAmmo)
                .map($ -> findAmmo);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || newStack.getItem() != oldStack.getItem();
    }

    @Override
    public int getDefaultProjectileRange() {
        return 45;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

//    @PlatformOnly(FORGE)
//    @Override
//    @Environment(EnvType.CLIENT)
//    public void initializeClient(Consumer<IItemRenderProperties> consumer) {
//        consumer.accept(SimpleCustomRenderer.create(this, new PotatoCannonItemRenderer()));
//    }

}
