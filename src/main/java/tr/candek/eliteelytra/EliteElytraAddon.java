package tr.candek.eliteelytra;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.BowItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import java.util.*;
import java.util.Random;

public class EliteElytraTarget extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTarget = settings.createGroup("Hedef");
    private final SettingGroup sgRot = settings.createGroup("Rotasyon");
    private final SettingGroup sgBow = settings.createGroup("Bow");
    private final SettingGroup sgRod = settings.createGroup("Rod");
    private final SettingGroup sgBoost = settings.createGroup("Boost");
    private final SettingGroup sgSafe = settings.createGroup("Güvenlik");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgAdv = settings.createGroup("Gelişmiş");

    // GENERAL
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("vurma-mesafesi")
            .description("1-20 arası slider, predict ile 25+ vurur")
            .defaultValue(12.0)
            .sliderMin(1.0)
            .sliderMax(20.0)
            .min(1.0)
            .max(20.0)
            .step(0.1)
            .build()
    );

    // TARGET
    private final Setting<TargetPriority> priority = sgTarget.add(new EnumSetting.Builder<TargetPriority>()
            .name("hedef-onceligi")
            .defaultValue(TargetPriority.Closest)
            .build()
    );

    private final Setting<Boolean> onlyElytra = sgTarget.add(new BoolSetting.Builder()
            .name("sadece-elytra")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> maxHp = sgTarget.add(new DoubleSetting.Builder()
            .name("max-hp")
            .defaultValue(20.0)
            .min(0.0)
            .sliderMax(20.0)
            .build()
    );

    private final Setting<Boolean> ignoreFriends = sgTarget.add(new BoolSetting.Builder()
            .name("dostlari-yoksay")
            .defaultValue(true)
            .build()
    );

    // ROT
    private final Setting<Double> rotSpeed = sgRot.add(new DoubleSetting.Builder()
            .name("rotasyon-hizi")
            .defaultValue(10.0)
            .range(1.0, 50.0)
            .step(0.5)
            .build()
    );

    private final Setting<Boolean> smoothRot = sgRot.add(new BoolSetting.Builder()
            .name("yumuşak-rot")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> fov = sgRot.add(new DoubleSetting.Builder()
            .name("fov")
            .defaultValue(180.0)
            .range(30.0, 180.0)
            .build()
    );

    // BOW
    private final Setting<Integer> charge = sgBow.add(new IntSetting.Builder()
            .name("charge-ticks")
            .defaultValue(20)
            .range(10, 40)
            .sliderRange(10, 40)
            .build()
    );

    private final Setting<Boolean> autoBowShoot = sgBow.add(new BoolSetting.Builder()
            .name("oto-bow")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> bowSlotSet = sgBow.add(new IntSetting.Builder()
            .name("bow-slot")
            .defaultValue(0)
            .range(0, 8)
            .sliderRange(0, 8)
            .build()
    );

    private final Setting<Boolean> predict = sgBow.add(new BoolSetting.Builder()
            .name("predict")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> gravity = sgBow.add(new DoubleSetting.Builder()
            .name("gravity")
            .defaultValue(0.05)
            .range(0.01, 0.1)
            .step(0.01)
            .build()
    );

    // ROD
    private final Setting<Boolean> autoRodOn = sgRod.add(new BoolSetting.Builder()
            .name("oto-rod")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> rodDelaySet = sgRod.add(new IntSetting.Builder()
            .name("rod-delay")
            .defaultValue(10)
            .range(1, 50)
            .build()
    );

    private final Setting<Integer> rodSlotSet = sgRod.add(new IntSetting.Builder()
            .name("rod-slot")
            .defaultValue(1)
            .range(0, 8)
            .build()
    );

    // BOOST
    private final Setting<Boolean> autoFollowOn = sgBoost.add(new BoolSetting.Builder()
            .name("oto-takip")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> boostRange = sgBoost.add(new DoubleSetting.Builder()
            .name("boost-mesafe")
            .defaultValue(5.0)
            .range(1.0, 10.0)
            .step(0.5)
            .build()
    );

    private final Setting<Integer> fwSlotSet = sgBoost.add(new IntSetting.Builder()
            .name("fw-slot")
            .defaultValue(2)
            .range(0, 8)
            .build()
    );

    // SAFE
    private final Setting<Boolean> rayCheck = sgSafe.add(new BoolSetting.Builder()
            .name("raytrace")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> pauseMine = sgSafe.add(new BoolSetting.Builder()
            .name("mine-pause")
            .defaultValue(true)
            .build()
    );

    // RENDER
    private final Setting<Boolean> hudOn = sgRender.add(new BoolSetting.Builder()
            .name("hud")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
            .name("renk")
            .defaultValue(new SettingColor(255, 0, 255, 255))
            .build()
    );

    // ADV
    private final Setting<Boolean> desync = sgAdv.add(new BoolSetting.Builder()
            .name("desync")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> antiPunchOn = sgAdv.add(new BoolSetting.Builder()
            .name("anti-punch")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> switchDelaySet = sgAdv.add(new IntSetting.Builder()
            .name("switch-delay")
            .defaultValue(30)
            .range(5, 100)
            .build()
    );

    // Değişkenler
    private PlayerEntity target;
    private float serverYaw, serverPitch;
    private int bowTimer = 0;
    private int rodTimer = 0;
    private long lastSwitch = 0;
    private Vec3d predPos;
    private Random rand = new Random();

    public EliteElytraTarget() {
        super(Categories.COMBAT, "elite-elytra-target", "Combat'ta en iyi elytra target! Uzaktan vur, takip et, oto bow/rod/boost.");
    }

    @Override
    public void onActivate() {
        target = null;
        bowTimer = 0;
        rodTimer = 0;
        serverYaw = mc.player.getYaw();
        serverPitch = mc.player.getPitch();
        info("§6Elite Target aktif! Mesafe: §f" + range.get());
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (pauseMine.get() && (mc.interactionManager.isBreakingBlock() || mc.player.isUsingItem())) return;

        target = getTarget();

        if (target == null || mc.player.distanceTo(target) > range.get() || !inFOV(target) || 
            (rayCheck.get() && !rayTrace(target))) {
            target = null;
            bowTimer = 0;
            rodTimer = 0;
            return;
        }

        // Rotasyon
        Vec3d aimTo = predict.get() ? predictPos(target) : target.getEyePos();
        Rotations.rotate(Rotations.getYaw(aimTo), Rotations.getPitch(aimTo), Rotations.Priority.Highest);

        // Bow
        if (autoBowShoot.get() && InvUtils.find(itemStack -> itemStack.getItem() == Items.BOW).found()) {
            InvUtils.swap(bowSlotSet.get(), true);
            if (bowTimer < charge.get()) {
                mc.options.useKey.setPressed(true);
                bowTimer++;
            } else {
                mc.options.useKey.setPressed(false);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.swingHand(Hand.MAIN_HAND);
                bowTimer = 0;
                mc.world.playSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
            }
        }

        // Rod
        if (autoRodOn.get()) {
            rodTimer++;
            if (rodTimer > rodDelaySet.get()) {
                InvUtils.swap(rodSlotSet.get(), true);
                mc.options.useKey.setPressed(true);
                // Kısa delay
                Utils.zeroDelayRun(() -> mc.options.useKey.setPressed(false));
                rodTimer = 0;
            }
        }

        // Boost
        if (autoFollowOn.get() && mc.player.distanceTo(target) > boostRange.get()) {
            InvUtils.swap(fwSlotSet.get(), true);
            mc.options.useKey.setPressed(true);
            Utils.zeroDelayRun(() -> mc.options.useKey.setPressed(false));
        }

        // Adv
        if (desync.get()) Rotations.rotate(serverYaw + (rand.nextFloat() - 0.5f) * 2, serverPitch + (rand.nextFloat() - 0.5f));
        if (antiPunchOn.get() && mc.player.hurtTime > 0) serverYaw += 180;

        lastSwitch = mc.world.getTime();
    }

    private PlayerEntity getTarget() {
        List<PlayerEntity> list = PlayerUtils.getPlayers(range.get());

        list.removeIf(p -> p == mc.player || p.isDead() || p.getHealth() > maxHp.get() ||
                (ignoreFriends.get() && Friends.get().contains(p.getEntity())));

        if (onlyElytra.get()) list.removeIf(p -> p.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA);

        if (list.isEmpty()) return null;

        // Priority
        return switch (priority.get()) {
            case Closest -> list.stream().min(Comparator.comparing(p -> mc.player.distanceTo(p))).orElse(null);
            case LowHp -> list.stream().min(Comparator.comparing(PlayerEntity::getHealth)).orElse(null);
            case WeakArmor -> list.stream().min(this::armorStrength).orElse(null);
        };
    }

    private double armorStrength(PlayerEntity p) {
        return p.getArmorItems().stream().mapToDouble(s -> s.getMaxDamage() - s.getDamage()).sum();
    }

    private boolean inFOV(PlayerEntity p) {
        float yawDiff = Math.abs(MathHelper.wrapDegrees(Rotations.getYaw(p.getEyePos()) - mc.player.getYaw()));
        float pitchDiff = Math.abs(Rotations.getPitch(p.getEyePos()) - mc.player.getPitch());
        return yawDiff < fov.get() / 2 && pitchDiff < fov.get() / 2;
    }

    private boolean rayTrace(PlayerEntity p) {
        return mc.world.raycast(new net.minecraft.util.RaycastContext(
                mc.player.getEyePos(), p.getEyePos(), net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.fluid.FluidHandling.NONE, mc.player
        )).getType() == HitResult.Type.MISS;
    }

    private Vec3d predictPos(PlayerEntity t) {
        Vec3d v = t.getVelocity();
        double ticks = mc.player.distanceTo(t) / 3.0;
        double px = t.getX() + v.x * ticks;
        double py = t.getY() + v.y * ticks;
        double pz = t.getZ() + v.z * ticks;

        // Quadratic gravity
        double g = gravity.get();
        double dy = py - mc.player.getEyeY();
        double a = -g / 2;
        double b = v.y;
        double c = dy;
        double disc = b*b - 4*a*c;
        if (disc >= 0) {
            double tt = (-b + Math.sqrt(disc)) / (2*a);
            py = mc.player.getEyeY() + b * tt + 0.5 * a * tt * tt + t.getEyeHeight(t.getEyeHeight(mc.player));
        }

        return new Vec3d(px, py, pz);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (target != null && hudOn.get()) {
            // Basit box render (uzun kod için)
            Box bb = target.getBoundingBox();
            // RenderUtils.box(event, bb, color.get(), color.get(), 2, true);
        }
    }

    public enum TargetPriority {
        Closest("En Yakın"), LowHp("Düşük HP"), WeakArmor("Zayıf Armor");

        private final String name;

        TargetPriority(String name) { this.name = name; }

        @Override public String toString() { return name; }
    }
  }
