package com.ninjawars;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class NinjaEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> VILLAGE =
            SynchedEntityData.defineId(NinjaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RANK =
            SynchedEntityData.defineId(NinjaEntity.class, EntityDataSerializers.INT);

    private boolean invader = false;
    private BlockPos dest = null;
    private BlockPos home = null;
    private int jutsuCooldown = 60;

    public NinjaEntity(EntityType<? extends NinjaEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(VILLAGE, 0);
        entityData.define(RANK, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new JutsuGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::isEnemy));
    }

    public Village getVillage() { return Village.byIndex(entityData.get(VILLAGE)); }
    public int getRank() { return Math.max(0, Math.min(4, entityData.get(RANK))); }
    public boolean isInvader() { return invader; }

    public void setup(Village v, int rank) {
        entityData.set(VILLAGE, v.ordinal());
        entityData.set(RANK, rank);
        applyStats(true);
        equip();
        updateName();
    }

    public void setInvader(BlockPos destination) { invader = true; dest = destination; }

    public void setHome(BlockPos p) {
        home = p;
        restrictTo(p, 28);
    }

    private void updateName() {
        setCustomName(Component.literal(Ranks.NAMES[getRank()] + " de " + getVillage().display));
        setCustomNameVisible(getRank() >= 4);
        if (getRank() >= 4) setCustomName(Component.literal(getVillage().kageTitle + " de " + getVillage().display));
    }

    private void applyStats(boolean heal) {
        int r = getRank();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(20 + r * 10);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3 + r * 1.5);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.27 + r * 0.015);
        if (heal) setHealth(getMaxHealth());
    }

    private ItemStack dyed(Item item) {
        ItemStack s = new ItemStack(item);
        ((DyeableLeatherItem) item).setColor(s, getVillage().color);
        return s;
    }

    private void equip() {
        int r = getRank();
        setItemSlot(EquipmentSlot.HEAD, dyed(Items.LEATHER_HELMET));
        if (r >= 1) setItemSlot(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE));
        if (r >= 2) setItemSlot(EquipmentSlot.LEGS, dyed(Items.LEATHER_LEGGINGS));
        if (r >= 3) setItemSlot(EquipmentSlot.FEET, dyed(Items.LEATHER_BOOTS));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(r >= 3 ? Items.DIAMOND_SWORD : r >= 1 ? Items.IRON_SWORD : Items.STONE_SWORD));
        for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0f);
    }

    public boolean isEnemy(LivingEntity e) {
        if (e == this || !e.isAlive()) return false;
        if (e instanceof Monster) return true;
        if (!(level() instanceof ServerLevel sl)) return false;
        WarData d = WarData.get(sl);
        if (e instanceof NinjaEntity n) return n.getVillage() != getVillage() && d.atWar(getVillage(), n.getVillage());
        if (e instanceof ServerPlayer p) {
            Village pv = PlayerData.village(p);
            return pv != null && pv != getVillage() && d.atWar(getVillage(), pv) && !p.isCreative() && !p.isSpectator();
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        Entity e = src.getEntity();
        if (e instanceof NinjaEntity n && n.getVillage() == getVillage()) return false;
        if (e instanceof ServerPlayer p && PlayerData.village(p) == getVillage()) return false;
        return super.hurt(src, amount);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(src, looting, recentlyHit);
        if (random.nextFloat() < 0.25f + 0.1f * getRank()) spawnAtLocation(Items.EMERALD);
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && invader && dest != null && getTarget() == null
                && tickCount % 40 == 0 && distanceToSqr(Vec3.atCenterOf(dest)) > 36) {
            getNavigation().moveTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, 1.1);
        }
        if (jutsuCooldown > 0) jutsuCooldown--;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("nwVillage", entityData.get(VILLAGE));
        tag.putInt("nwRank", entityData.get(RANK));
        tag.putBoolean("nwInvader", invader);
        if (dest != null) tag.put("nwDest", NbtUtils.writeBlockPos(dest));
        if (home != null) tag.put("nwHome", NbtUtils.writeBlockPos(home));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(VILLAGE, tag.getInt("nwVillage"));
        entityData.set(RANK, tag.getInt("nwRank"));
        applyStats(false);
        super.readAdditionalSaveData(tag);
        invader = tag.getBoolean("nwInvader");
        if (tag.contains("nwDest")) dest = NbtUtils.readBlockPos(tag.getCompound("nwDest"));
        if (tag.contains("nwHome")) setHome(NbtUtils.readBlockPos(tag.getCompound("nwHome")));
        updateName();
    }

    // ---------- Jutsu ----------
    private void castJutsu(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Village v = getVillage();
        float dmg = 2f + getRank() * 1.5f;
        AABB box = target.getBoundingBox().inflate(3);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, x -> x != this && isEnemy(x))) {
            e.hurt(damageSources().indirectMagic(this, this), dmg);
            switch (v.jutsu) {
                case FIRE -> e.setSecondsOnFire(4);
                case WIND -> {
                    Vec3 push = e.position().subtract(position()).normalize().scale(1.2);
                    e.push(push.x, 0.5, push.z);
                    e.hurtMarked = true;
                }
                case WATER -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                case LIGHTNING -> e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                case EARTH -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
            }
        }
        sl.sendParticles(v.particle, target.getX(), target.getY() + 1, target.getZ(), 40, 1.2, 0.6, 1.2, 0.05);
        if (v.jutsu == Village.Jutsu.LIGHTNING) {
            LightningBolt b = EntityType.LIGHTNING_BOLT.create(sl);
            if (b != null) { b.moveTo(target.position()); b.setVisualOnly(true); sl.addFreshEntity(b); }
        }
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1f, 1f);
        jutsuCooldown = Math.max(60, 200 - getRank() * 30);
    }

    private class JutsuGoal extends Goal {
        JutsuGoal() { setFlags(EnumSet.noneOf(Flag.class)); }

        @Override
        public boolean canUse() {
            LivingEntity t = getTarget();
            return t != null && t.isAlive() && jutsuCooldown <= 0
                    && distanceToSqr(t) < 144 && getSensing().hasLineOfSight(t);
        }

        @Override
        public void start() {
            LivingEntity t = getTarget();
            if (t != null) castJutsu(t);
        }
    }
}
