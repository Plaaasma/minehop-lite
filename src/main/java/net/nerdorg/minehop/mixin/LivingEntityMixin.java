// ORIGINAL BY hatninja ON GITHUB

package net.nerdorg.minehop.mixin;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.nerdorg.minehop.Minehop;
import net.nerdorg.minehop.config.MinehopConfig;
import net.nerdorg.minehop.config.ConfigWrapper;
import net.nerdorg.minehop.util.MovementUtil;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow private float movementSpeed;
    @Shadow public float sidewaysSpeed;
    @Shadow public float forwardSpeed;
    @Shadow private int jumpingCooldown;
    @Shadow protected boolean jumping;

    @Shadow protected abstract Vec3d applyClimbingSpeed(Vec3d velocity);
    @Shadow protected abstract float getJumpVelocity();
    @Shadow public abstract boolean hasStatusEffect(StatusEffect effect);
    @Shadow public abstract StatusEffectInstance getStatusEffect(StatusEffect effect);
    @Shadow public abstract boolean isFallFlying();
    @Shadow public abstract boolean isClimbing();

    @Shadow public abstract float getYaw(float tickDelta);

    @Shadow public abstract void updateLimbs(boolean flutter);

    @Shadow public float prevHeadYaw;

    @Shadow public abstract float getHeadYaw();

    @Shadow public abstract boolean isDead();

    @Shadow public abstract boolean isSleeping();

    @Shadow public abstract void wakeUp();

    @Shadow protected int despawnCounter;

    @Shadow public abstract boolean blockedByShield(DamageSource source);

    @Shadow public abstract void damageShield(float amount);

    @Shadow protected abstract void takeShieldHit(LivingEntity attacker);

    @Shadow @Final public LimbAnimator limbAnimator;
    @Shadow protected float lastDamageTaken;

    @Shadow protected abstract void applyDamage(DamageSource source, float amount);

    @Shadow public int maxHurtTime;
    @Shadow public int hurtTime;

    @Shadow public abstract ItemStack getEquippedStack(EquipmentSlot slot);

    @Shadow public abstract void damageHelmet(DamageSource source, float amount);

    @Shadow public abstract void setAttacker(@Nullable LivingEntity attacker);

    @Shadow protected int playerHitTimer;
    @Shadow @Nullable protected PlayerEntity attackingPlayer;

    @Shadow public abstract void takeKnockback(double strength, double x, double z);

    @Shadow public abstract void tiltScreen(double deltaX, double deltaZ);

    @Shadow protected abstract boolean tryUseTotem(DamageSource source);

    @Shadow @Nullable protected abstract SoundEvent getDeathSound();

    @Shadow protected abstract float getSoundVolume();

    @Shadow public abstract float getSoundPitch();

    @Shadow public abstract void onDeath(DamageSource damageSource);

    @Shadow protected abstract void playHurtSound(DamageSource source);

    @Shadow @Nullable private DamageSource lastDamageSource;
    @Shadow private long lastDamageTime;
    private boolean wasOnGround;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    public void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        MinehopConfig config = ConfigWrapper.config;

        if (source.isOf(DamageTypes.FALL) && !config.fall_damage) {
            cir.cancel();
        }
        else {
            if (this.isInvulnerableTo(source)) {
                cir.setReturnValue(false);
            } else if (this.getWorld().isClient) {
                cir.setReturnValue(false);
            } else if (this.isDead()) {
                cir.setReturnValue(false);
            } else if (source.isIn(DamageTypeTags.IS_FIRE) && this.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                cir.setReturnValue(false);
            } else {
                if (this.isSleeping() && !this.getWorld().isClient) {
                    this.wakeUp();
                }

                this.despawnCounter = 0;
                float f = amount;
                boolean bl = false;
                float g = 0.0F;
                if (amount > 0.0F && this.blockedByShield(source)) {
                    this.damageShield(amount);
                    g = amount;
                    amount = 0.0F;
                    if (!source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                        Entity entity = source.getSource();
                        if (entity instanceof LivingEntity) {
                            LivingEntity livingEntity = (LivingEntity) entity;
                            this.takeShieldHit(livingEntity);
                        }
                    }

                    bl = true;
                }

                if (source.isIn(DamageTypeTags.IS_FREEZING) && this.getType().isIn(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                    amount *= 5.0F;
                }

                this.limbAnimator.setSpeed(1.5F);
                boolean bl2 = true;
                if ((float) this.timeUntilRegen > 10.0F && !source.isIn(DamageTypeTags.BYPASSES_COOLDOWN)) {
                    if (amount <= this.lastDamageTaken) {
                        cir.setReturnValue(false);
                    }

                    this.applyDamage(source, amount - this.lastDamageTaken);
                    this.lastDamageTaken = amount;
                    bl2 = false;
                } else {
                    this.lastDamageTaken = amount;
                    this.timeUntilRegen = 20;
                    this.applyDamage(source, amount);
                    this.maxHurtTime = 10;
                    this.hurtTime = this.maxHurtTime;
                }

                if (source.isIn(DamageTypeTags.DAMAGES_HELMET) && !this.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
                    this.damageHelmet(source, amount);
                    amount *= 0.75F;
                }

                Entity entity2 = source.getAttacker();
                if (entity2 != null) {
                    if (entity2 instanceof LivingEntity) {
                        LivingEntity livingEntity2 = (LivingEntity) entity2;
                        if (!source.isIn(DamageTypeTags.NO_ANGER)) {
                            this.setAttacker(livingEntity2);
                        }
                    }

                    if (entity2 instanceof PlayerEntity) {
                        PlayerEntity playerEntity = (PlayerEntity) entity2;
                        this.playerHitTimer = 100;
                        this.attackingPlayer = playerEntity;
                    } else if (entity2 instanceof WolfEntity) {
                        WolfEntity wolfEntity = (WolfEntity) entity2;
                        if (wolfEntity.isTamed()) {
                            this.playerHitTimer = 100;
                            LivingEntity var11 = wolfEntity.getOwner();
                            if (var11 instanceof PlayerEntity) {
                                PlayerEntity playerEntity2 = (PlayerEntity) var11;
                                this.attackingPlayer = playerEntity2;
                            } else {
                                this.attackingPlayer = null;
                            }
                        }
                    }
                }

                if (bl2) {
                    if (bl) {
                        this.getWorld().sendEntityStatus(this, (byte) 29);
                    } else {
                        this.getWorld().sendEntityDamage(this, source);
                    }

                    if (!source.isIn(DamageTypeTags.NO_IMPACT) && (!bl || amount > 0.0F)) {
                        if (!source.isOf(DamageTypes.FALL)) {
                            this.scheduleVelocityUpdate();
                        }
                    }

                    if (entity2 != null && !source.isIn(DamageTypeTags.IS_EXPLOSION)) {
                        double d = entity2.getX() - this.getX();

                        double e;
                        for (e = entity2.getZ() - this.getZ(); d * d + e * e < 1.0E-4; e = (Math.random() - Math.random()) * 0.01) {
                            d = (Math.random() - Math.random()) * 0.01;
                        }

                        this.takeKnockback(0.4000000059604645, d, e);
                        if (!bl) {
                            this.tiltScreen(d, e);
                        }
                    }
                }

                if (this.isDead()) {
                    if (!this.tryUseTotem(source)) {
                        SoundEvent soundEvent = this.getDeathSound();
                        if (bl2 && soundEvent != null) {
                            this.playSound(soundEvent, this.getSoundVolume(), this.getSoundPitch());
                        }

                        this.onDeath(source);
                    }
                } else if (bl2) {
                    this.playHurtSound(source);
                }

                boolean bl3 = !bl || amount > 0.0F;
                if (bl3) {
                    this.lastDamageSource = source;
                    this.lastDamageTime = this.getWorld().getTime();
                }

                LivingEntity self = (LivingEntity) this.getWorld().getEntityById(this.getId());

                if (self instanceof ServerPlayerEntity) {
                    Criteria.ENTITY_HURT_PLAYER.trigger((ServerPlayerEntity) self, source, f, amount, bl);
                    if (g > 0.0F && g < 3.4028235E37F) {
                        ((ServerPlayerEntity) self).increaseStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(g * 10.0F));
                    }
                }

                if (entity2 instanceof ServerPlayerEntity) {
                    Criteria.PLAYER_HURT_ENTITY.trigger((ServerPlayerEntity) entity2, this, source, f, amount, bl);
                }

                cir.setReturnValue(bl3);
            }
        }

        cir.cancel();
    }

    /**
     * @Author lolrow and Plaaasma
     * @Reason Fixed movement made it better and fucking awesome.
     */

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void travel(Vec3d movementInput, CallbackInfo ci) {
        MinehopConfig config = ConfigWrapper.config;

        //Disable if it's disabled lol
        if (!config.enabled) { return; }

        //Enable for Players only
        if (this.getType() != EntityType.PLAYER) { return; }

        if (!this.canMoveVoluntarily() && !this.isLogicalSideForUpdatingMovement()) { return; }

        //Cancel override if not in plain walking state.
        if (this.isTouchingWater() || this.isInLava() || this.isFallFlying()) { return; }

        //I don't have a better clue how to do this atm.
        LivingEntity self = (LivingEntity) this.getWorld().getEntityById(this.getId());

        //Disable on creative flying.
        if (this.getType() == EntityType.PLAYER && MovementUtil.isFlying((PlayerEntity) self)) { return; }

        //Reverse multiplication done by the function that calls this one.
        this.sidewaysSpeed /= 0.98F;
        this.forwardSpeed /= 0.98F;
        double sI = movementInput.x / 0.98F;
        double fI = movementInput.z / 0.98F;

        //Have no jump cooldown, why not?
        this.jumpingCooldown = 0;

        //Get Slipperiness and Movement speed.
        BlockPos blockPos = this.getVelocityAffectingPos();
        float slipperiness = this.getWorld().getBlockState(blockPos).getBlock().getSlipperiness();
        float friction = 1-(slipperiness*slipperiness);

        //
        //Apply Friction
        //
        boolean fullGrounded = this.wasOnGround && this.isOnGround(); //Allows for no friction 1-frame upon landing.
        if (fullGrounded) {
            Vec3d velFin = this.getVelocity();
            Vec3d horFin = new Vec3d(velFin.x,0.0F,velFin.z);
            float speed = (float) horFin.length();
            if (speed > 0.001F) {
                float drop = 0.0F;

                drop += (speed * config.movement.sv_friction * friction);

                float newspeed = Math.max(speed - drop, 0.0F);
                newspeed /= speed;
                this.setVelocity(
                        horFin.x * newspeed,
                        velFin.y,
                        horFin.z * newspeed
                );
            }
        }
        this.wasOnGround = this.isOnGround();

        //
        // Accelerate
        //
        float yawDifference = MathHelper.wrapDegrees(this.getHeadYaw() - this.prevHeadYaw);
        if (yawDifference < 0) {
            yawDifference = yawDifference * -1;
        }

        if (!fullGrounded && !this.isClimbing()) {
            sI = sI * yawDifference;
            fI = fI * yawDifference;
        }

        double perfectAngle = findOptimalStrafeAngle(sI, fI, config, fullGrounded);

        if (this.isOnGround()) {
            if (Minehop.efficiencyListMap.containsKey(this.getNameForScoreboard())) {
                List<Double> efficiencyList = Minehop.efficiencyListMap.get(this.getNameForScoreboard());
                if (efficiencyList != null && efficiencyList.size() > 0) {
                    double averageEfficiency = efficiencyList.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
                    Entity localEntity = this.getWorld().getEntityById(this.getId());
                    if (localEntity instanceof PlayerEntity playerEntity) {
                        Minehop.efficiencyUpdateMap.put(playerEntity.getNameForScoreboard(), averageEfficiency);
                    }
                    Minehop.efficiencyListMap.put(this.getNameForScoreboard(), new ArrayList<>());
                }
            }
        }

        if (sI != 0.0F || fI != 0.0F) {
            Vec3d moveDir = MovementUtil.movementInputToVelocity(new Vec3d(sI, 0.0F, fI), 1.0F, this.getYaw());
            Vec3d accelVec = this.getVelocity();

            double projVel = new Vec3d(accelVec.x, 0.0F, accelVec.z).dotProduct(moveDir);
            double accelVel = (this.isOnGround() ? config.movement.sv_accelerate : (config.movement.sv_airaccelerate));

            float maxVel;
            if (fullGrounded) {
                maxVel = (float) (this.movementSpeed * config.movement.speed_mul);
            } else {
                maxVel = (float) (config.movement.sv_maxairspeed);

                double angleBetween = Math.acos(accelVec.normalize().dotProduct(moveDir.normalize()));

                maxVel *= (angleBetween * angleBetween * angleBetween);
            }

            if (projVel + accelVel > maxVel) {
                accelVel = maxVel - projVel;
            }
            Vec3d accelDir = moveDir.multiply(Math.max(accelVel, 0.0F));

            Vec3d newVelocity = accelVec.add(accelDir);

            if (!this.isOnGround()) {
                double v = Math.sqrt((newVelocity.x * newVelocity.x) + (newVelocity.z * newVelocity.z));
                double nogainv2 = (accelVec.x * accelVec.x) + (accelVec.z * accelVec.z);
                double nogainv = Math.sqrt(nogainv2);
                double maxgainv = Math.sqrt(nogainv2 + (maxVel * maxVel));

                double normalYaw = this.getYaw();

                double gaugeValue = sI < 0 || fI < 0 ? (normalYaw - perfectAngle) : (perfectAngle - normalYaw);
                gaugeValue = normalizeAngle(gaugeValue) * 2;

                List<Double> gaugeList = Minehop.gaugeListMap.containsKey(this.getNameForScoreboard()) ? Minehop.gaugeListMap.get(this.getNameForScoreboard()) : new ArrayList<>();
                gaugeList.add(gaugeValue);
                Minehop.gaugeListMap.put(this.getNameForScoreboard(), gaugeList);

                double strafeEfficiency = MathHelper.clamp((((v - nogainv) / (maxgainv - nogainv)) * 100), 0D, 100D);
                List<Double> efficiencyList = Minehop.efficiencyListMap.containsKey(this.getNameForScoreboard()) ? Minehop.efficiencyListMap.get(this.getNameForScoreboard()) : new ArrayList<>();
                efficiencyList.add(strafeEfficiency);
                Minehop.efficiencyListMap.put(this.getNameForScoreboard(), efficiencyList);
            }

            this.setVelocity(newVelocity);
        }

        this.setVelocity(this.applyClimbingSpeed(this.getVelocity()));
        this.move(MovementType.SELF, this.getVelocity());

        //u8
        //Ladder Logic
        //
        Vec3d preVel = this.getVelocity();
        if ((this.horizontalCollision || this.jumping) && this.isClimbing()) {
            preVel = new Vec3d(preVel.x * 0.7D, 0.2D, preVel.z * 0.7D);
        }

        //
        //Apply Gravity (If not in Water)
        //
        double yVel = preVel.y;
        double gravity = config.movement.sv_gravity;
        if (preVel.y <= 0.0D && this.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            gravity = 0.01D;
            this.fallDistance = 0.0F;
        }
        if (this.hasStatusEffect(StatusEffects.LEVITATION)) {
            yVel += (0.05D * (this.getStatusEffect(StatusEffects.LEVITATION).getAmplifier() + 1) - preVel.y) * 0.2D;
            this.fallDistance = 0.0F;
        } else if (this.getWorld().isClient && !this.getWorld().isChunkLoaded(blockPos)) {
            yVel = 0.0D;
        } else if (!this.hasNoGravity()) {
            yVel -= gravity;
        }

        this.setVelocity(preVel.x,yVel,preVel.z);

        //
        //Update limbs.
        //
        this.updateLimbs(self instanceof Flutterer);

        //Override original method.
        ci.cancel();
    }

    public double findOptimalStrafeAngle(double sI, double fI, MinehopConfig config, boolean fullGrounded) {
        double highestVelocity = -Double.MAX_VALUE;
        double optimalAngle = 0;
        for (double angle = this.prevYaw - 45; angle < this.prevYaw + 45; angle += 1) {  // Test angles 0 to 355 degrees, in 5 degree increments
            Vec3d moveDir = MovementUtil.movementInputToVelocity(new Vec3d(sI, 0.0F, fI), 1.0F, (float) angle);
            Vec3d accelVec = this.getVelocity();

            double projVel = new Vec3d(accelVec.x, 0.0F, accelVec.z).dotProduct(moveDir);
            double accelVel = (this.isOnGround() ? config.movement.sv_accelerate : (config.movement.sv_airaccelerate));

            float maxVel;
            if (fullGrounded) {
                maxVel = (float) (this.movementSpeed * config.movement.speed_mul);
            } else {
                maxVel = (float) (config.movement.sv_maxairspeed);

                double angleBetween = Math.acos(accelVec.normalize().dotProduct(moveDir.normalize()));

                maxVel *= (float) (angleBetween * angleBetween * angleBetween);
            }

            if (projVel + accelVel > maxVel) {
                accelVel = maxVel - projVel;
            }
            Vec3d accelDir = moveDir.multiply(Math.max(accelVel, 0.0F));

            Vec3d newVelocity = accelVec.add(accelDir);

            if (newVelocity.horizontalLength() > highestVelocity) {
                highestVelocity = newVelocity.horizontalLength();
                optimalAngle = angle;
            }
        }
        return optimalAngle;
    }

    private static double normalizeAngle(double angle) {
        angle = angle % 360;
        if (angle > 180) angle -= 360;
        else if (angle < -180) angle += 360;
        return angle;
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    void jump(CallbackInfo ci) {
        MinehopConfig config = ConfigWrapper.config;

        //Disable if it's disabled lol
        if (!config.enabled) { return; }

        Vec3d vecFin = this.getVelocity();
        double yVel = this.getJumpVelocity();
        if (this.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            yVel += 0.1F * (this.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1);
        }

        this.setVelocity(vecFin.x, yVel, vecFin.z);
        this.velocityDirty = true;

        ci.cancel();
    }
}
