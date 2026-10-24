import com.google.gson.GsonBuilder;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueSelectionHandler;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.index.SimItems;
import net.createmod.catnip.outliner.Outliner;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** New held-glue visibility boundary: real paired bounds, native hover and actual rendered pixels. */
public final class PackagedGlueVisibilityRegression {
    private static final class Fixture {
        final String id; final boolean honey, moved; final BlockPos source;
        Entity glue; ServerSubLevel plot; FixedConstraintHandle joint; AABB bounds; Vec3 target;
        Fixture(int index, boolean honey, boolean moved) {
            this.honey = honey; this.moved = moved; id = (honey ? "honey" : "super") + (moved ? "-rotated-plot" : "-world");
            source = new BlockPos(index * 20, 120, 0);
        }
    }
    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        var fixtures = List.of(new Fixture(0, false, false), new Fixture(1, true, false),
                new Fixture(2, false, true), new Fixture(3, true, true));
        var report = new LinkedHashMap<String, Object>(); var rows = new ArrayList<Map<String, Object>>(); report.put("cases", rows);
        var path = Path.of(System.getProperty("port.smoke.report")).resolveSibling("glue-visibility.json");
        boolean hidden = test.computeOnClient(client -> client.gui.hud.isHidden());
        try {
            world.getServer().runCommand("gamemode creative @a"); world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear"); world.getServer().runCommand("gamerule minecraft:advance_time false");
            world.getServer().runCommand("tp @a -100 150 -100");
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst(); player.getAbilities().flying = true; player.onUpdateAbilities();
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); player.inventoryMenu.broadcastFullState();
                for (Fixture fixture : fixtures) setup(server.overworld(), fixture);
            });
            world.getServer().waitFor(server -> fixtures.stream().filter(fixture -> fixture.moved).allMatch(fixture ->
                    fixture.plot.getMassTracker().getMass() > 0 && RigidBodyHandle.of(fixture.plot) != null), 200);
            world.getServer().runOnServer(server -> {
                for (Fixture fixture : fixtures) {
                    if (fixture.moved) stabilize(server.overworld(), fixture);
                    fixture.target = fixture.plot == null ? fixture.bounds.getCenter() : fixture.plot.logicalPose().transformPosition(fixture.bounds.getCenter());
                }
            });
            test.runOnClient(client -> { if (!client.gui.hud.isHidden()) client.gui.hud.toggle(); });
            for (Fixture fixture : fixtures) {
                final Vec3 eye = fixture.target.add(0, .7, 3.8);
                world.getServer().runCommand("tp @a " + eye.x + " " + (eye.y - 1.62) + " " + eye.z);
                test.waitFor(client -> client.player != null && client.player.position().distanceToSqr(new Vec3(eye.x, eye.y - 1.62, eye.z)) < .04, 300);
                look(test, fixture.target);
                test.waitFor(client -> paired(client, fixture), 400);
                world.getConnection().waitForChunksRender(); test.waitTicks(20);
                var before = test.takeScreenshot("glue-" + fixture.id + "-empty-hand");
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(fixture.honey ? SimItems.HONEY_GLUE : AllItems.SUPER_GLUE));
                    player.inventoryMenu.broadcastFullState();
                });
                test.waitFor(client -> client.player.getMainHandItem().is(fixture.honey ? SimItems.HONEY_GLUE : AllItems.SUPER_GLUE), 200);
                test.waitFor(client -> hovered(client, fixture), 300); test.waitTicks(12);
                var after = test.takeScreenshot("glue-" + fixture.id + "-existing-region");
                int visible = visiblePixels(before, after, fixture.honey);
                var evidence = new LinkedHashMap<String, Object>();
                evidence.put("case", fixture.id); evidence.put("entityUuid", fixture.glue.getUUID().toString());
                evidence.put("serverBounds", bounds(fixture.bounds)); evidence.put("pairedBoundsExact", true); evidence.put("nativeHoverSelected", true);
                evidence.put("changedOverlayColorPixels", visible); evidence.put("emptyHandScreenshot", before.toString());
                evidence.put("heldGlueScreenshot", after.toString()); evidence.put("nativeNbtLoadedBeforeLatePairing", fixture.honey);
                if (fixture.plot != null) { evidence.put("plotUuid", fixture.plot.getUniqueId().toString()); evidence.put("plotRotatedYDegrees", 31); evidence.put("plotRotatedZDegrees", 13); }
                rows.add(evidence); Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report));
                check(visible >= 12, fixture.id + ": existing glued region produced no visible colored overlay pixels: " + visible);
                checks.add("glue visibility/" + fixture.id + ": exact paired native bounds, real hovered volume and " + visible + " visible overlay pixels");
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst(); player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); player.inventoryMenu.broadcastFullState();
                });
                test.waitTicks(20);
            }
            report.put("status", "passed");
            report.put("persistenceScope", "Honey entities use native Entity saveWithoutId/load with original relative From/To before a player starts tracking; this scope does not claim a separate-JVM restart.");
        } catch (Throwable failure) { report.put("status", "failed"); report.put("error", failure.toString()); throw failure; }
        finally {
            test.runOnClient(client -> { if (client.gui.hud.isHidden() != hidden) client.gui.hud.toggle(); });
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }
    }
    private static void setup(net.minecraft.server.level.ServerLevel level, Fixture fixture) {
        var blocks = new ArrayList<BlockPos>();
        for (BlockPos pos : BlockPos.betweenClosed(fixture.source, fixture.source.offset(1, 1, 1))) {
            var immutable = pos.immutable(); level.getChunk(immutable); level.setBlockAndUpdate(immutable, Blocks.SMOOTH_QUARTZ.defaultBlockState()); blocks.add(immutable);
        }
        fixture.bounds = new AABB(fixture.source).expandTowards(1, 1, 1);
        if (fixture.moved) {
            fixture.plot = SubLevelAssemblyHelper.assembleBlocks(level, fixture.source, blocks, BoundingBox3i.from(blocks));
            check(fixture.plot != null && !fixture.plot.isRemoved(), "Actual plot allocation failed");
            var anchor = fixture.plot.getPlot().getCenterBlock(); fixture.bounds = fixture.bounds.move(Vec3.atLowerCornerOf(anchor.subtract(fixture.source)));
        }
        if (fixture.honey) {
            var original = new HoneyGlueEntity(level, fixture.bounds);
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess()); original.saveWithoutId(output);
            var tag = output.buildResult(); check(tag.contains("From") && tag.contains("To"), "Legacy relative Honey From/To were not saved");
            var loaded = new HoneyGlueEntity(dev.simulated_team.simulated.index.SimEntityTypes.HONEY_GLUE, level);
            loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
            check(loaded.getBoundingBox().equals(fixture.bounds), "Native saved Honey bounds changed on load"); fixture.glue = loaded;
        } else fixture.glue = new SuperGlueEntity(level, fixture.bounds);
        check(level.addFreshEntity(fixture.glue), "Genuine glue entity was not added");
        fixture.target = fixture.plot == null ? fixture.bounds.getCenter() : fixture.plot.logicalPose().transformPosition(fixture.bounds.getCenter());
    }
    private static void stabilize(net.minecraft.server.level.ServerLevel level, Fixture fixture) {
        var rotation = new Quaterniond().rotateY(Math.toRadians(31)).rotateZ(Math.toRadians(13));
        var location = new Vector3d(fixture.plot.logicalPose().position()).add(3, 1, 0);
        RigidBodyHandle.of(fixture.plot).teleport(location, rotation);
        fixture.joint = SubLevelContainer.getContainer(level).physicsSystem().getPipeline().addConstraint(null, fixture.plot,
                new FixedConstraintConfiguration(location, new Vector3d(fixture.plot.getMassTracker().getCenterOfMass()), rotation));
        check(fixture.joint != null && fixture.joint.isValid(), "Actual fixed plot joint failed");
    }
    private static boolean paired(Minecraft client, Fixture fixture) {
        if (client.level == null) return false;
        var glue = client.level.getEntity(fixture.glue.getId());
        return glue != null && glue.getUUID().equals(fixture.glue.getUUID()) && glue.getBoundingBox().equals(fixture.bounds);
    }
    private static boolean hovered(Minecraft client, Fixture fixture) {
        if (!paired(client, fixture)) return false;
        if (fixture.honey) return Outliner.getInstance().getOutlines().containsKey("HoneyGlueActive" + fixture.glue.getId());
        var frame = SuperGlueSelectionHandler.latest();
        return frame != null && frame.lines() >= 12 && frame.selected() != null && frame.selected() == fixture.glue.getId();
    }
    private static int visiblePixels(Path before, Path after, boolean honey) throws Exception {
        var base = ImageIO.read(before.toFile()); var held = ImageIO.read(after.toFile()); check(base != null && held != null, "Actual screenshots could not be decoded");
        int count = 0;
        for (int y = held.getHeight() / 8; y < held.getHeight() * 7 / 8; y++) for (int x = held.getWidth() / 5; x < held.getWidth() * 4 / 5; x++) {
            int p = held.getRGB(x, y), q = base.getRGB(x, y); int r = p >> 16 & 255, g = p >> 8 & 255, b = p & 255;
            boolean colored = honey ? r > b * 1.25 + 8 && g > b * 1.12 + 5 : g > r * 1.12 + 5 && g > b * 1.12 + 5;
            int change = Math.abs(r - (q >> 16 & 255)) + Math.abs(g - (q >> 8 & 255)) + Math.abs(b - (q & 255));
            if (colored && change > 35) count++;
        }
        return count;
    }
    private static List<Double> bounds(AABB b) { return List.of(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ); }
    private static void look(ClientGameTestContext test, Vec3 point) {
        var angle = test.computeOnClient(client -> {
            var d = point.subtract(client.player.getEyePosition());
            return new Vec2((float) Math.toDegrees(Math.atan2(-d.x, d.z)), (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
        });
        test.getInput().lookAt(angle.x, angle.y);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private PackagedGlueVisibilityRegression() {}
}
