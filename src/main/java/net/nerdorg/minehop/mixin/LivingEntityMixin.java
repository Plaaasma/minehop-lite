// ORIGINAL BY hatninja ON GITHUB
// Ported to Minecraft 26.2 / official Mojang mappings.
// NOTE: This is a blind (uncompiled) port. Method/field names below follow Mojang
// mappings for the 1.21.x -> 26.x line; a couple of the riskier symbols are flagged
// with TODO and should be confirmed against a local `./gradlew build`.

package net.nerdorg.minehop.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.nerdorg.minehop.Minehop;
import net.nerdorg.minehop.config.ConfigWrapper;
import net.nerdorg.minehop.config.MinehopConfig;
import net.nerdorg.minehop.util.MovementUtil;
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
    @Shadow public float speed;              // yarn: movementSpeed
    @Shadow public float xxa;                // yarn: sidewaysSpeed
    @Shadow public float zza;                // yarn: forwardSpeed
    @Shadow private int noJumpDelay;         // yarn: jumpingCooldown
    @Shadow protected boolean jumping;
    @Shadow public float yHeadRotO;          // yarn: prevHeadYaw

    @Shadow protected abstract Vec3 handleOnClimbable(Vec3 vec);           // yarn: applyClimbingSpeed
    @Shadow protected abstract float getJumpPower();                       // yarn: getJumpVelocity
    @Shadow public abstract boolean hasEffect(Holder<MobEffect> effect);   // yarn: hasStatusEffect
    @Shadow public abstract MobEffectInstance getEffect(Holder<MobEffect> effect); // yarn: getStatusEffect
    @Shadow public abstract boolean onClimbable();                         // yarn: isClimbing
    @Shadow public abstract float getYHeadRot();                           // yarn: getHeadYaw
    @Shadow public abstract boolean isFallFlying();                        // yarn: isGliding
    @Shadow public abstract boolean isEffectiveAi();                       // yarn: canMoveVoluntarily
    @Shadow public abstract BlockPos getBlockPosBelowThatAffectsMyMovement(); // yarn: getVelocityAffectingPos

    private boolean wasOnGround;
    private boolean wasCrouching = false;
    private static final float STAND_HEIGHT = 1.8f;
    private static final float CROUCH_HEIGHT = 1.35f;
    private double baseFriction;
    private double activeFriction;

    public LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    // The original copied the entire vanilla damage() body just to add a fall-damage toggle.
    // In 26.x the server damage entrypoint is hurtServer(ServerLevel, DamageSource, float) and the
    // internal pipeline changed shape, so re-implementing it verbatim is fragile. We keep only the
    // actual intent: cancel fall damage when disabled in config, and let vanilla handle the rest.
    // TODO(verify): confirm the method name is "hurtServer" on 26.2 (fallback candidates: "hurt").
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    public void onHurt(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.is(DamageTypes.FALL) && !ConfigWrapper.config.fall_damage) {
            cir.setReturnValue(false);
        }
    }

    /**
     * @Author lolrow and Plaaasma
     * @Reason Fixed movement made it better and fucking awesome.
     */
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void travel(Vec3 movementInput, CallbackInfo ci) {
        MinehopConfig config = ConfigWrapper.config;

        //Disable if it's disabled lol
        if (!config.enabled) { return; }

        //Enable for Players only
        if (this.getType() != EntityType.PLAYER) { return; }

        if (!this.isEffectiveAi() && !this.isControlledByLocalInstance()) { return; }

        //Cancel override if not in plain walking state.
        if (this.isInWater() || this.isInLava() || this.isFallFlying()) { return; }

        //I don't have a better clue how to do this atm.
        LivingEntity self = (LivingEntity) this.level().getEntity(this.getId());

        //Disable on creative flying.
        if (this.getType() == EntityType.PLAYER && MovementUtil.isFlying((Player) self)) { return; }

        if (baseFriction == 0) {
            baseFriction = ConfigWrapper.config.movement.sv_friction; // Save wtv friction player put in config
            activeFriction = baseFriction; // Start with normal friction ( change l8r)
        }

        if (this.isShiftKeyDown()) {
            activeFriction = 0.85; // seems corrcet
        } else {
            activeFriction = baseFriction;
        }

        boolean isCrouching = this.isShiftKeyDown();
        if (isCrouching && !wasCrouching) {
            // Check if there's room to "stand" before shifting up
            if (this.level().noCollision(this, this.getBoundingBox().inflate(0.0, STAND_HEIGHT - CROUCH_HEIGHT, 0.0))) {
                this.setPos(this.getX(), this.getY() + (STAND_HEIGHT - CROUCH_HEIGHT), this.getZ());
            }
        }
        wasCrouching = isCrouching;

        //Reverse multiplication done by the function that calls this one.
        this.xxa /= 0.98F;
        this.zza /= 0.98F;
        double sI = movementInput.x / 0.98F;
        double fI = movementInput.z / 0.98F;

        //Have no jump cooldown, why not?
        this.noJumpDelay = 0;

        //Get Slipperiness and Movement speed.
        BlockPos blockPos = this.getBlockPosBelowThatAffectsMyMovement();
        float slipperiness = this.level().getBlockState(blockPos).getBlock().getFriction();
        float friction = 1-(slipperiness*slipperiness);

        //
        //Apply Friction
        //
        boolean fullGrounded = this.wasOnGround && this.onGround(); //Allows for no friction 1-frame upon landing.
        if (fullGrounded) {
            Vec3 velFin = this.getDeltaMovement();
            Vec3 horFin = new Vec3(velFin.x,0.0F,velFin.z);
            float speed = (float) horFin.length();
            if (speed > 0.001F) {
                float drop = 0.0F;

                drop += (speed * activeFriction * friction);

                float newspeed = Math.max(speed - drop, 0.0F);
                newspeed /= speed;
                this.setDeltaMovement(
                        horFin.x * newspeed,
                        velFin.y,
                        horFin.z * newspeed
                );
            }
        }
        this.wasOnGround = this.onGround();

        //
        // Accelerate
        //
        float yawDifference = Mth.wrapDegrees(this.getYHeadRot() - this.yHeadRotO);
        if (yawDifference < 0) {
            yawDifference = yawDifference * -1;
        }

        if (!fullGrounded && !this.onClimbable()) {
            sI = sI * yawDifference;
            fI = fI * yawDifference;
        }

        double perfectAngle = findOptimalStrafeAngle(sI, fI, config, fullGrounded);

        if (this.onGround()) {
            if (Minehop.efficiencyListMap.containsKey(this.getScoreboardName())) {
                List efficiencyList = Minehop.efficiencyListMap.get(this.getScoreboardName());
                if (efficiencyList != null && efficiencyList.size() > 0) {
                    double averageEfficiency = efficiencyList.stream().mapToDouble(o -> (Double) o).average().orElse(Double.NaN);
                    Entity localEntity = this.level().getEntity(this.getId());
                    if (localEntity instanceof Player playerEntity) {
                        Minehop.efficiencyUpdateMap.put(playerEntity.getScoreboardName(), averageEfficiency);
                    }
                    Minehop.efficiencyListMap.put(this.getScoreboardName(), new ArrayList<>());
                }
            }
        }

        if (sI != 0.0F || fI != 0.0F) {
            Vec3 moveDir = MovementUtil.movementInputToVelocity(new Vec3(sI, 0.0F, fI), 1.0F, this.getYRot());
            Vec3 accelVec = this.getDeltaMovement();

            double projVel = new Vec3(accelVec.x, 0.0F, accelVec.z).dot(moveDir);
            double accelVel = (this.onGround() ? config.movement.sv_accelerate : (config.movement.sv_airaccelerate));

            float maxVel;
            if (fullGrounded) {
                maxVel = (float) (this.speed * config.movement.speed_mul);
            } else {
                maxVel = (float) (config.movement.sv_maxairspeed);

                double angleBetween = Math.acos(accelVec.normalize().dot(moveDir.normalize()));

                maxVel *= (angleBetween * angleBetween * angleBetween);
            }

            if (projVel + accelVel > maxVel) {
                accelVel = maxVel - projVel;
            }
            Vec3 accelDir = moveDir.scale(Math.max(accelVel, 0.0F));

            Vec3 newVelocity = accelVec.add(accelDir);

            if (!this.onGround()) {
                double v = Math.sqrt((newVelocity.x * newVelocity.x) + (newVelocity.z * newVelocity.z));
                double nogainv2 = (accelVec.x * accelVec.x) + (accelVec.z * accelVec.z);
                double nogainv = Math.sqrt(nogainv2);
                double maxgainv = Math.sqrt(nogainv2 + (maxVel * maxVel));

                double normalYaw = this.getYRot();

                double gaugeValue = sI < 0 || fI < 0 ? (normalYaw - perfectAngle) : (perfectAngle - normalYaw);
                gaugeValue = normalizeAngle(gaugeValue) * 2;

                List gaugeList = Minehop.gaugeListMap.containsKey(this.getScoreboardName()) ? Minehop.gaugeListMap.get(this.getScoreboardName()) : new ArrayList<>();
                gaugeList.add(gaugeValue);
                Minehop.gaugeListMap.put(this.getScoreboardName(), gaugeList);

                double strafeEfficiency = Mth.clamp((((v - nogainv) / (maxgainv - nogainv)) * 100), 0D, 100D);
                List efficiencyList = Minehop.efficiencyListMap.containsKey(this.getScoreboardName()) ? Minehop.efficiencyListMap.get(this.getScoreboardName()) : new ArrayList<>();
                efficiencyList.add(strafeEfficiency);
                Minehop.efficiencyListMap.put(this.getScoreboardName(), efficiencyList);
            }

            this.setDeltaMovement(newVelocity);
        }

        this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
        this.move(MoverType.SELF, this.getDeltaMovement());

        //
        //Ladder Logic
        //
        Vec3 preVel = this.getDeltaMovement();
        if ((this.horizontalCollision || this.jumping) && this.onClimbable()) {
            preVel = new Vec3(preVel.x * 0.7D, 0.2D, preVel.z * 0.7D);
        }

        //
        //Apply Gravity (If not in Water)
        //
        double yVel = preVel.y;
        double gravity = config.movement.sv_gravity;
        if (preVel.y <= 0.0D && this.hasEffect(MobEffects.SLOW_FALLING)) {
            gravity = 0.01D;
            this.fallDistance = 0.0F;
        }
        ChunkPos currentChunk = this.chunkPosition();
        if (this.hasEffect(MobEffects.LEVITATION)) {
            yVel += (0.05D * (this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1) - preVel.y) * 0.2D;
            this.fallDistance = 0.0F;
        } else if (this.level().isClientSide && !this.level().hasChunk(currentChunk.x, currentChunk.z)) {
            yVel = 0.0D;
        } else if (!this.isNoGravity()) {
            yVel -= gravity;
        }

        this.setDeltaMovement(preVel.x, yVel, preVel.z);

        //
        //Update limbs. (yarn updateLimbs(boolean) has no clean 1:1 Mojang equivalent here;
        //it is a cosmetic limb-animation update and is intentionally omitted for the port.)
        //
        // this.updateWalkAnimation(...);

        //Override original method.
        ci.cancel();
    }

    public double findOptimalStrafeAngle(double sI, double fI, MinehopConfig config, boolean fullGrounded) {
        double highestVelocity = -Double.MAX_VALUE;
        double optimalAngle = 0;
        for (double angle = this.yRotO - 45; angle < this.yRotO + 45; angle += 1) {
            Vec3 moveDir = MovementUtil.movementInputToVelocity(new Vec3(sI, 0.0F, fI), 1.0F, (float) angle);
            Vec3 accelVec = this.getDeltaMovement();

            double projVel = new Vec3(accelVec.x, 0.0F, accelVec.z).dot(moveDir);
            double accelVel = (this.onGround() ? config.movement.sv_accelerate : (config.movement.sv_airaccelerate));

            float maxVel;
            if (fullGrounded) {
                maxVel = (float) (this.speed * config.movement.speed_mul);
            } else {
                maxVel = (float) (config.movement.sv_maxairspeed);

                double angleBetween = Math.acos(accelVec.normalize().dot(moveDir.normalize()));

                maxVel *= (float) (angleBetween * angleBetween * angleBetween);
            }

            if (projVel + accelVel > maxVel) {
                accelVel = maxVel - projVel;
            }
            Vec3 accelDir = moveDir.scale(Math.max(accelVel, 0.0F));

            Vec3 newVelocity = accelVec.add(accelDir);

            if (newVelocity.horizontalDistance() > highestVelocity) {
                highestVelocity = newVelocity.horizontalDistance();
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

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        EntityDimensions original = super.getDimensions(pose);
        if (this.isShiftKeyDown()) {
            return EntityDimensions.scalable(original.width(), CROUCH_HEIGHT);
        }
        return original;
    }

    // yarn LivingEntity#jump() -> Mojang LivingEntity#jumpFromGround()
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    void jump(CallbackInfo ci) {
        MinehopConfig config = ConfigWrapper.config;

        //Disable if it's disabled lol
        if (!config.enabled) { return; }

        Vec3 vecFin = this.getDeltaMovement();
        double yVel = this.getJumpPower();
        // TODO(verify): MobEffects.JUMP_BOOST is the jump-boost effect holder on 26.x (older maps used MobEffects.JUMP).
        if (this.hasEffect(MobEffects.JUMP_BOOST)) {
            yVel += 0.1F * (this.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1);
        }

        this.setDeltaMovement(vecFin.x, yVel, vecFin.z);
        this.hasImpulse = true;

        ci.cancel();
    }
}
