import com.google.gson.GsonBuilder;
import com.simibubi.create.content.schematics.SchematicFiles;
import com.simibubi.create.content.schematics.SchematicStructure;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.index.SimEntityTypes;
import dev.simulated_team.simulated.index.SimItems;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Real six-face item placement, native pairing/persistence and the original user's legacy schematic. */
public final class PackagedDiagramRegression {
    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        var report = new LinkedHashMap<String,Object>(); var rows = new ArrayList<Object>(); report.put("placements", rows);
        var path = Path.of(System.getProperty("port.smoke.report")).resolveSibling("diagram-regression.json");
        try {
            var audit = world.getServer().computeOnServer(server -> {
                try {
                    var level = server.overworld();
                    var template = SchematicFiles.read(level, Path.of(System.getProperty("port.regression.schematic")));
                    var source = SchematicStructure.load(level, template, new BlockPos(128,128,128), new StructurePlaceSettings(), false);
                    check(source.positions().size() == 18659, "Native DataFixer lost original airship blocks");
                    check(source.entities().size() == 111, "Native schematic lost original entities");
                    var diagrams = source.entities().stream().filter(DiagramEntity.class::isInstance).map(DiagramEntity.class::cast).toList();
                    check(diagrams.size() == 1, "Original legacy schematic diagram did not load");
                    var diagram = diagrams.getFirst(); check(diagram.getPos() != null, "Original diagram has no native block anchor");
                    check(diagram.getPos().closerThan(diagram.blockPosition(), 2), "Legacy tile anchor remained at original world coordinates");
                    check(diagram.getDirection() == Direction.SOUTH && diagram.getSize() == 1, "Legacy diagram facing/size changed");
                    return Map.of("nativeSourceBlocks", source.positions().size(), "nativeSourceEntities", source.entities().size(),
                            "legacyDiagramAnchor", diagram.getPos().toShortString(), "legacyDiagramPosition", diagram.position().toString(),
                            "legacyDiagramFacing", diagram.getDirection().toString());
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            report.put("originalAirship", audit);
            checks.add("original 1.21.1 airship decoded by genuine Create DataFixer and sparse structure: all18659 blocks/111entities and the relocated legacy diagram remain");
            world.getServer().runCommand("gamemode creative @a");
            for (var face : Direction.values()) {
                var anchor = new BlockPos(face.ordinal()*12, 124, 0);
                var support = anchor.relative(face.getOpposite());
                var vertical = face.getAxis().isHorizontal() ? Direction.UP : (face == Direction.UP ? Direction.SOUTH : Direction.NORTH);
                var lateral = face.getAxis().isHorizontal() ? face.getCounterClockWise() : Direction.SOUTH.getClockWise();
                world.getServer().runOnServer(server -> {
                    var level = server.overworld(); level.getChunk(anchor);
                    for (int u=-1;u<=1;u++) for(int v=-1;v<=1;v++) level.setBlockAndUpdate(support.relative(lateral,u).relative(vertical,v), Blocks.STONE.defaultBlockState());
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.getAbilities().flying=true; player.onUpdateAbilities();
                    player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(SimItems.CONTRAPTION_DIAGRAM,16)); player.inventoryMenu.broadcastFullState();
                });
                var camera = Vec3.atCenterOf(anchor).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(2)).add(0,1,0);
                world.getServer().runCommand("tp @a "+camera.x+" "+camera.y+" "+camera.z+" 0 0");
                test.waitFor(client -> client.player.getMainHandItem().is(SimItems.CONTRAPTION_DIAGRAM)
                        && client.level.getBlockState(support).is(Blocks.STONE) && client.player.position().distanceTo(camera)<1, 200);
                test.runOnClient(client -> client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(.5)),face,support,false)));
                var entityId = new AtomicReference<Integer>();
                world.getServer().waitFor(server -> {
                    var list=server.overworld().getEntitiesOfClass(DiagramEntity.class,new AABB(anchor).inflate(2));
                    if(list.isEmpty())return false;
                    var entity=list.getFirst(); entityId.set(entity.getId()); return entity.getPos().equals(anchor)&&entity.getDirection()==face;
                },200);
                test.waitFor(client -> client.level.getEntity(entityId.get()) instanceof DiagramEntity e
                        && e.getDirection()==face && e.getPos().equals(anchor) && e.getSize()==3,200);
                var evidence = world.getServer().computeOnServer(server -> {
                    var level=server.overworld(); var entity=(DiagramEntity)level.getEntity(entityId.get());
                    check(entity.survives(),"Actual placed diagram does not survive on "+face);
                    var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess()); entity.saveWithoutId(output);
                    var saved=output.buildResult(); var sizes=new ArrayList<Object>();
                    for(int size=1;size<=3;size++){
                        var tag=saved.copy();tag.putInt("Size",size);
                        var restored=(DiagramEntity)SimEntityTypes.CONTRAPTION_DIAGRAM.create(level,EntitySpawnReason.LOAD);
                        restored.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag));
                        check(restored.getPos().equals(anchor)&&restored.getDirection()==face&&restored.getSize()==size,"Native fresh-owner diagram restore changed facing/anchor/size");
                        var box=restored.getBoundingBox();check(box.getXsize()>0&&box.getYsize()>0&&box.getZsize()>0&&Double.isFinite(box.getSize()),"Invalid native diagram bounds");
                        sizes.add(Map.of("size",size,"bounds",box.toString(),"xRot",restored.getXRot(),"yRot",restored.getYRot()));
                    }
                    return Map.of("face",face.toString(),"pairedClientId",entity.getId(),"anchor",anchor.toShortString(),"nativeRestores",sizes);
                });
                rows.add(evidence); Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));
            }
            checks.add("actual DiagramItem useOn/client prediction/server placement/native paired facing supports all6faces including floor/ceiling;18 fresh-owner native restores retain3sizes");
            report.put("status","passed");
        } catch (Exception|Error e) {report.put("status","failed");report.put("error",e.toString());throw e;}
        finally {Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));}
    }
    private static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
}
